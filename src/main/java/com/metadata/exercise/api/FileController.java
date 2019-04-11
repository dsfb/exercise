package com.metadata.exercise.api;

import com.metadata.exercise.dto.UploadFileResponse;
import com.metadata.exercise.entities.File;
import com.metadata.exercise.entities.Version;
import com.metadata.exercise.exception.MyFileNotFoundException;
import com.metadata.exercise.service.FileStorageService;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;


@RestController
@Slf4j
public class FileController {


    @Autowired private FileStorageService fileStorageService;

    @PostMapping(path = "/file")
    public UploadFileResponse uploadFile(@RequestParam("file") MultipartFile file) {
        File fileEntity = fileStorageService.storeFile(file);

        Version version = fileEntity.getVersions().iterator().next();

        String fileDownloadUri =
                ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/downloadFile/").path(version.getUrl()).toUriString();

        return new UploadFileResponse(fileEntity.getId(), version.getUrl(), fileDownloadUri,
              file.getContentType(), file.getSize());
    }

    @PostMapping(path = "/file/{id}")
    public UploadFileResponse uploadFileResponseVersion(@PathVariable long id,
            @RequestParam("file") MultipartFile file) {

        File fileEntity = fileStorageService.storeFileVersion(id, file);

        Version version =
                fileEntity.getVersions().stream().filter(version1 -> version1.isActive())
                .findAny().orElseThrow(() -> new MyFileNotFoundException("We can't find that file and version"));

        String fileDownloadUri =
                ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/downloadFile/").path(version.getUrl()).toUriString();

        return new UploadFileResponse(fileEntity.getId(), version.getUrl(), fileDownloadUri, file.getContentType(),
              file.getSize());
    }

    @GetMapping("/file/{id}/")
    public ResponseEntity<Resource> downloadFileVersion(
            @PathVariable long id, HttpServletRequest request) {

        return downloadFile(id, null, request);
    }

    @GetMapping("/file/{id}/version/{version}/")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long id, @PathVariable Long version, HttpServletRequest request) {

        // Load file as Resource
        Resource resource;
        if (version == null) {
            resource = fileStorageService.loadFileAsResourceCurrentVersion(id);
        } else {
            resource = fileStorageService.loadFileAsResource(id, version);
        }

        // Try to determine file's content type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            log.info("Could not determine file type.");
        }

        // Fallback to the default content type if type could not be determined
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + resource.getFilename() + "\"")
            .body(resource);
    }

    @GetMapping("/file")
    public ResponseEntity<List<File>> listFiles() {
        return ResponseEntity.ok().body(fileStorageService.getFiles());
    }

    @DeleteMapping("/file/{id}/")
    public ResponseEntity delete(@PathVariable Long id) {
        fileStorageService.deleteFile(id);
        return ResponseEntity.ok().body("");
    }

}
