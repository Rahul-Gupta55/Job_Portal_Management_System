package com.jobportal.applicationservice.repository;

import com.jobportal.applicationservice.entity.Application;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByJobIdAndCandidateId(Long jobId, Long candidateId);

    @EntityGraph(attributePaths = "statusHistory")
    Optional<Application> findById(Long id);

    List<Application> findByJobIdOrderByAppliedAtDesc(Long jobId);

    List<Application> findByCandidateIdOrderByAppliedAtDesc(Long candidateId);
}
