package com.jobportal.searchservice.service;

import com.jobportal.searchservice.entity.SearchJob;
import com.jobportal.searchservice.event.JobDeletedEvent;
import com.jobportal.searchservice.event.JobUpsertedEvent;
import com.jobportal.searchservice.repository.SearchJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SearchJobEventListenerTest {

    @Mock
    private SearchJobRepository searchJobRepository;

    @InjectMocks
    private SearchJobEventListener listener;

    @Test
    void onJobUpsertedSavesMappedSearchJob() {
        JobUpsertedEvent event = JobUpsertedEvent.builder()
                .jobId(10L)
                .title("Java Developer")
                .company("Acme")
                .location("Hyderabad")
                .salaryRange("12 LPA")
                .jobType("FULL_TIME")
                .status("OPEN")
                .recruiterId(99L)
                .experienceMin(1)
                .experienceMax(3)
                .deadline(LocalDate.now().plusDays(30))
                .createdAt(LocalDateTime.now())
                .build();

        listener.onJobUpserted(event);

        ArgumentCaptor<SearchJob> captor = ArgumentCaptor.forClass(SearchJob.class);
        verify(searchJobRepository).save(captor.capture());

        SearchJob saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(10L);
        assertThat(saved.getTitle()).isEqualTo("Java Developer");
        assertThat(saved.getCompany()).isEqualTo("Acme");
        assertThat(saved.getStatus()).isEqualTo("OPEN");
    }

    @Test
    void onJobDeletedDeletesById() {
        listener.onJobDeleted(JobDeletedEvent.builder().jobId(55L).build());

        verify(searchJobRepository).deleteById(55L);
    }
}