package com.geosegbar.infra.instrument_graph_pattern_folder.dtos;

import java.util.List;

import com.geosegbar.infra.instrument_graph_pattern.dtos.GraphPatternDetailResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DamFoldersWithPatternsDetailResponseDTO {

    private Long damId;
    private String damName;
    private String damCity;
    private String damState;
    private List<FolderWithPatternsDetailResponseDTO> folders;
    private List<GraphPatternDetailResponseDTO> patternsWithoutFolder;
}
