package com.geosegbar.infra.dam.dtos;

import java.util.List;

import com.geosegbar.infra.map_kml.dtos.MapKmlFolderResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DamMapDataDTO {

    private List<MapInstrumentDTO> instruments;
    private List<MapSectionDTO> sections;
    private List<MapAnomalyDTO> anomalies;
    private List<MapKmlFolderResponseDTO> kmlFolders;
}
