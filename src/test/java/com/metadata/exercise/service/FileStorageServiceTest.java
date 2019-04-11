package com.metadata.exercise.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.metadata.exercise.entities.File;
import com.metadata.exercise.entities.Version;
import com.metadata.exercise.exception.FileStorageException;
import com.metadata.exercise.exception.MyFileNotFoundException;
import com.metadata.exercise.property.FileStorageProperties;
import com.metadata.exercise.repositories.FileRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.multipart.MultipartFile;

@RunWith(SpringRunner.class)
@SpringBootTest
public class FileStorageServiceTest {

    private static final String uploads = "./uploads";
    private static final String tesfile_name = "sign.jpg";
    private static final String no_file_testfile_name = "sign_1.jpg";
    private static final Long ID = 1L;


    @Autowired
    FileStorageService fileStorageService;

    @MockBean
    @Autowired FileRepository fileRepository;

    @TestConfiguration
    static class EmployeeServiceImplTestContextConfiguration {

        @Bean
        public FileStorageService fileStorageService() {
            FileStorageProperties fileStorageProperties = new FileStorageProperties();
            fileStorageProperties.setUploadDir(uploads);
            return new FileStorageServiceImpl(fileStorageProperties);
        }
    }

    @Test
    public void testShouldStoreFileVersion() throws IOException {

        MultipartFile file = getMultipartFile();
        File fileEntity = getFilEntity();

        when(fileRepository.findById(ID)).thenReturn(Optional.of(fileEntity));
        //act
        File fileEntityResult = fileStorageService.storeFileVersion(ID, file);

        //Assert
        assertEquals(2, fileEntityResult.getVersions().size());

    }

    @Test
    public void shouldDeleteFile() {

        File fileEntity = getFilEntity();

        //we don't want to delete our test file :-)
        fileEntity.getVersions().iterator().next().setUrl(no_file_testfile_name);

        //Arrange
        when(fileRepository.findById(ID)).thenReturn(Optional.of(fileEntity));

        //Act
        fileStorageService.deleteFile(ID);
    }

    @Test(expected = MyFileNotFoundException.class)
    public void shouldThrowExceptionDeletingFile() {

        File fileEntity = getFilEntity();

        //Arrange
        when(fileRepository.findById(ID)).thenReturn(Optional.empty());

        //Act
        fileStorageService.deleteFile(ID);
    }

    @Test
    public void testShouldStoreFile() throws IOException {
        //arrange
        MultipartFile file = getMultipartFile();
        //act
        File fileEntity = fileStorageService.storeFile(file);

        //assert
        assertNotNull(fileEntity);
        assertEquals(fileEntity.getVersions().size(),1);
        Version version = fileEntity.getVersions().iterator().next();
        assertTrue(version.getUrl().contains(tesfile_name));
    }

    @Test(expected = FileStorageException.class)
    public void testShouldFailInvalidChars() throws IOException {
        //arrange
        MultipartFile file = getMultipartFileBadName();
        //act
        File fileEntity = fileStorageService.storeFile(file);

        //assert
    }

    @Test
    public void shouldLoadFileAsResource() {
        File fileEntity = getFilEntity();

        //Arrange
        when(fileRepository.findById(ID)).thenReturn(Optional.of(fileEntity));

        Resource resource = fileStorageService.loadFileAsResourceCurrentVersion(ID);

        assertEquals(resource.getFilename(), tesfile_name);
    }

    @Test
    public void shouldLoadFileVersionAsResource() {
        File fileEntity = getFilEntity();

        //Arrange
        when(fileRepository.findById(ID)).thenReturn(Optional.of(fileEntity));

        Resource resource = fileStorageService.loadFileAsResource(ID, ID);

        assertEquals(resource.getFilename(), tesfile_name);
    }


    @Test(expected = MyFileNotFoundException.class)
    public void shouldThrowExceptionLoadingFileAsResource() {
        File fileEntity = getFilEntity();

        //Arrange
        when(fileRepository.findById(ID)).thenReturn(Optional.empty());

        //Act
        Resource resource = fileStorageService.loadFileAsResourceCurrentVersion(ID);

    }


    private MultipartFile getMultipartFile() throws IOException {

        Path path = Paths.get("src/test/resources/testfiles/" + tesfile_name);
        String name = tesfile_name;
        String originalFileName = tesfile_name;
        String contentType = "image/jpg";
        byte[] content = null;
        content = Files.readAllBytes(path);

        MultipartFile result = new MockMultipartFile(name,
                originalFileName, contentType, content);

        return result;
    }

    private MultipartFile getMultipartFileBadName() throws IOException {

        Path path = Paths.get("src/test/resources/testfiles/" + tesfile_name);
        String name = tesfile_name;
        String originalFileName = tesfile_name + "..";
        String contentType = "image/jpg";
        byte[] content = null;
        content = Files.readAllBytes(path);

        MultipartFile result = new MockMultipartFile(name,
                originalFileName, contentType, content);

        return result;
    }

    private File getFilEntity() {
        File file = new File();
        file.setId(ID);
        Version version = new Version();
        version.setActive(true);
        version.setId(ID);
        version.setUrl(tesfile_name);
        file.getVersions().add(version);
        return file;
    }

}
