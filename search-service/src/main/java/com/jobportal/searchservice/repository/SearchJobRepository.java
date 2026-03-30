package com.jobportal.searchservice.repository;

import com.jobportal.searchservice.entity.SearchJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SearchJobRepository extends JpaRepository<SearchJob, Long>, JpaSpecificationExecutor<SearchJob> {
}
