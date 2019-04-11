package com.metadata.exercise.service;

import com.metadata.exercise.entities.File;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;


public interface FileStorageService {

    File storeFile(MultipartFile file);

    File storeFileVersion(long id, MultipartFile file);

    Resource loadFileAsResource(Long id, Long versionId);

    Resource loadFileAsResourceCurrentVersion(Long id);

    void deleteFile(long id);

    List<File> getFiles();

}
