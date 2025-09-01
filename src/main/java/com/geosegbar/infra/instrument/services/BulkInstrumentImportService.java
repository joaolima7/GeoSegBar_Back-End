package com.geosegbar.infra.instrument.services;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.InstrumentTypeEntity;
import com.geosegbar.entities.MeasurementUnitEntity;
import com.geosegbar.entities.SectionEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.infra.dam.services.DamService;
import com.geosegbar.infra.instrument.dtos.ConstantDTO;
import com.geosegbar.infra.instrument.dtos.CreateInstrumentRequest;
import com.geosegbar.infra.instrument.dtos.DeterministicLimitDTO;
import com.geosegbar.infra.instrument.dtos.ImportInstrumentsRequest;
import com.geosegbar.infra.instrument.dtos.InputDTO;
import com.geosegbar.infra.instrument.dtos.OutputDTO;
import com.geosegbar.infra.instrument.dtos.StatisticalLimitDTO;
import com.geosegbar.infra.instrument_type.persistence.jpa.InstrumentTypeRepository;
import com.geosegbar.infra.measurement_unit.persistence.jpa.MeasurementUnitRepository;
import com.geosegbar.infra.section.persistence.jpa.SectionRepository;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkInstrumentImportService {

    private final InstrumentService instrumentService;
    private final DamService damService;
    private final MeasurementUnitRepository muRepository;
    private final SectionRepository sectionRepository;
    private final InstrumentTypeRepository instrumentTypeRepository;

    @Data
    public static class ImportResult {

        private int total;
        private int success;
        private int failures;
        private List<String> errors = new ArrayList<>();
    }

    public ImportResult importFromExcel(ImportInstrumentsRequest meta, MultipartFile file) {
        ImportResult result = new ImportResult();

        Map<String, Long> unitMap = muRepository.findAll().stream()
                .collect(Collectors.toMap(MeasurementUnitEntity::getAcronym, MeasurementUnitEntity::getId));

        // Criar mapa de tipos de instrumentos pelo nome (em uppercase para comparação case-insensitive)
        Map<String, InstrumentTypeEntity> instrumentTypesByName = instrumentTypeRepository.findAll().stream()
                .collect(Collectors.toMap(
                        type -> type.getName().toUpperCase(),
                        type -> type
                ));

        Map<String, SectionEntity> sectionsByName = sectionRepository
                .findAllByDamId(meta.getDamId())
                .stream()
                .collect(Collectors.toMap(SectionEntity::getName, s -> s));

        DamEntity dam = damService.findById(meta.getDamId());

        try (InputStream is = file.getInputStream()) {
            Workbook wb = WorkbookFactory.create(is);

            Map<String, InstrumentRow> instruments = parseInstrumentSheet(wb.getSheet("Instruments"));
            Map<String, List<InputDTO>> inputs = parseComponent(wb.getSheet("Inputs"), unitMap, InputDTO.class);
            Map<String, List<ConstantDTO>> constants = parseComponent(wb.getSheet("Constants"), unitMap, ConstantDTO.class);
            Map<String, List<OutputDTO>> outputs = parseComponent(wb.getSheet("Outputs"), unitMap, OutputDTO.class);
            Map<Key, LimitData> limits = parseLimits(wb.getSheet("Limits"));

            result.setTotal(instruments.size());

            for (InstrumentRow ir : instruments.values()) {
                // Validar se a seção existe
                Long sectionId = null;
                if (ir.sectionName != null && !ir.sectionName.isBlank()) {
                    SectionEntity sec = sectionsByName.get(ir.sectionName);
                    if (sec == null) {
                        throw new InvalidInputException(
                                String.format("Seção '%s' não encontrada para barragem %s",
                                        ir.sectionName, dam.getName())
                        );
                    }
                    sectionId = sec.getId();
                }

                // Validar se o tipo de instrumento existe
                String instrumentTypeName = ir.instrumentTypeName != null ? ir.instrumentTypeName.trim().toUpperCase() : null;
                if (instrumentTypeName == null || instrumentTypeName.isBlank()) {
                    throw new InvalidInputException("Tipo de instrumento é obrigatório para o instrumento: " + ir.id);
                }

                InstrumentTypeEntity instrumentType = instrumentTypesByName.get(instrumentTypeName);
                if (instrumentType == null) {
                    throw new InvalidInputException("Tipo de instrumento não encontrado: " + ir.instrumentTypeName);
                }

                CreateInstrumentRequest req = ir.toRequest(meta, sectionId);
                req.setInstrumentTypeId(instrumentType.getId()); // Usar o ID do tipo encontrado

                req.setInputs(inputs.getOrDefault(ir.id, Collections.emptyList()));
                req.setConstants(constants.getOrDefault(ir.id, Collections.emptyList()));

                List<OutputDTO> outs = outputs.getOrDefault(ir.id, Collections.emptyList());
                for (OutputDTO o : outs) {
                    LimitData ld = limits.get(new Key(ir.id, o.getAcronym()));
                    if (ld != null && Boolean.FALSE.equals(req.getNoLimit())) {
                        if (ld.isStatistical) {
                            o.setStatisticalLimit(ld.statDTO);
                        } else {
                            o.setDeterministicLimit(ld.detDTO);
                        }
                    }
                }
                req.setOutputs(outs);

                try {
                    instrumentService.createComplete(req);
                    result.success++;
                } catch (DuplicateResourceException dre) {
                    String msg = String.format("Instrument %s (linha %d): %s",
                            ir.id, ir.row, dre.getMessage());
                    result.failures++;
                    result.errors.add(msg);
                    log.warn("Pulando duplicata: {}", msg);
                } catch (Exception e) {
                    String msg = String.format("Instrument %s (linha %d): %s",
                            ir.id, ir.row, e.getMessage());
                    result.failures++;
                    result.errors.add(msg);
                    log.error("Erro importando {}: {}", ir.id, e.getMessage());
                }
            }

        } catch (Exception e) {
            throw new InvalidInputException("Erro ao processar Excel: " + e.getMessage());
        }

        return result;
    }

    private Map<String, InstrumentRow> parseInstrumentSheet(Sheet sheet) {
        Iterator<Row> it = sheet.iterator();
        if (!it.hasNext()) {
            throw new InvalidInputException("Aba Instruments está vazia");
        }
        Map<String, Integer> idx = headerIndex(it.next());
        Map<String, InstrumentRow> map = new LinkedHashMap<>();
        int rowNum = 1;
        while (it.hasNext()) {
            rowNum++;
            Row r = it.next();
            String id = getString(r, idx, "ID");
            if (id == null || id.isBlank()) {
                continue;
            }
            InstrumentRow ir = new InstrumentRow();
            ir.id = id;
            ir.row = rowNum;
            ir.name = getString(r, idx, "Nome");
            ir.location = getString(r, idx, "Localização");
            ir.distanceOffset = getDouble(r, idx, "Distância");
            ir.latitude = getDouble(r, idx, "Latitude");
            ir.longitude = getDouble(r, idx, "Longitude");
            ir.noLimit = getBoolean(r, idx, "Sem Limites");
            ir.instrumentTypeName = getString(r, idx, "Tipo de Instrumento"); // Renomeado para evitar confusão
            ir.sectionName = getString(r, idx, "Seção");
            map.put(id, ir);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private <T> Map<String, List<T>> parseComponent(Sheet sheet,
            Map<String, Long> unitMap, Class<T> clz) {
        Iterator<Row> it = sheet.iterator();
        if (!it.hasNext()) {
            return Collections.emptyMap();
        }
        Map<String, Integer> idx = headerIndex(it.next());
        Map<String, List<T>> map = new HashMap<>();
        while (it.hasNext()) {
            Row row = it.next();
            String id = getString(row, idx, "ID");
            if (id == null || id.isBlank()) {
                continue;
            }

            T dto;
            if (clz.equals(InputDTO.class)) {
                InputDTO d = new InputDTO();
                d.setAcronym(getString(row, idx, "Sigla"));
                d.setName(getString(row, idx, "Nome"));
                d.setPrecision(getInt(row, idx, "Precisão"));
                d.setMeasurementUnitId(fetchUnit(getString(row, idx, "Unidade de Medida"), unitMap));
                dto = (T) d;

            } else if (clz.equals(ConstantDTO.class)) {
                ConstantDTO d = new ConstantDTO();
                d.setAcronym(getString(row, idx, "Sigla"));
                d.setName(getString(row, idx, "Nome"));
                d.setPrecision(getInt(row, idx, "Precisão"));
                d.setValue(getDouble(row, idx, "Valor"));
                d.setMeasurementUnitId(fetchUnit(getString(row, idx, "Unidade de Medida"), unitMap));
                dto = (T) d;

            } else {
                OutputDTO d = new OutputDTO();
                d.setAcronym(getString(row, idx, "Sigla"));
                d.setName(getString(row, idx, "Nome"));
                d.setEquation(getString(row, idx, "Equação"));
                d.setPrecision(getInt(row, idx, "Precisão"));
                d.setMeasurementUnitId(fetchUnit(getString(row, idx, "Unidade de Medida"), unitMap));
                dto = (T) d;
            }
            map.computeIfAbsent(id, k -> new ArrayList<>()).add(dto);
        }
        return map;
    }

    private Map<Key, LimitData> parseLimits(Sheet sheet) {
        Iterator<Row> it = sheet.iterator();
        if (!it.hasNext()) {
            return Collections.emptyMap();
        }

        Map<String, Integer> idx = headerIndex(it.next());
        Map<Key, LimitData> map = new HashMap<>();
        while (it.hasNext()) {
            Row row = it.next();
            String id = getString(row, idx, "ID");
            String out = getString(row, idx, "Sigla do Output");
            if (id == null || out == null) {
                continue;
            }

            String type = getString(row, idx, "Tipo de Limite");
            LimitData ld = new LimitData();
            if ("Estatistico".equalsIgnoreCase(type)) {
                ld.isStatistical = true;
                ld.statDTO = new StatisticalLimitDTO(
                        null,
                        getDouble(row, idx, "Valor Inferior"),
                        getDouble(row, idx, "Valor Superior")
                );
            } else {
                ld.isStatistical = false;
                ld.detDTO = new DeterministicLimitDTO(
                        null,
                        getDouble(row, idx, "Valor de Atenção"),
                        getDouble(row, idx, "Valor de Alerta"),
                        getDouble(row, idx, "Valor de Emergência")
                );
            }
            map.put(new Key(id, out), ld);
        }
        return map;
    }

    private Map<String, Integer> headerIndex(Row header) {
        Map<String, Integer> idx = new HashMap<>();
        for (Cell c : header) {
            idx.put(c.getStringCellValue(), c.getColumnIndex());
        }
        return idx;
    }

    private String getString(Row r, Map<String, Integer> ix, String col) {
        Integer i = ix.get(col);
        if (i == null) {
            return null;
        }
        Cell c = r.getCell(i);
        if (c == null) {
            return null;
        }
        return switch (c.getCellType()) {
            case STRING ->
                c.getStringCellValue();
            case NUMERIC ->
                BigDecimal.valueOf(c.getNumericCellValue())
                .stripTrailingZeros()
                .toPlainString();
            case BOOLEAN ->
                Boolean.toString(c.getBooleanCellValue());
            case FORMULA ->
                c.getCachedFormulaResultType() == CellType.STRING
                ? c.getStringCellValue()
                : String.valueOf(c.getNumericCellValue());
            default ->
                null;
        };
    }

    private Double getDouble(Row r, Map<String, Integer> ix, String col) {
        Integer i = ix.get(col);
        if (i == null) {
            return null;
        }
        Cell c = r.getCell(i);
        return (c != null && c.getCellType() == CellType.NUMERIC)
                ? c.getNumericCellValue() : null;
    }

    private Integer getInt(Row r, Map<String, Integer> ix, String col) {
        Double d = getDouble(r, ix, col);
        return d == null ? null : d.intValue();
    }

    private Boolean getBoolean(Row r, Map<String, Integer> ix, String col) {
        String v = getString(r, ix, col);
        return v == null ? null : Boolean.valueOf(v);
    }

    private Long fetchUnit(String acr, Map<String, Long> unitMap) {
        Long id = unitMap.get(acr);
        if (id == null) {
            throw new InvalidInputException("Unidade não encontrada: " + acr);
        }
        return id;
    }

    private record Key(String id, String out) {

    }

    private static class LimitData {

        boolean isStatistical;
        StatisticalLimitDTO statDTO;
        DeterministicLimitDTO detDTO;
    }

    private static class InstrumentRow {

        String id;
        int row;
        String name, location, instrumentTypeName;
        Double distanceOffset, latitude, longitude;
        Boolean noLimit;
        String sectionName;

        CreateInstrumentRequest toRequest(ImportInstrumentsRequest meta, Long resolvedSectionId) {
            CreateInstrumentRequest r = new CreateInstrumentRequest();
            r.setName(name);
            r.setLocation(location);
            r.setDistanceOffset(distanceOffset);
            r.setLatitude(latitude);
            r.setLongitude(longitude);
            r.setNoLimit(noLimit != null ? noLimit : meta.getNoLimit());
            r.setActiveForSection(meta.getActiveForSection());
            r.setDamId(meta.getDamId());
            r.setSectionId(resolvedSectionId);
            return r;
        }
    }
}
