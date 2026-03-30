package com.jobportal.searchservice.service;

import com.jobportal.searchservice.config.RabbitMQConfig;
import com.jobportal.searchservice.entity.SearchJob;
import com.jobportal.searchservice.event.JobDeletedEvent;
import com.jobportal.searchservice.event.JobUpsertedEvent;
import com.jobportal.searchservice.repository.SearchJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SearchJobEventListener {

    private final SearchJobRepository searchJobRepository;

    @Transactional
    @RabbitListener(queues = RabbitMQConfig.JOB_UPSERT_QUEUE)
    public void onJobUpserted(JobUpsertedEvent event) {
        SearchJob searchJob = SearchJob.builder()
                .id(event.getJobId())
                .title(event.getTitle())
                .company(event.getCompany())
                .location(event.getLocation())
                .salaryRange(event.getSalaryRange())
                .jobType(event.getJobType())
                .status(event.getStatus())
                .recruiterId(event.getRecruiterId())
                .experienceMin(event.getExperienceMin())
                .experienceMax(event.getExperienceMax())
                .deadline(event.getDeadline())
                .createdAt(event.getCreatedAt())
                .build();
        searchJobRepository.save(searchJob);
    }

    @Transactional
    @RabbitListener(queues = RabbitMQConfig.JOB_DELETE_QUEUE)
    public void onJobDeleted(JobDeletedEvent event) {
        searchJobRepository.deleteById(event.getJobId());
    }
}
