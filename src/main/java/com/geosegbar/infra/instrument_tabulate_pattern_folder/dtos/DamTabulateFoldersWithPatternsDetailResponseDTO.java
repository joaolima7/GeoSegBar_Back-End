package com.geosegbar.infra.instrument_tabulate_pattern_folder.dtos;

import java.util.List;

import com.geosegbar.infra.instrument_tabulate_pattern.dtos.TabulatePatternResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DamTabulateFoldersWithPatternsDetailResponseDTO {

    private Long damId;
    private String damName;
    private String damCity;
    private String damState;
    private List<TabulateFolderWithPatternsDetailResponseDTO> folders;
    private List<TabulatePatternResponseDTO> patternsWithoutFolder;
}