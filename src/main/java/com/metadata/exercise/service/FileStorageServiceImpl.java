package com.metadata.exercise.service;

import com.metadata.exercise.entities.File;
import com.metadata.exercise.entities.Version;
import com.metadata.exercise.exception.FileStorageException;
import com.metadata.exercise.exception.MyFileNotFoundException;
import com.metadata.exercise.property.FileStorageProperties;
import com.metadata.exercise.repositories.FileRepository;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class FileStorageServiceImpl implements FileStorageService {

    private final Path fileStorageLocation;

    @Autowired FileRepository fileRepository;


    @Autowired
    public FileStorageServiceImpl(FileStorageProperties fileStorageProperties) {
        this.fileStorageLocation =
              Paths.get(fileStorageProperties.getUploadDir()).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.",
                ex);
        }
    }

    @Transactional
    public File storeFile(MultipartFile file) {
        // Normalize file name
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            // Check if the file's name contains invalid characters
            if (fileName.contains("..")) {
                throw new FileStorageException(
                    "Sorry! Filename contains invalid path sequence " + fileName);
            }

            // Copy file to the target location (Replacing existing file with the same name but uuid to
            // avoid collisions)
            Path targetLocation =
                    this.fileStorageLocation.resolve(UUID.randomUUID().toString() + "-" + fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);


            Version version = new Version();
            version.setActive(true);
            version.setUrl(targetLocation.toString());
            HashSet versions = new HashSet<Version>();
            versions.add(version);
            File fileEntity = new File();
            fileEntity.setVersions(versions);
            fileRepository.save(fileEntity);

            return fileEntity;

        } catch (Exception e) {
            // no matter what when wrong we should fail this
            throw new FileStorageException("Could not store file " + fileName + ". Please try again!", e);
        }
    }

    @Transactional
    public File storeFileVersion(long id, MultipartFile file) {
        // Normalize file name
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            // Check if the file's name contains invalid characters
            if (fileName.contains("..")) {
                throw new FileStorageException(
                    "Sorry! filename contains invalid path sequence " + fileName);
            }
            Optional<File> fileOptional = fileRepository.findById(id);

            if (!fileOptional.isPresent()) {
                throw new MyFileNotFoundException("can't file with id: " + id);
            }

            File fileEntity = fileOptional.get();

            // Copy file to the target location (Replacing existing file with the same name and id as
            // prefix to avoid collisions)
            Path targetLocation =
                    this.fileStorageLocation.resolve(UUID.randomUUID().toString() + "-" + fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // deactivate current version
            Version curretVersion = findCurrentVersion(fileEntity);
            curretVersion.setActive(false);

            // new version
            Version newVersion = new Version();
            newVersion.setActive(true);
            newVersion.setUrl(targetLocation.toString());
            fileEntity.getVersions().add(newVersion);
            fileRepository.save(fileEntity);

            return fileEntity;

        } catch (Exception e) {
            // no matter what when wrong we should fail this
            throw new FileStorageException("Could not store file " + fileName + ". Please try again!", e);
        }

    }

    public Resource loadFileAsResourceCurrentVersion(Long id) {
        return loadFileAsResource(id, null);
    }

    public Resource loadFileAsResource(Long id, Long versionId) {
        try {
            Optional<File> fileOptional = fileRepository.findById(id);

            if (!fileOptional.isPresent()) {
                throw new MyFileNotFoundException("can't find file with id: " + id);
            }

            File fileEntity = fileOptional.get();

            Version version;
            if (versionId == null) {
                version = findCurrentVersion(fileEntity);
            } else {
                version = findVersion(fileEntity, versionId);
            }

            Path filePath = this.fileStorageLocation.resolve(version.getUrl()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new MyFileNotFoundException("File not found id " + id);
            }

        } catch (MalformedURLException ex) {
            throw new MyFileNotFoundException("File not found ", ex);
        }
    }

    private void deleteFile(Version version) {
        try {
            Path filePath = this.fileStorageLocation.resolve(version.getUrl()).normalize();
            Files.delete(filePath);
        } catch (Exception e) {
            // if it fails, we just log I'm assume no reason to stop and swallow it
            log.error("Error deleting file :", e);
        }
    }

    public void deleteFile(long id) {

        Optional<File> fileOptional = fileRepository.findById(id);

        if (!fileOptional.isPresent()) {
            throw new MyFileNotFoundException("can't file with id: " + id);
        }

        File fileEntity = fileOptional.get();
        fileEntity.getVersions().stream().forEach(version -> deleteFile(version));
        fileRepository.deleteById(id);
    }

    public List<File> getFiles() {
        return fileRepository.findAll();
    }


    private static Version findCurrentVersion(File fileEntity) {
        return fileEntity.getVersions().stream()
            .filter(version1 -> version1.isActive())
            .findAny()
            .orElseThrow(
                () -> new MyFileNotFoundException("We can't find that file and active version"));
    }

    private static Version findVersion(File fileEntity, Long version) {
        return fileEntity.getVersions().stream()
            .filter(version1 -> version1.getId().equals(version))
            .findAny()
            .orElseThrow(() -> new MyFileNotFoundException("We can't find that file and version"));
    }

}
