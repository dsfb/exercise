package com.metadata.exercise.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadFileResponse {

    private long id;
    private String fileName;
    private String fileDownloadUri;
    private String fileType;
    private long size;

}
