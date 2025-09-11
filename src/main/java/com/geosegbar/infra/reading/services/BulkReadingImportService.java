package com.geosegbar.infra.reading.services;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.infra.reading.dtos.ImportReadingsResult;
import com.geosegbar.infra.reading.dtos.ReadingRequestDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkReadingImportService {

    private final ReadingService readingService;

    private static final DateTimeFormatter DATE_FMT
            = DateTimeFormatter.ofPattern("[yyyy-MM-dd][dd-MM-yyyy][dd/MM/yyyy]");
    private static final DateTimeFormatter TIME_FMT
            = DateTimeFormatter.ofPattern("[HH:mm:ss][HH:mm]");

    public ImportReadingsResult importFromExcel(Long instrumentId, MultipartFile file) {
        ImportReadingsResult result = new ImportReadingsResult();

        if (instrumentId == null) {
            throw new InvalidInputException("ID do instrumento não fornecido. Por favor, informe o instrumento para importação das leituras.");
        }

        if (file == null || file.isEmpty()) {
            throw new InvalidInputException("Nenhum arquivo foi enviado. Por favor, selecione uma planilha Excel válida.");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !(fileName.endsWith(".xlsx") || fileName.endsWith(".xls"))) {
            throw new InvalidInputException("Formato de arquivo inválido. Por favor, envie um arquivo Excel (.xlsx ou .xls).");
        }

        try (InputStream is = file.getInputStream()) {
            Workbook wb;
            try {
                wb = WorkbookFactory.create(is);
            } catch (Exception e) {
                throw new InvalidInputException("Não foi possível abrir o arquivo Excel. Verifique se o arquivo está corrompido ou se é uma planilha Excel válida.");
            }

            Sheet sheet = wb.getSheet("Leituras");
            if (sheet == null) {
                throw new InvalidInputException("Aba 'Leituras' não encontrada. Por favor, use o modelo de planilha correto que contenha uma aba chamada 'Leituras'.");
            }

            if (sheet.getPhysicalNumberOfRows() <= 1) {
                throw new InvalidInputException("A planilha não contém dados de leituras para importar. A aba 'Leituras' está vazia ou contém apenas o cabeçalho.");
            }

            Row header = sheet.getRow(0);
            if (header == null) {
                throw new InvalidInputException("Cabeçalho não encontrado na aba 'Leituras'. Verifique se a planilha está no formato correto.");
            }

            Map<String, Integer> idx = new HashMap<>();
            for (Cell c : header) {
                if (c.getCellType() == CellType.STRING) {
                    String h = c.getStringCellValue().trim();
                    if (!h.isEmpty()) {
                        idx.put(h, c.getColumnIndex());
                    }
                }
            }

            Integer idxDate = idx.get("Data da Leitura");
            Integer idxHour = idx.get("Hora da Leitura");
            if (idxDate == null || idxHour == null) {
                throw new InvalidInputException("Colunas obrigatórias não encontradas: 'Data da Leitura' e/ou 'Hora da Leitura'. Verifique se está usando o modelo de planilha correto.");
            }

            List<String> inputAcronyms = idx.keySet().stream()
                    .filter(col -> !col.equals("Data da Leitura") && !col.equals("Hora da Leitura"))
                    .toList();

            if (inputAcronyms.isEmpty()) {
                throw new InvalidInputException("Nenhuma coluna de leitura encontrada além de 'Data da Leitura' e 'Hora da Leitura'. A planilha deve conter ao menos uma coluna adicional com valores de leitura.");
            }

            int total = 0;
            for (int rn = 1; rn <= sheet.getLastRowNum(); rn++) {
                Row r = sheet.getRow(rn);
                if (r == null) {
                    continue;
                }

                Cell dCell = r.getCell(idxDate);
                Cell tCell = r.getCell(idxHour);

                if ((dCell == null || dCell.getCellType() == CellType.BLANK)
                        && (tCell == null || tCell.getCellType() == CellType.BLANK)) {
                    continue;
                }

                total++;

                try {

                    if (dCell == null) {
                        throw new InvalidInputException("Data da Leitura não informada");
                    }

                    LocalDate date;
                    try {
                        if (dCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dCell)) {
                            date = dCell.getLocalDateTimeCellValue().toLocalDate();
                        } else {
                            String s = dCell.getStringCellValue().trim();
                            date = LocalDate.parse(s, DATE_FMT);
                        }
                    } catch (DateTimeParseException | IllegalStateException e) {
                        throw new InvalidInputException("Data da Leitura em formato inválido. Use o formato DD/MM/YYYY.");
                    }

                    if (tCell == null) {
                        throw new InvalidInputException("Hora da Leitura não informada");
                    }

                    LocalTime hour;
                    try {
                        if (tCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(tCell)) {
                            double frac = tCell.getNumericCellValue();
                            hour = LocalTime.ofSecondOfDay((long) (frac * 24 * 3600));
                        } else {
                            String s = tCell.getStringCellValue().trim();
                            hour = LocalTime.parse(s, TIME_FMT);
                        }
                    } catch (DateTimeParseException | IllegalStateException e) {
                        throw new InvalidInputException("Hora da Leitura em formato inválido. Use o formato HH:MM ou HH:MM:SS.");
                    }

                    Map<String, Double> inputValues = new HashMap<>();
                    for (String acr : inputAcronyms) {
                        Cell c = r.getCell(idx.get(acr));
                        if (c == null) {
                            throw new InvalidInputException("Coluna '" + acr + "' vazia");
                        }
                        double v;
                        try {
                            if (c.getCellType() == CellType.NUMERIC) {
                                v = c.getNumericCellValue();
                            } else {
                                String ss = c.getStringCellValue().trim().replace(",", ".");
                                v = Double.parseDouble(ss);
                            }
                        } catch (NumberFormatException | IllegalStateException ex) {
                            throw new InvalidInputException("Valor de '" + acr + "' inválido: "
                                    + (c.getCellType() == CellType.STRING ? c.getStringCellValue() : "não é um número"));
                        }
                        inputValues.put(acr, v);
                    }

                    ReadingRequestDTO req = new ReadingRequestDTO(date, hour, inputValues, null);
                    readingService.create(instrumentId, req);
                    result.setSuccessCount(result.getSuccessCount() + 1);

                } catch (Exception ex) {
                    result.setFailureCount(result.getFailureCount() + 1);
                    result.getErrors().add("Linha " + (rn + 1) + ": " + ex.getMessage());
                    log.warn("Falha importando linha {}: {}", rn + 1, ex.getMessage());
                }
            }
            result.setTotalRows(total);

            if (total == 0) {
                throw new InvalidInputException("Nenhuma leitura encontrada na planilha. Verifique se os dados estão formatados corretamente.");
            }

        } catch (InvalidInputException e) {
            throw e;
        } catch (IOException e) {
            throw new InvalidInputException("Erro ao ler o arquivo Excel: " + e.getMessage());
        } catch (Exception e) {

            String detailedMessage = e.getMessage();
            String friendlyMessage = "Erro ao processar a planilha Excel. ";

            if (detailedMessage != null && detailedMessage.contains("iterator()") && detailedMessage.contains("null")) {
                friendlyMessage += "Uma ou mais abas necessárias não foram encontradas. Certifique-se de usar o modelo de planilha correto com a aba 'Leituras'.";
            } else if (detailedMessage != null && detailedMessage.contains("Invalid header signature")) {
                friendlyMessage += "O arquivo não é uma planilha Excel válida ou está corrompido.";
            } else {
                friendlyMessage += "Detalhes: " + detailedMessage;
            }

            throw new InvalidInputException(friendlyMessage);
        }

        return result;
    }
}
