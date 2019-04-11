package com.metadata.exercise.repositories;

import com.metadata.exercise.entities.File;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface FileRepository extends CrudRepository<File, Long> {

    @Override
    List<File> findAll();

}
