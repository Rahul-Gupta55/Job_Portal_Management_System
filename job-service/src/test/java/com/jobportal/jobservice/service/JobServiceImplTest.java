package com.jobportal.jobservice.service;

import com.jobportal.commonsecurity.security.JwtUserPrincipal;
import com.jobportal.jobservice.dto.CreateJobRequest;
import com.jobportal.jobservice.dto.JobResponse;
import com.jobportal.jobservice.dto.UpdateJobRequest;
import com.jobportal.jobservice.entity.Job;
import com.jobportal.jobservice.entity.JobStatus;
import com.jobportal.jobservice.entity.JobType;
import com.jobportal.jobservice.exception.BadRequestException;
import com.jobportal.jobservice.exception.ResourceNotFoundException;
import com.jobportal.jobservice.exception.UnauthorizedActionException;
import com.jobportal.jobservice.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    @Mock
    private JobRepository jobRepository;
    @Mock
    private JobEventPublisher jobEventPublisher;

    @InjectMocks
    private JobServiceImpl jobService;

    @Test
    void createJobAsRecruiterPublishesCreatedAndUpsertEvents() {
        CreateJobRequest request = CreateJobRequest.builder()
                .title("Java Developer")
                .company("Acme")
                .location("Hyderabad")
                .salaryRange("10 LPA")
                .description("This description is long enough to satisfy validation requirements for the service.")
                .requirements("Spring Boot")
                .jobType(JobType.FULL_TIME)
                .skills(List.of("Java", "Spring Boot"))
                .experienceMin(1)
                .experienceMax(3)
                .deadline(LocalDate.now().plusDays(30))
                .build();

        JwtUserPrincipal principal = new JwtUserPrincipal(11L, "recruiter@example.com", "RECRUITER");

        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
            Job job = invocation.getArgument(0);
            job.setId(100L);
            return job;
        });

        JobResponse response = jobService.createJob(request, principal);

        assertThat(response.getRecruiterId()).isEqualTo(11L);
        assertThat(response.getStatus()).isEqualTo(JobStatus.OPEN);
        assertThat(response.getSkills()).containsExactly("Java", "Spring Boot");
        verify(jobEventPublisher).publishJobCreated(any());
        verify(jobEventPublisher).publishJobUpserted(any());
    }

    @Test
    void createJobRejectsJobSeeker() {
        CreateJobRequest request = CreateJobRequest.builder()
                .title("Java Developer")
                .company("Acme")
                .location("Hyderabad")
                .description("This description is long enough to satisfy validation requirements for the service.")
                .jobType(JobType.FULL_TIME)
                .build();

        JwtUserPrincipal principal = new JwtUserPrincipal(99L, "seeker@example.com", "JOB_SEEKER");

        assertThatThrownBy(() -> jobService.createJob(request, principal))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void createJobRejectsInvalidExperienceRange() {
        CreateJobRequest request = CreateJobRequest.builder()
                .title("Java Developer")
                .company("Acme")
                .location("Hyderabad")
                .description("This description is long enough to satisfy validation requirements for the service.")
                .jobType(JobType.FULL_TIME)
                .experienceMin(5)
                .experienceMax(2)
                .build();

        JwtUserPrincipal principal = new JwtUserPrincipal(11L, "recruiter@example.com", "RECRUITER");

        assertThatThrownBy(() -> jobService.createJob(request, principal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Minimum experience cannot be greater");
    }

    @Test
    void getJobByIdHidesClosedJobFromPublicUsers() {
        Job job = Job.builder()
                .id(5L)
                .recruiterId(11L)
                .status(JobStatus.CLOSED)
                .build();

        when(jobRepository.findById(5L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.getJobById(5L, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getJobByIdAllowsOwnerToSeeClosedJob() {
        Job job = Job.builder()
                .id(5L)
                .title("Backend Engineer")
                .company("Acme")
                .location("Bangalore")
                .description("desc")
                .requirements("req")
                .jobType(JobType.FULL_TIME)
                .status(JobStatus.CLOSED)
                .recruiterId(11L)
                .build();

        JwtUserPrincipal principal = new JwtUserPrincipal(11L, "recruiter@example.com", "RECRUITER");
        when(jobRepository.findById(5L)).thenReturn(Optional.of(job));

        JobResponse response = jobService.getJobById(5L, principal);

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getStatus()).isEqualTo(JobStatus.CLOSED);
    }

    @Test
    void updateJobRejectsClosedToDraftTransition() {
        Job job = Job.builder().id(5L).recruiterId(11L).status(JobStatus.CLOSED).build();
        UpdateJobRequest request = UpdateJobRequest.builder()
                .title("Updated")
                .company("Acme")
                .location("Hyd")
                .description("This description is long enough to satisfy validation requirements for the service.")
                .jobType(JobType.FULL_TIME)
                .status(JobStatus.DRAFT)
                .build();

        JwtUserPrincipal principal = new JwtUserPrincipal(11L, "recruiter@example.com", "RECRUITER");
        when(jobRepository.findById(5L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.updateJob(5L, request, principal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Closed job cannot be moved to draft");
    }

    @Test
    void closeJobPublishesClosedAndUpsertEvents() {
        Job job = Job.builder()
                .id(9L)
                .title("Backend Engineer")
                .company("Acme")
                .location("Bangalore")
                .description("desc")
                .requirements("req")
                .jobType(JobType.FULL_TIME)
                .status(JobStatus.OPEN)
                .recruiterId(11L)
                .build();

        JwtUserPrincipal principal = new JwtUserPrincipal(11L, "recruiter@example.com", "RECRUITER");

        when(jobRepository.findById(9L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobResponse response = jobService.closeJob(9L, principal);

        assertThat(response.getStatus()).isEqualTo(JobStatus.CLOSED);
        verify(jobEventPublisher).publishJobClosed(any());
        verify(jobEventPublisher).publishJobUpserted(any());
    }

    @Test
    void deleteDraftJobDeletesOnlyDraft() {
        Job job = Job.builder()
                .id(6L)
                .recruiterId(11L)
                .status(JobStatus.DRAFT)
                .build();

        JwtUserPrincipal principal = new JwtUserPrincipal(11L, "recruiter@example.com", "RECRUITER");
        when(jobRepository.findById(6L)).thenReturn(Optional.of(job));

        jobService.deleteDraftJob(6L, principal);

        verify(jobRepository).delete(job);
        verify(jobEventPublisher).publishJobDeleted(any());
    }

    @Test
    void deleteDraftJobRejectsNonDraftJob() {
        Job job = Job.builder()
                .id(6L)
                .recruiterId(11L)
                .status(JobStatus.OPEN)
                .build();

        JwtUserPrincipal principal = new JwtUserPrincipal(11L, "recruiter@example.com", "RECRUITER");
        when(jobRepository.findById(6L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.deleteDraftJob(6L, principal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only DRAFT jobs can be deleted");
    }

    @Test
    void getAllOpenJobsReturnsMappedPage() {
        Job openJob = Job.builder()
                .id(1L)
                .title("Backend Engineer")
                .company("Acme")
                .location("Bangalore")
                .description("desc")
                .requirements("req")
                .jobType(JobType.FULL_TIME)
                .status(JobStatus.OPEN)
                .recruiterId(7L)
                .build();

        when(jobRepository.findByStatus(eq(JobStatus.OPEN), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(openJob), PageRequest.of(0, 10), 1));

        var page = jobService.getAllOpenJobs(PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getStatus()).isEqualTo(JobStatus.OPEN);
    }


    @Test
    void updateJobPersistsChangesAndPublishesUpsertEvent() {
        Job job = Job.builder()
                .id(15L)
                .title("Old Title")
                .company("Acme")
                .location("Hyderabad")
                .description("Old description")
                .requirements("Old requirements")
                .jobType(JobType.FULL_TIME)
                .status(JobStatus.OPEN)
                .recruiterId(11L)
                .build();
        UpdateJobRequest request = UpdateJobRequest.builder()
                .title("Updated Title")
                .company("Acme")
                .location("Bangalore")
                .salaryRange("20 LPA")
                .description("This updated description is long enough to satisfy validation requirements for the service.")
                .requirements("Spring Boot, Kafka")
                .jobType(JobType.CONTRACT)
                .status(JobStatus.OPEN)
                .skills(List.of("Java", "Kafka"))
                .experienceMin(3)
                .experienceMax(6)
                .deadline(LocalDate.now().plusDays(45))
                .build();
        JwtUserPrincipal principal = new JwtUserPrincipal(11L, "recruiter@example.com", "RECRUITER");

        when(jobRepository.findById(15L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobResponse response = jobService.updateJob(15L, request, principal);

        assertThat(response)
                .extracting(JobResponse::getTitle, JobResponse::getLocation, JobResponse::getJobType)
                .containsExactly("Updated Title", "Bangalore", JobType.CONTRACT);
        assertThat(response.getSkills()).containsExactly("Java", "Kafka");
        verify(jobEventPublisher).publishJobUpserted(any());
    }

    @Test
    void reopenJobMarksClosedJobAsOpen() {
        Job job = Job.builder()
                .id(19L)
                .title("Backend Engineer")
                .company("Acme")
                .location("Bangalore")
                .description("desc")
                .requirements("req")
                .jobType(JobType.FULL_TIME)
                .status(JobStatus.CLOSED)
                .recruiterId(11L)
                .build();
        JwtUserPrincipal principal = new JwtUserPrincipal(11L, "recruiter@example.com", "RECRUITER");

        when(jobRepository.findById(19L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobResponse response = jobService.reopenJob(19L, principal);

        assertThat(response.getStatus()).isEqualTo(JobStatus.OPEN);
        verify(jobEventPublisher).publishJobUpserted(any());
    }

    @Test
    void getJobsByRecruiterAllowsAdmin() {
        Job job = Job.builder()
                .id(25L)
                .title("Backend Engineer")
                .company("Acme")
                .location("Bangalore")
                .description("desc")
                .requirements("req")
                .jobType(JobType.FULL_TIME)
                .status(JobStatus.OPEN)
                .recruiterId(11L)
                .build();
        JwtUserPrincipal admin = new JwtUserPrincipal(1L, "admin@example.com", "ADMIN");
        when(jobRepository.findByRecruiterIdOrderByCreatedAtDesc(11L)).thenReturn(List.of(job));

        List<JobResponse> jobs = jobService.getJobsByRecruiter(11L, admin);

        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).getId()).isEqualTo(25L);
    }

    @Test
    void getSkillsReturnsMappedSkillNames() {
        Job job = Job.builder()
                .id(30L)
                .status(JobStatus.OPEN)
                .recruiterId(11L)
                .build();
        job.addSkill(com.jobportal.jobservice.entity.JobSkill.builder().skill("Java").build());
        job.addSkill(com.jobportal.jobservice.entity.JobSkill.builder().skill("Spring Boot").build());
        when(jobRepository.findById(30L)).thenReturn(Optional.of(job));

        List<String> skills = jobService.getSkills(30L, null);

        assertThat(skills).containsExactly("Java", "Spring Boot");
    }


    @Test
    void createJobWithNullSkillsDefaultsToEmptySkillList() {
        CreateJobRequest request = CreateJobRequest.builder()
                .title("Java Developer")
                .company("Acme")
                .location("Hyderabad")
                .description("This description is long enough to satisfy validation requirements for the service.")
                .jobType(JobType.FULL_TIME)
                .skills(null)
                .build();
        JwtUserPrincipal principal = new JwtUserPrincipal(11L, "recruiter@example.com", "RECRUITER");
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
            Job job = invocation.getArgument(0);
            job.setId(101L);
            return job;
        });

        JobResponse response = jobService.createJob(request, principal);

        assertThat(response.getSkills()).isEmpty();
    }

    @Test
    void getJobByIdAllowsAdminToSeeClosedJob() {
        Job job = Job.builder()
                .id(8L)
                .status(JobStatus.CLOSED)
                .recruiterId(11L)
                .build();
        JwtUserPrincipal admin = new JwtUserPrincipal(1L, "admin@example.com", "ADMIN");
        when(jobRepository.findById(8L)).thenReturn(Optional.of(job));

        JobResponse response = jobService.getJobById(8L, admin);

        assertThat(response.getId()).isEqualTo(8L);
        assertThat(response.getStatus()).isEqualTo(JobStatus.CLOSED);
    }

    @Test
    void getSkillsAllowsAdminForClosedJob() {
        Job job = Job.builder()
                .id(31L)
                .status(JobStatus.CLOSED)
                .recruiterId(11L)
                .build();
        job.addSkill(com.jobportal.jobservice.entity.JobSkill.builder().skill("Kafka").build());
        JwtUserPrincipal admin = new JwtUserPrincipal(1L, "admin@example.com", "ADMIN");
        when(jobRepository.findById(31L)).thenReturn(Optional.of(job));

        List<String> skills = jobService.getSkills(31L, admin);

        assertThat(skills).containsExactly("Kafka");
    }
}