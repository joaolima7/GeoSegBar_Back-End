package com.geosegbar.infra.checklist_submission.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhotoSubmissionDTO {
    
    private String base64Image;
    
    private String fileName;
    
    private String contentType;
}
