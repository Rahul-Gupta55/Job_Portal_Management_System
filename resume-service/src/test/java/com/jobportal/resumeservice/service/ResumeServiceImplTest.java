package com.jobportal.resumeservice.service;

import com.jobportal.commonsecurity.security.JwtUserPrincipal;
import com.jobportal.resumeservice.dto.ResumeResponse;
import com.jobportal.resumeservice.entity.Resume;
import com.jobportal.resumeservice.exception.ResourceNotFoundException;
import com.jobportal.resumeservice.exception.UnauthorizedActionException;
import com.jobportal.resumeservice.repository.ResumeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeServiceImplTest {

    @Mock
    private ResumeRepository resumeRepository;

    @InjectMocks
    private ResumeServiceImpl resumeService;

    @TempDir
    Path tempDir;

    @Test
    void createStoresValidPdfAndMarksItPrimary() throws Exception {
        ReflectionTestUtils.setField(resumeService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(resumeService, "maxFileSizeBytes", 5_242_880L);

        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "seeker@example.com", "JOB_SEEKER");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                validPdfBytes()
        );

        Resume existingPrimary = Resume.builder()
                .id(99L)
                .userId(5L)
                .primaryResume(true)
                .fileName("old.pdf")
                .fileUrl("/api/resumes/99/file")
                .storagePath("old.pdf")
                .build();

        when(resumeRepository.findByUserIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(existingPrimary));
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> {
            Resume resume = invocation.getArgument(0);
            if (resume.getId() == null) {
                resume.setId(1L);
            }
            return resume;
        });

        ResumeResponse response = resumeService.create(file, true, principal);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFileUrl()).isEqualTo("/api/resumes/1/file");
        assertThat(response.isPrimary()).isTrue();
        assertThat(existingPrimary.isPrimaryResume()).isFalse();

        try (var stream = Files.list(tempDir)) {
            assertThat(stream.count()).isEqualTo(1);
        }
    }

    @Test
    void createRejectsNonPdfExtension() {
        ReflectionTestUtils.setField(resumeService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(resumeService, "maxFileSizeBytes", 5_242_880L);

        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "seeker@example.com", "JOB_SEEKER");
        MockMultipartFile file = new MockMultipartFile("file", "resume.txt", "text/plain", "hello".getBytes());

        assertThatThrownBy(() -> resumeService.create(file, false, principal))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only PDF files are allowed");
    }

    @Test
    void createRejectsInvalidPdfSignature() {
        ReflectionTestUtils.setField(resumeService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(resumeService, "maxFileSizeBytes", 5_242_880L);

        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "seeker@example.com", "JOB_SEEKER");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "not-a-real-pdf".getBytes(StandardCharsets.US_ASCII)
        );

        assertThatThrownBy(() -> resumeService.create(file, false, principal))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Uploaded file is not a valid PDF");
    }

    @Test
    void getByUserIdRejectsOtherUsers() {
        JwtUserPrincipal principal = new JwtUserPrincipal(99L, "other@example.com", "JOB_SEEKER");

        assertThatThrownBy(() -> resumeService.getByUserId(5L, principal))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void setPrimaryClearsExistingPrimaryResume() {
        ReflectionTestUtils.setField(resumeService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(resumeService, "maxFileSizeBytes", 5_242_880L);

        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "seeker@example.com", "JOB_SEEKER");

        Resume targetResume = Resume.builder()
                .id(3L)
                .userId(5L)
                .fileName("new.pdf")
                .fileUrl("/api/resumes/3/file")
                .storagePath("new.pdf")
                .primaryResume(false)
                .build();

        Resume oldPrimary = Resume.builder()
                .id(2L)
                .userId(5L)
                .fileName("old.pdf")
                .fileUrl("/api/resumes/2/file")
                .storagePath("old.pdf")
                .primaryResume(true)
                .build();

        when(resumeRepository.findById(3L)).thenReturn(Optional.of(targetResume));
        when(resumeRepository.findByUserIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(oldPrimary, targetResume));
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResumeResponse response = resumeService.setPrimary(3L, principal);

        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.isPrimary()).isTrue();
        assertThat(oldPrimary.isPrimaryResume()).isFalse();
        assertThat(targetResume.isPrimaryResume()).isTrue();
    }

    @Test
    void downloadFileThrowsWhenStoredFileIsMissing() {
        ReflectionTestUtils.setField(resumeService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(resumeService, "maxFileSizeBytes", 5_242_880L);

        Resume resume = Resume.builder()
                .id(8L)
                .userId(5L)
                .storagePath("missing.pdf")
                .fileName("resume.pdf")
                .fileUrl("/api/resumes/8/file")
                .build();

        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "seeker@example.com", "JOB_SEEKER");
        when(resumeRepository.findById(8L)).thenReturn(Optional.of(resume));

        assertThatThrownBy(() -> resumeService.downloadFile(8L, principal))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRemovesStoredFileAndDatabaseRow() throws Exception {
        ReflectionTestUtils.setField(resumeService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(resumeService, "maxFileSizeBytes", 5_242_880L);

        Path stored = tempDir.resolve("stored.pdf");
        Files.write(stored, validPdfBytes());

        Resume resume = Resume.builder()
                .id(3L)
                .userId(5L)
                .storagePath("stored.pdf")
                .fileName("resume.pdf")
                .fileUrl("/api/resumes/3/file")
                .build();

        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "seeker@example.com", "JOB_SEEKER");
        when(resumeRepository.findById(3L)).thenReturn(Optional.of(resume));

        resumeService.delete(3L, principal);

        verify(resumeRepository).delete(resume);
        assertThat(Files.exists(stored)).isFalse();
    }

    private byte[] validPdfBytes() {
        return "%PDF-1.4\n%dummy pdf\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }


    @Test
    void createRejectsPathTraversalFilename() {
        ReflectionTestUtils.setField(resumeService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(resumeService, "maxFileSizeBytes", 5_242_880L);

        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "seeker@example.com", "JOB_SEEKER");
        MockMultipartFile file = new MockMultipartFile("file", "../resume.pdf", "application/pdf", validPdfBytes());

        assertThatThrownBy(() -> resumeService.create(file, false, principal))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid file name");
    }

    @Test
    void createRejectsOversizedFile() {
        ReflectionTestUtils.setField(resumeService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(resumeService, "maxFileSizeBytes", 4L);

        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "seeker@example.com", "JOB_SEEKER");
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", validPdfBytes());

        assertThatThrownBy(() -> resumeService.create(file, false, principal))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Resume file size must be less than");
    }

    @Test
    void createRejectsUnauthorizedRole() {
        ReflectionTestUtils.setField(resumeService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(resumeService, "maxFileSizeBytes", 5_242_880L);

        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "recruiter@example.com", "RECRUITER");
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", validPdfBytes());

        assertThatThrownBy(() -> resumeService.create(file, false, principal))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("Only job seekers can manage resumes");
    }

    @Test
    void getByIdReturnsOwnedResume() {
        Resume resume = Resume.builder()
                .id(7L)
                .userId(5L)
                .fileName("resume.pdf")
                .fileUrl("/api/resumes/7/file")
                .storagePath("stored.pdf")
                .fileSize(123L)
                .primaryResume(true)
                .build();
        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "seeker@example.com", "JOB_SEEKER");
        when(resumeRepository.findById(7L)).thenReturn(Optional.of(resume));

        ResumeResponse response = resumeService.getById(7L, principal);

        assertThat(response)
                .extracting(ResumeResponse::getId, ResumeResponse::getFileName, ResumeResponse::isPrimary)
                .containsExactly(7L, "resume.pdf", true);
    }

    @Test
    void getByUserIdReturnsMappedResponses() {
        Resume first = Resume.builder().id(1L).userId(5L).fileName("one.pdf").fileUrl("/api/resumes/1/file").storagePath("one.pdf").build();
        Resume second = Resume.builder().id(2L).userId(5L).fileName("two.pdf").fileUrl("/api/resumes/2/file").storagePath("two.pdf").build();
        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "seeker@example.com", "JOB_SEEKER");
        when(resumeRepository.findByUserIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(first, second));

        List<ResumeResponse> responses = resumeService.getByUserId(5L, principal);

        assertThat(responses).extracting(ResumeResponse::getId).containsExactly(1L, 2L);
    }

    @Test
    void downloadFileReturnsReadableResourceWhenStoredFileExists() throws Exception {
        ReflectionTestUtils.setField(resumeService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(resumeService, "maxFileSizeBytes", 5_242_880L);

        Path stored = tempDir.resolve("stored.pdf");
        Files.write(stored, validPdfBytes());

        Resume resume = Resume.builder()
                .id(12L)
                .userId(5L)
                .storagePath("stored.pdf")
                .fileName("resume.pdf")
                .fileUrl("/api/resumes/12/file")
                .build();
        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "seeker@example.com", "JOB_SEEKER");
        when(resumeRepository.findById(12L)).thenReturn(Optional.of(resume));

        org.springframework.core.io.Resource resource = resumeService.downloadFile(12L, principal);

        assertThat(resource.exists()).isTrue();
        assertThat(resource.getFilename()).isEqualTo("stored.pdf");
    }


    @Test
    void createUsesDefaultFilenameWhenOriginalNameMissing() throws Exception {
        ReflectionTestUtils.setField(resumeService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(resumeService, "maxFileSizeBytes", 5_242_880L);

        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "seeker@example.com", "JOB_SEEKER");
        MockMultipartFile file = new MockMultipartFile("file", null, "application/pdf", validPdfBytes());
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> {
            Resume resume = invocation.getArgument(0);
            if (resume.getId() == null) {
                resume.setId(21L);
            }
            return resume;
        });
        ResumeResponse response = resumeService.create(file, false, principal);

        assertThat(response.getFileName()).isEqualTo("resume.pdf");
    }

    @Test
    void getByIdRejectsDifferentUser() {
        Resume resume = Resume.builder()
                .id(13L)
                .userId(5L)
                .fileName("resume.pdf")
                .fileUrl("/api/resumes/13/file")
                .storagePath("stored.pdf")
                .build();
        JwtUserPrincipal principal = new JwtUserPrincipal(7L, "other@example.com", "JOB_SEEKER");
        when(resumeRepository.findById(13L)).thenReturn(Optional.of(resume));

        assertThatThrownBy(() -> resumeService.getById(13L, principal))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("your own resumes");
    }

    @Test
    void deleteStillRemovesDatabaseRowWhenFileDoesNotExist() {
        ReflectionTestUtils.setField(resumeService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(resumeService, "maxFileSizeBytes", 5_242_880L);

        Resume resume = Resume.builder()
                .id(22L)
                .userId(5L)
                .storagePath("missing-again.pdf")
                .fileName("resume.pdf")
                .fileUrl("/api/resumes/22/file")
                .build();
        JwtUserPrincipal principal = new JwtUserPrincipal(5L, "seeker@example.com", "JOB_SEEKER");
        when(resumeRepository.findById(22L)).thenReturn(Optional.of(resume));

        resumeService.delete(22L, principal);

        verify(resumeRepository).delete(resume);
    }
}