package com.geosegbar.infra.checklist_response.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhotoInfoDTO {
    private Long id;
    private String imagePath;
}
