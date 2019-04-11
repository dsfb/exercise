package com.metadata.exercise.repositories;

import com.metadata.exercise.entities.Version;
import org.springframework.data.repository.CrudRepository;

public interface VersionRepository extends CrudRepository<Version, Long> {}
