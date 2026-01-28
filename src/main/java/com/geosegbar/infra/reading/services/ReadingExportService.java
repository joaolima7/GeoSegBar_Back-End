package com.geosegbar.infra.reading.services;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.enums.LimitStatusEnum;
import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.OutputEntity;
import com.geosegbar.entities.ReadingEntity;
import com.geosegbar.entities.ReadingInputValueEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.exceptions.UnauthorizedException;
import com.geosegbar.infra.instrument.persistence.jpa.InstrumentRepository;
import com.geosegbar.infra.reading.dtos.GroupedReadingData;
import com.geosegbar.infra.reading.dtos.InputMetadata;
import com.geosegbar.infra.reading.dtos.ReadingExportRequestDTO;
import com.geosegbar.infra.reading.persistence.jpa.ReadingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReadingExportService {

    private final ReadingRepository readingRepository;
    private final InstrumentRepository instrumentRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Transactional(readOnly = true)
    public ByteArrayResource exportToExcel(ReadingExportRequestDTO request) {

        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (!userLogged.getInstrumentationPermission().getViewRead()) {
                throw new UnauthorizedException("Usuário não tem permissão para exportar leituras!");
            }
        }

        if (request.getInstrumentIds() == null || request.getInstrumentIds().isEmpty()) {
            throw new InvalidInputException("É necessário fornecer pelo menos um ID de instrumento!");
        }

        if (request.getInstrumentIds().size() > 50) {
            throw new InvalidInputException("Máximo de 50 instrumentos permitidos por exportação!");
        }

        log.info("Iniciando exportação de leituras para {} instrumentos", request.getInstrumentIds().size());

        try (Workbook workbook = new XSSFWorkbook()) {

            Map<String, CellStyle> styles = createStyles(workbook);

            for (Long instrumentId : request.getInstrumentIds()) {
                processInstrument(workbook, instrumentId, request.getStartDate(), request.getEndDate(), styles);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            byte[] bytes = outputStream.toByteArray();

            log.info("Exportação concluída com sucesso. Tamanho do arquivo: {} bytes", bytes.length);

            return new ByteArrayResource(bytes);

        } catch (Exception e) {
            log.error("Erro ao exportar leituras: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao gerar arquivo Excel: " + e.getMessage(), e);
        }
    }

    private void processInstrument(Workbook workbook, Long instrumentId, LocalDate startDate,
            LocalDate endDate, Map<String, CellStyle> styles) {

        InstrumentEntity instrument = instrumentRepository.findWithCompleteDetailsById(instrumentId)
                .orElseThrow(() -> new NotFoundException("Instrumento não encontrado com ID: " + instrumentId));

        log.info("Processando instrumento: {} (ID: {})", instrument.getName(), instrumentId);

        List<ReadingEntity> readings = readingRepository.findByInstrumentIdForExport(
                instrumentId, startDate, endDate);

        log.info("Encontradas {} leituras para o instrumento {}", readings.size(), instrument.getName());

        Map<String, GroupedReadingData> groupedReadings = groupReadingsByDateTime(readings);

        List<InputMetadata> uniqueInputs = collectUniqueInputs(readings);

        List<OutputEntity> uniqueOutputs = readings.stream()
                .map(ReadingEntity::getOutput)
                .distinct()
                .sorted(Comparator.comparing(OutputEntity::getName))
                .collect(Collectors.toList());

        String sheetName = createSheetName(instrument);
        Sheet sheet = workbook.createSheet(sheetName);

        if (groupedReadings.isEmpty()) {
            createEmptySheet(sheet, instrument, styles);
            return;
        }

        writeHeader(sheet, uniqueInputs, uniqueOutputs, styles);

        writeDataRows(sheet, groupedReadings, uniqueInputs, uniqueOutputs, styles);

        autoSizeColumns(sheet, uniqueInputs, uniqueOutputs);

        log.info("Aba '{}' criada com sucesso", sheetName);
    }

    private Map<String, GroupedReadingData> groupReadingsByDateTime(List<ReadingEntity> readings) {
        Map<String, GroupedReadingData> grouped = new LinkedHashMap<>();

        for (ReadingEntity reading : readings) {
            String key = reading.getDate().toString() + "_" + reading.getHour().toString();

            GroupedReadingData group = grouped.computeIfAbsent(key, k -> {
                GroupedReadingData newGroup = new GroupedReadingData();
                newGroup.setDate(reading.getDate());
                newGroup.setHour(reading.getHour());
                newGroup.setReadings(new ArrayList<>());
                newGroup.setComment(reading.getComment());
                newGroup.setCreatedBy(reading.getUser() != null ? reading.getUser().getName() : "-");

                Map<String, Double> inputValues = new HashMap<>();
                for (ReadingInputValueEntity riv : reading.getInputValues()) {
                    inputValues.put(
                            riv.getInputAcronym(),
                            riv.getValue() != null ? riv.getValue().doubleValue() : 0.0
                    );
                }
                newGroup.setInputValues(inputValues);

                return newGroup;
            });

            group.getReadings().add(reading);
        }

        return grouped;
    }

    private List<InputMetadata> collectUniqueInputs(List<ReadingEntity> readings) {
        Set<InputMetadata> uniqueInputsSet = new LinkedHashSet<>();

        for (ReadingEntity reading : readings) {
            for (ReadingInputValueEntity inputValue : reading.getInputValues()) {
                InputMetadata metadata = new InputMetadata();
                metadata.setAcronym(inputValue.getInputAcronym());
                metadata.setName(inputValue.getInputName());

                metadata.setUnit(getInputUnit(reading.getInstrument(), inputValue.getInputAcronym()));
                uniqueInputsSet.add(metadata);
            }
        }

        return new ArrayList<>(uniqueInputsSet);
    }

    private String getInputUnit(InstrumentEntity instrument, String acronym) {
        return instrument.getInputs().stream()
                .filter(input -> input.getAcronym().equals(acronym))
                .findFirst()
                .map(input -> input.getMeasurementUnit() != null ? input.getMeasurementUnit().getAcronym() : "")
                .orElse("");
    }

    private void writeHeader(Sheet sheet, List<InputMetadata> inputs,
            List<OutputEntity> outputs, Map<String, CellStyle> styles) {
        Row headerRow = sheet.createRow(0);
        int colIndex = 0;

        createHeaderCell(headerRow, colIndex++, "Data Leitura", styles.get("header"));
        createHeaderCell(headerRow, colIndex++, "Hora Leitura", styles.get("header"));

        for (InputMetadata input : inputs) {
            String headerText = input.getAcronym() + " (Input)";
            if (input.getUnit() != null && !input.getUnit().isEmpty()) {
                headerText += "\n" + input.getUnit();
            }
            createHeaderCell(headerRow, colIndex++, headerText, styles.get("header"));
        }

        for (OutputEntity output : outputs) {
            String outputHeader = output.getAcronym() + " (Output)";
            if (output.getMeasurementUnit() != null) {
                outputHeader += "\n" + output.getMeasurementUnit().getAcronym();
            }
            createHeaderCell(headerRow, colIndex++, outputHeader, styles.get("header"));
            createHeaderCell(headerRow, colIndex++, "Status " + output.getAcronym(), styles.get("header"));
        }

        createHeaderCell(headerRow, colIndex++, "Comentário", styles.get("header"));
        createHeaderCell(headerRow, colIndex++, "Criado Por", styles.get("header"));

        headerRow.setHeightInPoints(30);
    }

    private void writeDataRows(Sheet sheet, Map<String, GroupedReadingData> groupedReadings,
            List<InputMetadata> inputs, List<OutputEntity> outputs,
            Map<String, CellStyle> styles) {
        int rowNum = 1;

        for (GroupedReadingData group : groupedReadings.values()) {
            Row row = sheet.createRow(rowNum++);
            int colIndex = 0;

            Cell dateCell = row.createCell(colIndex++);
            dateCell.setCellValue(group.getDate().format(DATE_FORMATTER));
            dateCell.setCellStyle(styles.get("date"));

            Cell timeCell = row.createCell(colIndex++);
            timeCell.setCellValue(group.getHour().format(TIME_FORMATTER));
            timeCell.setCellStyle(styles.get("time"));

            for (InputMetadata input : inputs) {
                Cell cell = row.createCell(colIndex++);
                Double value = group.getInputValues().get(input.getAcronym());
                if (value != null) {
                    cell.setCellValue(value);
                    cell.setCellStyle(styles.get("number"));
                } else {
                    cell.setCellValue("-");
                    cell.setCellStyle(styles.get("empty"));
                }
            }

            Map<Long, ReadingEntity> outputReadingMap = group.getReadings().stream()
                    .collect(Collectors.toMap(r -> r.getOutput().getId(), r -> r));

            for (OutputEntity output : outputs) {
                ReadingEntity reading = outputReadingMap.get(output.getId());

                Cell valueCell = row.createCell(colIndex++);
                if (reading != null) {
                    valueCell.setCellValue(reading.getCalculatedValue());
                    valueCell.setCellStyle(styles.get("number"));
                } else {
                    valueCell.setCellValue("-");
                    valueCell.setCellStyle(styles.get("empty"));
                }

                Cell statusCell = row.createCell(colIndex++);
                if (reading != null) {
                    String statusText = translateLimitStatus(reading.getLimitStatus());
                    statusCell.setCellValue(statusText);
                    statusCell.setCellStyle(styles.get("text"));
                } else {
                    statusCell.setCellValue("-");
                    statusCell.setCellStyle(styles.get("empty"));
                }
            }

            Cell commentCell = row.createCell(colIndex++);
            commentCell.setCellValue(group.getComment() != null ? group.getComment() : "-");
            commentCell.setCellStyle(styles.get("text"));

            Cell createdByCell = row.createCell(colIndex++);
            createdByCell.setCellValue(group.getCreatedBy());
            createdByCell.setCellStyle(styles.get("text"));
        }
    }

    private void createEmptySheet(Sheet sheet, InstrumentEntity instrument, Map<String, CellStyle> styles) {
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        cell.setCellValue("Nenhuma leitura encontrada para o instrumento: " + instrument.getName());
        cell.setCellStyle(styles.get("header"));
        sheet.setColumnWidth(0, 15000);
    }

    private void createHeaderCell(Row row, int colIndex, String value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private String createSheetName(InstrumentEntity instrument) {
        String baseName = instrument.getName() + " - " + instrument.getInstrumentType().getName();

        if (baseName.length() > 31) {
            baseName = baseName.substring(0, 28) + "...";
        }

        baseName = baseName.replaceAll("[\\\\/?*\\[\\]:]", "_");

        return baseName;
    }

    private String translateLimitStatus(LimitStatusEnum status) {
        if (status == null) {
            return "-";
        }

        switch (status) {
            case NORMAL:
                return "Normal";
            case INFERIOR:
                return "Inferior";
            case SUPERIOR:
                return "Superior";
            case ATENCAO:
                return "Atenção";
            case ALERTA:
                return "Alerta";
            case EMERGENCIA:
                return "Emergência";
            default:
                return status.name();
        }
    }

    private void autoSizeColumns(Sheet sheet, List<InputMetadata> inputs, List<OutputEntity> outputs) {
        int totalColumns = 2 + inputs.size() + (outputs.size() * 2) + 2;

        for (int i = 0; i < totalColumns; i++) {
            sheet.autoSizeColumn(i);

            int currentWidth = sheet.getColumnWidth(i);
            int minWidth = 3000;
            int maxWidth = 15000;

            if (currentWidth < minWidth) {
                sheet.setColumnWidth(i, minWidth);
            } else if (currentWidth > maxWidth) {
                sheet.setColumnWidth(i, maxWidth);
            }
        }

        int commentColumnIndex = 2 + inputs.size() + (outputs.size() * 2);
        sheet.setColumnWidth(commentColumnIndex, 20000);
    }

    private Map<String, CellStyle> createStyles(Workbook workbook) {
        Map<String, CellStyle> styles = new LinkedHashMap<>();

        Font defaultFont = workbook.createFont();
        defaultFont.setFontName("Arial");
        defaultFont.setFontHeightInPoints((short) 10);

        Font headerFont = workbook.createFont();
        headerFont.setFontName("Arial");
        headerFont.setFontHeightInPoints((short) 10);
        headerFont.setBold(true);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setWrapText(true);
        styles.put("header", headerStyle);

        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.setFont(defaultFont);
        dateStyle.setBorderTop(BorderStyle.THIN);
        dateStyle.setBorderBottom(BorderStyle.THIN);
        dateStyle.setBorderLeft(BorderStyle.THIN);
        dateStyle.setBorderRight(BorderStyle.THIN);
        dateStyle.setAlignment(HorizontalAlignment.CENTER);
        dateStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        styles.put("date", dateStyle);

        CellStyle timeStyle = workbook.createCellStyle();
        timeStyle.setFont(defaultFont);
        timeStyle.setBorderTop(BorderStyle.THIN);
        timeStyle.setBorderBottom(BorderStyle.THIN);
        timeStyle.setBorderLeft(BorderStyle.THIN);
        timeStyle.setBorderRight(BorderStyle.THIN);
        timeStyle.setAlignment(HorizontalAlignment.CENTER);
        timeStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        styles.put("time", timeStyle);

        CellStyle numberStyle = workbook.createCellStyle();
        numberStyle.setFont(defaultFont);
        numberStyle.setBorderTop(BorderStyle.THIN);
        numberStyle.setBorderBottom(BorderStyle.THIN);
        numberStyle.setBorderLeft(BorderStyle.THIN);
        numberStyle.setBorderRight(BorderStyle.THIN);
        numberStyle.setAlignment(HorizontalAlignment.CENTER);
        numberStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        styles.put("number", numberStyle);

        CellStyle textStyle = workbook.createCellStyle();
        textStyle.setFont(defaultFont);
        textStyle.setBorderTop(BorderStyle.THIN);
        textStyle.setBorderBottom(BorderStyle.THIN);
        textStyle.setBorderLeft(BorderStyle.THIN);
        textStyle.setBorderRight(BorderStyle.THIN);
        textStyle.setAlignment(HorizontalAlignment.CENTER);
        textStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        textStyle.setWrapText(true);
        styles.put("text", textStyle);

        Font emptyFont = workbook.createFont();
        emptyFont.setFontName("Arial");
        emptyFont.setFontHeightInPoints((short) 10);
        emptyFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

        CellStyle emptyStyle = workbook.createCellStyle();
        emptyStyle.setFont(emptyFont);
        emptyStyle.setBorderTop(BorderStyle.THIN);
        emptyStyle.setBorderBottom(BorderStyle.THIN);
        emptyStyle.setBorderLeft(BorderStyle.THIN);
        emptyStyle.setBorderRight(BorderStyle.THIN);
        emptyStyle.setAlignment(HorizontalAlignment.CENTER);
        emptyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        styles.put("empty", emptyStyle);

        return styles;
    }
}
