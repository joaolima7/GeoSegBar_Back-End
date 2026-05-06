package com.geosegbar.infra.section_rendering_config.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.enums.LimitStatusEnum;
import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.entities.ConstantEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.OutputEntity;
import com.geosegbar.entities.ReadingEntity;
import com.geosegbar.entities.ReservoirEntity;
import com.geosegbar.entities.SectionCustomLevelEntity;
import com.geosegbar.entities.SectionEntity;
import com.geosegbar.entities.SectionRenderingConfigEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.exceptions.UnauthorizedException;
import com.geosegbar.infra.instrument.persistence.jpa.InstrumentRepository;
import com.geosegbar.infra.reading.persistence.jpa.ReadingRepository;
import com.geosegbar.infra.reservoir.persistence.ReservoirRepository;
import com.geosegbar.infra.section.persistence.jpa.SectionRepository;
import com.geosegbar.infra.section_rendering_config.dtos.ElevationVariableDTO;
import com.geosegbar.infra.section_rendering_config.dtos.ElevationVariableDTO.VariableType;
import com.geosegbar.infra.section_rendering_config.dtos.InstrumentMeasurementUnitDTO;
import com.geosegbar.infra.section_rendering_config.dtos.SectionCustomLevelDTO;
import com.geosegbar.infra.section_rendering_config.dtos.SectionRenderDataDTO;
import com.geosegbar.infra.section_rendering_config.dtos.SectionRenderInstrumentDTO;
import com.geosegbar.infra.section_rendering_config.dtos.SectionRenderReservoirDTO;
import com.geosegbar.infra.section_rendering_config.dtos.SectionRenderTelemetricInstrumentDTO;
import com.geosegbar.infra.section_rendering_config.dtos.SectionRenderingConfigResponseDTO;
import com.geosegbar.infra.section_rendering_config.dtos.UpdateSectionRenderingConfigRequest;
import com.geosegbar.infra.section_rendering_config.persistence.jpa.SectionRenderingConfigRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SectionRenderingConfigService {

    private static final String TRANSLATE_FROM = "àáâãäåçèéêëìíîïñòóôõöùúûüý";
    private static final String TRANSLATE_TO   = "aaaaaaceeeeiiiinooooouuuuy";

    private final SectionRenderingConfigRepository configRepository;
    private final SectionRepository sectionRepository;
    private final InstrumentRepository instrumentRepository;
    private final ReservoirRepository reservoirRepository;
    private final ReadingRepository readingRepository;

    @Transactional(readOnly = true)
    public SectionRenderingConfigResponseDTO getBySectionId(Long sectionId) {
        checkViewPermission();

        if (!sectionRepository.existsById(sectionId)) {
            throw new NotFoundException("Seção não encontrada com ID: " + sectionId);
        }

        return configRepository.findBySectionId(sectionId)
                .map(this::toResponseDTO)
                .orElseGet(() -> defaultResponse(sectionId));
    }

    @Transactional
    public SectionRenderingConfigResponseDTO upsert(Long sectionId, UpdateSectionRenderingConfigRequest req) {
        checkEditPermission();

        SectionEntity section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new NotFoundException("Seção não encontrada com ID: " + sectionId));

        Long damId = section.getDam() != null ? section.getDam().getId() : null;

        SectionRenderingConfigEntity config = configRepository.findBySectionId(sectionId)
                .orElseGet(() -> {
                    SectionRenderingConfigEntity c = new SectionRenderingConfigEntity();
                    c.setSection(section);
                    return c;
                });

        applyScalars(config, req);
        applyCustomLevels(config, req.getCustomLevels());
        applySelectedInstruments(config, sectionId, req.getSelectedInstrumentIds());
        applySelectedReservoirs(config, damId, req.getSelectedReservoirIds());

        SectionRenderingConfigEntity saved = configRepository.save(config);
        return toResponseDTO(saved);
    }

    private void applyScalars(SectionRenderingConfigEntity config, UpdateSectionRenderingConfigRequest req) {
        config.setSoilLabel(req.getSoilLabel());
        config.setSoilColor(req.getSoilColor());
        config.setFilterLabel(req.getFilterLabel());
        config.setFilterColor(req.getFilterColor());
        config.setRockLabel(req.getRockLabel());
        config.setRockColor(req.getRockColor());
        config.setTopElevationColor(req.getTopElevationColor());
        config.setBottomElevationColor(req.getBottomElevationColor());
        config.setPiezometricElevationColor(req.getPiezometricElevationColor());
        config.setAxisXMin(req.getAxisXMin());
        config.setAxisXMax(req.getAxisXMax());
        config.setAxisYMin(req.getAxisYMin());
        config.setAxisYMax(req.getAxisYMax());
        config.setShowDamAxis(Boolean.TRUE.equals(req.getShowDamAxis()));
        config.setShowLastUpstreamReading(Boolean.TRUE.equals(req.getShowLastUpstreamReading()));
        config.setShowLastDownstreamReading(Boolean.TRUE.equals(req.getShowLastDownstreamReading()));
    }

    private void applyCustomLevels(SectionRenderingConfigEntity config, List<SectionCustomLevelDTO> incoming) {
        config.getCustomLevels().clear();
        if (incoming == null || incoming.isEmpty()) {
            return;
        }
        for (SectionCustomLevelDTO dto : incoming) {
            SectionCustomLevelEntity level = new SectionCustomLevelEntity();
            level.setConfig(config);
            level.setName(dto.getName());
            level.setValue(dto.getValue());
            level.setColor(dto.getColor());
            level.setEnabled(dto.getEnabled() == null ? Boolean.TRUE : dto.getEnabled());
            config.getCustomLevels().add(level);
        }
    }

    private void applySelectedInstruments(SectionRenderingConfigEntity config, Long sectionId, List<Long> ids) {
        config.getSelectedInstruments().clear();
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<Long> distinctIds = ids.stream().distinct().toList();
        List<InstrumentEntity> instruments = instrumentRepository.findAllById(distinctIds);
        if (instruments.size() != distinctIds.size()) {
            throw new InvalidInputException("Um ou mais instrumentos informados não existem.");
        }
        for (InstrumentEntity instrument : instruments) {
            if (instrument.getSection() == null || !sectionId.equals(instrument.getSection().getId())) {
                throw new InvalidInputException(
                        "Instrumento ID " + instrument.getId() + " não pertence à seção informada.");
            }
        }
        config.getSelectedInstruments().addAll(new HashSet<>(instruments));
    }

    private void applySelectedReservoirs(SectionRenderingConfigEntity config, Long damId, List<Long> ids) {
        config.getSelectedReservoirs().clear();
        if (ids == null || ids.isEmpty()) {
            return;
        }
        if (damId == null) {
            throw new InvalidInputException("A seção não está associada a nenhuma barragem.");
        }
        List<Long> distinctIds = ids.stream().distinct().toList();
        List<ReservoirEntity> reservoirs = reservoirRepository.findAllById(distinctIds);
        if (reservoirs.size() != distinctIds.size()) {
            throw new InvalidInputException("Um ou mais reservatórios informados não existem.");
        }
        for (ReservoirEntity reservoir : reservoirs) {
            if (reservoir.getDam() == null || !damId.equals(reservoir.getDam().getId())) {
                throw new InvalidInputException(
                        "Reservatório ID " + reservoir.getId() + " não pertence à barragem desta seção.");
            }
        }
        config.getSelectedReservoirs().addAll(new HashSet<>(reservoirs));
    }

    private SectionRenderingConfigResponseDTO toResponseDTO(SectionRenderingConfigEntity c) {
        SectionRenderingConfigResponseDTO dto = new SectionRenderingConfigResponseDTO();
        dto.setId(c.getId());
        dto.setSectionId(c.getSection() != null ? c.getSection().getId() : null);
        dto.setSoilLabel(c.getSoilLabel());
        dto.setSoilColor(c.getSoilColor());
        dto.setFilterLabel(c.getFilterLabel());
        dto.setFilterColor(c.getFilterColor());
        dto.setRockLabel(c.getRockLabel());
        dto.setRockColor(c.getRockColor());
        dto.setTopElevationColor(c.getTopElevationColor());
        dto.setBottomElevationColor(c.getBottomElevationColor());
        dto.setPiezometricElevationColor(c.getPiezometricElevationColor());
        dto.setAxisXMin(c.getAxisXMin());
        dto.setAxisXMax(c.getAxisXMax());
        dto.setAxisYMin(c.getAxisYMin());
        dto.setAxisYMax(c.getAxisYMax());
        dto.setShowDamAxis(c.getShowDamAxis());
        dto.setShowLastUpstreamReading(c.getShowLastUpstreamReading());
        dto.setShowLastDownstreamReading(c.getShowLastDownstreamReading());

        List<SectionCustomLevelDTO> levels = c.getCustomLevels().stream()
                .map(l -> new SectionCustomLevelDTO(
                        l.getId(), l.getName(), l.getValue(), l.getColor(),
                        l.getEnabled() == null ? Boolean.TRUE : l.getEnabled()))
                .collect(Collectors.toList());
        dto.setCustomLevels(levels);

        Set<Long> instrumentIds = c.getSelectedInstruments().stream()
                .map(InstrumentEntity::getId)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        dto.setSelectedInstrumentIds(new ArrayList<>(instrumentIds));

        Set<Long> reservoirIds = c.getSelectedReservoirs().stream()
                .map(ReservoirEntity::getId)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        dto.setSelectedReservoirIds(new ArrayList<>(reservoirIds));

        return dto;
    }

    @Transactional(readOnly = true)
    public SectionRenderDataDTO getRenderData(Long sectionId) {
        checkViewPermission();

        SectionEntity section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new NotFoundException("Seção não encontrada com ID: " + sectionId));

        Long damId = section.getDam() != null ? section.getDam().getId() : null;

        SectionRenderingConfigEntity config = configRepository.findBySectionId(sectionId).orElse(null);

        Set<Long> selectedInstrumentIds = new HashSet<>();
        Set<Long> selectedReservoirIds = new HashSet<>();
        if (config != null) {
            for (InstrumentEntity sel : config.getSelectedInstruments()) {
                selectedInstrumentIds.add(sel.getId());
            }
            for (ReservoirEntity sel : config.getSelectedReservoirs()) {
                selectedReservoirIds.add(sel.getId());
            }
        }

        // ---- piezômetros -------------------------------------------------------
        List<InstrumentEntity> piezometerEntities =
                instrumentRepository.findPiezometersBySectionWithOutputsAndConstants(sectionId);

        // Collect all output IDs from piezometers that are elevation outputs
        List<Long> piezometerOutputIds = piezometerEntities.stream()
                .flatMap(i -> i.getOutputs().stream())
                .filter(OutputEntity::getActive)
                .map(OutputEntity::getId)
                .collect(Collectors.toList());

        Map<Long, ReadingEntity> latestByOutputId = fetchLatestReadingsByOutputIds(piezometerOutputIds);

        List<SectionRenderInstrumentDTO> piezometers = piezometerEntities.stream()
                .map(p -> {
                    SectionRenderInstrumentDTO dto = new SectionRenderInstrumentDTO();
                    dto.setId(p.getId());
                    dto.setName(p.getName());
                    dto.setDistanceOffset(p.getDistanceOffset());
                    dto.setIsSelected(selectedInstrumentIds.contains(p.getId()));
                    dto.setTopElevation(resolveTopElevation(p, latestByOutputId));
                    dto.setBottomElevation(resolveBottomElevation(p, latestByOutputId));
                    dto.setPiezometricElevation(resolvePiezometricElevation(p, latestByOutputId));
                    return dto;
                })
                .collect(Collectors.toList());

        // ---- reservatórios e telemetria ----------------------------------------
        List<SectionRenderReservoirDTO> reservoirs = new ArrayList<>();
        SectionRenderTelemetricInstrumentDTO upstream = null;
        SectionRenderTelemetricInstrumentDTO downstream = null;

        if (damId != null) {
            List<ReservoirEntity> damReservoirs = reservoirRepository.findByDamIdOrderByCreatedAtDesc(damId);
            for (ReservoirEntity r : damReservoirs) {
                reservoirs.add(new SectionRenderReservoirDTO(
                        r.getId(),
                        r.getLevel() != null ? r.getLevel().getId() : null,
                        r.getLevel() != null ? r.getLevel().getName() : null,
                        r.getLevel() != null ? r.getLevel().getValue() : null,
                        r.getLevel() != null ? r.getLevel().getUnitLevel() : null,
                        selectedReservoirIds.contains(r.getId())));
            }

            List<InstrumentEntity> telemetricEntities =
                    instrumentRepository.findActiveTelemetricByDamWithOutputsAndConstants(damId);

            List<Long> telemetricOutputIds = telemetricEntities.stream()
                    .flatMap(i -> i.getOutputs().stream())
                    .filter(OutputEntity::getActive)
                    .map(OutputEntity::getId)
                    .collect(Collectors.toList());

            Map<Long, ReadingEntity> latestByTelemetricOutput = fetchLatestReadingsByOutputIds(telemetricOutputIds);

            for (InstrumentEntity t : telemetricEntities) {
                SectionRenderTelemetricInstrumentDTO dto = new SectionRenderTelemetricInstrumentDTO();
                dto.setId(t.getId());
                dto.setName(t.getName());
                dto.setIsLinimetricRuler(t.getIsLinimetricRuler());
                dto.setIsDownstream(t.getIsDownstream());
                dto.setTopElevation(resolveTopElevation(t, latestByTelemetricOutput));
                dto.setBottomElevation(resolveBottomElevation(t, latestByTelemetricOutput));
                dto.setPiezometricElevation(resolvePiezometricElevation(t, latestByTelemetricOutput));
                if (Boolean.TRUE.equals(t.getIsLinimetricRuler())) {
                    upstream = dto;
                } else if (Boolean.TRUE.equals(t.getIsDownstream())) {
                    downstream = dto;
                }
            }
        }

        // ---- config scalars ----------------------------------------------------
        SectionRenderDataDTO result = new SectionRenderDataDTO();
        result.setSectionId(sectionId);

        if (config != null) {
            result.setConfigId(config.getId());
            result.setSoilLabel(config.getSoilLabel());
            result.setSoilColor(config.getSoilColor());
            result.setFilterLabel(config.getFilterLabel());
            result.setFilterColor(config.getFilterColor());
            result.setRockLabel(config.getRockLabel());
            result.setRockColor(config.getRockColor());
            result.setTopElevationColor(config.getTopElevationColor());
            result.setBottomElevationColor(config.getBottomElevationColor());
            result.setPiezometricElevationColor(config.getPiezometricElevationColor());
            result.setAxisXMin(config.getAxisXMin());
            result.setAxisXMax(config.getAxisXMax());
            result.setAxisYMin(config.getAxisYMin());
            result.setAxisYMax(config.getAxisYMax());
            result.setShowDamAxis(config.getShowDamAxis());
            result.setShowLastUpstreamReading(config.getShowLastUpstreamReading());
            result.setShowLastDownstreamReading(config.getShowLastDownstreamReading());

            List<SectionCustomLevelDTO> customLevels = config.getCustomLevels().stream()
                    .map(l -> new SectionCustomLevelDTO(l.getId(), l.getName(), l.getValue(),
                            l.getColor(), Boolean.TRUE.equals(l.getEnabled())))
                    .collect(Collectors.toList());
            result.setCustomLevels(customLevels);
        }

        result.setPiezometers(piezometers);
        result.setReservoirs(reservoirs);
        result.setUpstreamInstrument(upstream);
        result.setDownstreamInstrument(downstream);

        return result;
    }

    // Fetches the single most-recent active reading for each output in one query.
    private Map<Long, ReadingEntity> fetchLatestReadingsByOutputIds(List<Long> outputIds) {
        if (outputIds.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = readingRepository.findLatestReadingIdsByOutputIds(outputIds);
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<ReadingEntity> readings = readingRepository.findByIdsWithAllRelations(ids);
        Map<Long, ReadingEntity> map = new HashMap<>();
        for (ReadingEntity r : readings) {
            map.put(r.getOutput().getId(), r);
        }
        return map;
    }

    // Normalises a name for fuzzy matching: lower-case, strip diacritics, collapse spaces.
    private static String norm(String s) {
        if (s == null) return "";
        String lower = s.toLowerCase();
        StringBuilder sb = new StringBuilder(lower.length());
        for (char c : lower.toCharArray()) {
            int idx = TRANSLATE_FROM.indexOf(c);
            sb.append(idx >= 0 ? TRANSLATE_TO.charAt(idx) : c);
        }
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    private static boolean isTopElevation(String name) {
        String n = norm(name);
        return n.contains("cota de topo") || n.contains("cota de boca")
                || (n.contains("cota") && n.contains("topo"))
                || (n.contains("cota") && n.contains("boca"));
    }

    private static boolean isBottomElevation(String name) {
        String n = norm(name);
        return n.contains("cota de fundo") || n.contains("cota de instalacao")
                || (n.contains("cota") && n.contains("fundo"))
                || (n.contains("cota") && n.contains("instalacao"));
    }

    private static boolean isPiezometricElevation(String name) {
        String n = norm(name);
        return n.contains("cota piezometrica") || n.contains("cota piezometrico")
                || (n.contains("cota") && n.contains("piezometr"));
    }

    // top elevation: prefer constant over output
    private ElevationVariableDTO resolveTopElevation(InstrumentEntity i, Map<Long, ReadingEntity> latestByOutput) {
        for (ConstantEntity c : i.getConstants()) {
            if (isTopElevation(c.getName())) return toElevationDTO(c);
        }
        for (OutputEntity o : i.getOutputs()) {
            if (Boolean.TRUE.equals(o.getActive()) && isTopElevation(o.getName())) {
                return toElevationDTO(o, latestByOutput.get(o.getId()));
            }
        }
        return null;
    }

    // bottom elevation: prefer constant over output
    private ElevationVariableDTO resolveBottomElevation(InstrumentEntity i, Map<Long, ReadingEntity> latestByOutput) {
        for (ConstantEntity c : i.getConstants()) {
            if (isBottomElevation(c.getName())) return toElevationDTO(c);
        }
        for (OutputEntity o : i.getOutputs()) {
            if (Boolean.TRUE.equals(o.getActive()) && isBottomElevation(o.getName())) {
                return toElevationDTO(o, latestByOutput.get(o.getId()));
            }
        }
        return null;
    }

    // piezometric elevation: prefer output over constant
    private ElevationVariableDTO resolvePiezometricElevation(InstrumentEntity i, Map<Long, ReadingEntity> latestByOutput) {
        for (OutputEntity o : i.getOutputs()) {
            if (Boolean.TRUE.equals(o.getActive()) && isPiezometricElevation(o.getName())) {
                return toElevationDTO(o, latestByOutput.get(o.getId()));
            }
        }
        for (ConstantEntity c : i.getConstants()) {
            if (isPiezometricElevation(c.getName())) return toElevationDTO(c);
        }
        return null;
    }

    private ElevationVariableDTO toElevationDTO(ConstantEntity c) {
        ElevationVariableDTO dto = new ElevationVariableDTO();
        dto.setId(c.getId());
        dto.setName(c.getName());
        dto.setAcronym(c.getAcronym());
        dto.setPrecision(c.getPrecision());
        dto.setType(VariableType.CONSTANT);
        dto.setConstantValue(c.getValue());
        if (c.getMeasurementUnit() != null) {
            dto.setMeasurementUnit(new InstrumentMeasurementUnitDTO(
                    c.getMeasurementUnit().getName(), c.getMeasurementUnit().getAcronym()));
        }
        return dto;
    }

    private ElevationVariableDTO toElevationDTO(OutputEntity o, ReadingEntity lastReading) {
        ElevationVariableDTO dto = new ElevationVariableDTO();
        dto.setId(o.getId());
        dto.setName(o.getName());
        dto.setAcronym(o.getAcronym());
        dto.setPrecision(o.getPrecision());
        dto.setType(VariableType.OUTPUT);
        if (o.getMeasurementUnit() != null) {
            dto.setMeasurementUnit(new InstrumentMeasurementUnitDTO(
                    o.getMeasurementUnit().getName(), o.getMeasurementUnit().getAcronym()));
        }
        if (lastReading != null) {
            dto.setLastReadingDate(lastReading.getDate());
            dto.setLastReadingHour(lastReading.getHour());
            dto.setLastReadingValue(lastReading.getCalculatedValue());
            dto.setLastReadingLimitStatus(lastReading.getLimitStatus());
        }
        return dto;
    }

    private SectionRenderingConfigResponseDTO defaultResponse(Long sectionId) {
        SectionRenderingConfigResponseDTO dto = new SectionRenderingConfigResponseDTO();
        dto.setSectionId(sectionId);
        return dto;
    }

    private void checkViewPermission() {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (!userLogged.getInstrumentationPermission().getViewSections()) {
                throw new UnauthorizedException("Usuário não autorizado a visualizar seções!");
            }
        }
    }

    private void checkEditPermission() {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (!userLogged.getInstrumentationPermission().getEditSections()) {
                throw new UnauthorizedException("Usuário não autorizado a editar seções!");
            }
        }
    }
}
