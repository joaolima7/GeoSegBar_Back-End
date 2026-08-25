package com.geosegbar.infra.share_folder.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.PSBFileEntity;
import com.geosegbar.entities.PSBFolderEntity;
import com.geosegbar.exceptions.FileStorageException;
import com.geosegbar.infra.file_storage.FileStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Monta o ZIP de uma pasta PSB direto no fluxo de saída.
 *
 * NADA é acumulado em memória — nem os arquivos, nem o ZIP montado. Cada objeto
 * é copiado do S3 para o ZIP em blocos, e o ZIP vai sendo escrito na resposta
 * HTTP conforme é gerado.
 *
 * Essa restrição não é preciosismo. Em 24/08/2026 esta classe montava o ZIP em
 * um ByteArrayOutputStream e o controller ainda fazia toByteArray() — duas
 * cópias inteiras na memória, mais um byte[] por arquivo. Duas requisições de
 * download derrubaram a aplicação com OutOfMemoryError, e o processo ficou vivo
 * porém sem aceitar conexões: 18 horas fora do ar.
 *
 * O consumo de memória agora é o do buffer de cópia, independente do tamanho da
 * pasta — e uma pasta de PSB pode ter arquivos de até 512 MB cada.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ZipService {

    private final FileStorageService fileStorageService;

    /**
     * Escreve o ZIP da pasta (incluindo subpastas) diretamente em {@code saida}.
     *
     * @return quantidade de arquivos efetivamente incluídos
     */
    public int writeZipToStream(PSBFolderEntity folder, OutputStream saida) {
        Set<String> caminhosIncluidos = new HashSet<>();

        try (ZipOutputStream zos = new ZipOutputStream(saida)) {
            adicionarPasta(folder, "", zos, caminhosIncluidos);
            zos.finish();
        } catch (IOException e) {
            // O cliente pode ter desistido no meio do download; nesse caso o
            // "erro" é só a conexão fechando, e não há resposta a corrigir.
            log.error("Erro ao gerar ZIP da pasta {}: {}", folder.getName(), e.getMessage());
            throw new FileStorageException("Não foi possível gerar o arquivo ZIP", e);
        }

        log.info("ZIP gerado com {} arquivo(s) para a pasta: {}", caminhosIncluidos.size(), folder.getName());
        return caminhosIncluidos.size();
    }

    /**
     * Conta os arquivos da subárvore sem baixar nada.
     *
     * Serve para recusar cedo uma pasta vazia: depois que o primeiro byte da
     * resposta sai, já não dá para trocar o status HTTP por um erro.
     */
    public int contarArquivos(PSBFolderEntity folder) {
        int total = folder.getFiles() == null ? 0 : folder.getFiles().size();
        if (folder.getSubfolders() != null) {
            for (PSBFolderEntity sub : folder.getSubfolders()) {
                total += contarArquivos(sub);
            }
        }
        return total;
    }

    private void adicionarPasta(PSBFolderEntity folder, String prefixo,
            ZipOutputStream zos, Set<String> incluidos) throws IOException {

        String caminhoAtual = prefixo.isEmpty() ? folder.getName() : prefixo + "/" + folder.getName();

        if (folder.getFiles() != null) {
            for (PSBFileEntity file : folder.getFiles()) {
                adicionarArquivo(file, caminhoAtual, zos, incluidos);
            }
        }

        if (folder.getSubfolders() != null) {
            for (PSBFolderEntity subfolder : folder.getSubfolders()) {
                adicionarPasta(subfolder, caminhoAtual, zos, incluidos);
            }
        }
    }

    private void adicionarArquivo(PSBFileEntity file, String caminhoPasta,
            ZipOutputStream zos, Set<String> incluidos) throws IOException {

        String caminhoNoZip = caminhoPasta + "/" + file.getOriginalFilename();

        if (!incluidos.add(caminhoNoZip)) {
            log.warn("Arquivo duplicado ignorado no ZIP: {}", caminhoNoZip);
            return;
        }

        zos.putNextEntry(new ZipEntry(caminhoNoZip));

        // Cópia em blocos: o pico de memória é o buffer do transferTo, não o
        // tamanho do arquivo.
        try (InputStream entrada = fileStorageService.openStream(file.getDownloadUrl())) {
            entrada.transferTo(zos);
        } catch (RuntimeException e) {
            // Um arquivo ausente no S3 não pode abortar o ZIP inteiro: o
            // download já começou a ser transmitido e o usuário receberia um
            // arquivo truncado sem explicação. Registra e segue com os demais.
            incluidos.remove(caminhoNoZip);
            log.error("Falha ao ler '{}' do S3 ({}): {}",
                    file.getOriginalFilename(), file.getDownloadUrl(), e.getMessage());
        } finally {
            zos.closeEntry();
        }
    }
}
