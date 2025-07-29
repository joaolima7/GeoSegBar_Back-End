package com.geosegbar.infra.reading.services;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
        try (InputStream is = file.getInputStream()) {
            Workbook wb = WorkbookFactory.create(is);
            Sheet sheet = wb.getSheet("Leituras");
            if (sheet == null) {
                throw new InvalidInputException("Aba 'Leituras' não encontrada");
            }

            Row header = sheet.getRow(0);
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
                throw new InvalidInputException("Colunas obrigatórias: Data da Leitura, Hora da Leitura");
            }

            List<String> inputAcronyms = idx.keySet().stream()
                    .filter(col -> !col.equals("Data da Leitura") && !col.equals("Hora da Leitura"))
                    .toList();

            int total = 0;
            for (int rn = 1; rn <= sheet.getLastRowNum(); rn++) {
                Row r = sheet.getRow(rn);
                if (r == null) {
                    break;
                }
                Cell dCell = r.getCell(idxDate);
                Cell tCell = r.getCell(idxHour);

                if ((dCell == null || dCell.getCellType() == CellType.BLANK)
                        && (tCell == null || tCell.getCellType() == CellType.BLANK)) {
                    break;
                }
                total++;

                try {

                    LocalDate date;
                    if (dCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dCell)) {
                        date = dCell.getLocalDateTimeCellValue().toLocalDate();
                    } else {
                        String s = dCell.getStringCellValue().trim();
                        date = LocalDate.parse(s, DATE_FMT);
                    }

                    LocalTime hour;
                    if (tCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(tCell)) {
                        double frac = tCell.getNumericCellValue();
                        hour = LocalTime.ofSecondOfDay((long) (frac * 24 * 3600));
                    } else {
                        String s = tCell.getStringCellValue().trim();
                        hour = LocalTime.parse(s, TIME_FMT);
                    }

                    Map<String, Double> inputValues = new HashMap<>();
                    for (String acr : inputAcronyms) {
                        Cell c = r.getCell(idx.get(acr));
                        if (c == null) {
                            throw new InvalidInputException(
                                    "Linha " + (rn + 1) + ": coluna '" + acr + "' vazia");
                        }
                        double v;
                        if (c.getCellType() == CellType.NUMERIC) {
                            v = c.getNumericCellValue();
                        } else {
                            String ss = c.getStringCellValue().trim().replace(",", ".");
                            try {
                                v = Double.parseDouble(ss);
                            } catch (NumberFormatException ex) {
                                throw new InvalidInputException(
                                        "Linha " + (rn + 1) + ": valor de '" + acr + "' inválido: " + ss);
                            }
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

        } catch (IOException e) {
            throw new InvalidInputException("Erro lendo arquivo Excel: " + e.getMessage());
        }
        return result;
    }
}
