package com.jobportal.jobservice.repository;

import com.jobportal.jobservice.entity.Job;
import com.jobportal.jobservice.entity.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {

    @EntityGraph(attributePaths = "skills")
    Page<Job> findByStatus(JobStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "skills")
    List<Job> findByRecruiterIdOrderByCreatedAtDesc(Long recruiterId);

    @Override
    @EntityGraph(attributePaths = "skills")
    Optional<Job> findById(Long id);
}
