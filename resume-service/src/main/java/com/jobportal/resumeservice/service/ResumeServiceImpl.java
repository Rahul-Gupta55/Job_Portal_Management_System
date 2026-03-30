package com.jobportal.resumeservice.service;

import com.jobportal.commonsecurity.security.JwtUserPrincipal;
import com.jobportal.resumeservice.dto.ResumeResponse;
import com.jobportal.resumeservice.entity.Resume;
import com.jobportal.resumeservice.exception.ResourceNotFoundException;
import com.jobportal.resumeservice.exception.UnauthorizedActionException;
import com.jobportal.resumeservice.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ResumeServiceImpl implements ResumeService {

    private static final byte[] PDF_MAGIC = "%PDF".getBytes(StandardCharsets.US_ASCII);

    private final ResumeRepository resumeRepository;

    @Value("${app.resume.upload-dir:./uploads/resumes}")
    private String uploadDir;

    @Value("${app.resume.max-file-size-bytes:5242880}")
    private long maxFileSizeBytes;

    @Override
    public ResumeResponse create(MultipartFile file, Boolean primary, JwtUserPrincipal principal) {
        ensureJobSeekerOrAdmin(principal);
        validatePdfFile(file);

        if (Boolean.TRUE.equals(primary)) {
            clearPrimary(principal.userId());
        }

        String storedFileName = storeFile(file, principal.userId());

        Resume resume = Resume.builder()
                .userId(principal.userId())
                .fileName(resolveOriginalFilename(file))
                .fileUrl("PENDING")
                .storagePath(storedFileName)
                .fileSize(file.getSize())
                .primaryResume(Boolean.TRUE.equals(primary))
                .build();

        Resume savedResume = resumeRepository.save(resume);
        savedResume.setFileUrl("/api/resumes/" + savedResume.getId() + "/file");

        ResumeResponse response = toResponse(resumeRepository.save(savedResume));

        log.info("Resume created successfully. resumeId={}, userId={}, primary={}",
                savedResume.getId(), savedResume.getUserId(), savedResume.isPrimaryResume());

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeResponse getById(Long id, JwtUserPrincipal principal) {
        Resume resume = findById(id);
        ensureOwnerOrAdmin(resume.getUserId(), principal);

        log.info("Resume fetched successfully. resumeId={}, requestedBy={}",
                resume.getId(), principal.userId());

        return toResponse(resume);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeResponse> getByUserId(Long userId, JwtUserPrincipal principal) {
        ensureOwnerOrAdmin(userId, principal);

        List<ResumeResponse> resumes = resumeRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();

        log.info("Resume list fetched successfully. userId={}, count={}", userId, resumes.size());

        return resumes;
    }

    @Override
    public ResumeResponse setPrimary(Long id, JwtUserPrincipal principal) {
        Resume resume = findById(id);
        ensureOwnerOrAdmin(resume.getUserId(), principal);

        clearPrimary(resume.getUserId());
        resume.setPrimaryResume(true);

        ResumeResponse response = toResponse(resumeRepository.save(resume));

        log.info("Primary resume updated successfully. resumeId={}, userId={}",
                resume.getId(), resume.getUserId());

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadFile(Long id, JwtUserPrincipal principal) {
        Resume resume = findById(id);
        ensureOwnerOrAdmin(resume.getUserId(), principal);

        try {
            Path filePath = resolveUploadDirectory().resolve(resume.getStoragePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                log.warn("Resume file not found or not readable. resumeId={}, userId={}", id, resume.getUserId());
                throw new ResourceNotFoundException("Resume file", id);
            }

            log.info("Resume file downloaded successfully. resumeId={}, requestedBy={}",
                    resume.getId(), principal.userId());

            return resource;
        } catch (MalformedURLException ex) {
            log.error("Failed to download resume file. resumeId={}", id, ex);
            throw new IllegalStateException("Unable to read stored resume file", ex);
        }
    }

    @Override
    public void delete(Long id, JwtUserPrincipal principal) {
        Resume resume = findById(id);
        ensureOwnerOrAdmin(resume.getUserId(), principal);

        deleteStoredFileIfExists(resume.getStoragePath());
        resumeRepository.delete(resume);

        log.info("Resume deleted successfully. resumeId={}, userId={}",
                resume.getId(), resume.getUserId());
    }

    private Resume findById(Long id) {
        return resumeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", id));
    }

    private void validatePdfFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("Resume validation failed. File is missing or empty.");
            throw new IllegalArgumentException("Resume PDF is required");
        }

        String cleanedFilename = normalizeOriginalFilename(file);

        if (StringUtils.hasText(cleanedFilename)) {
            if (cleanedFilename.contains("..")) {
                log.warn("Resume validation failed. Invalid file name.");
                throw new IllegalArgumentException("Invalid file name");
            }

            if (!cleanedFilename.toLowerCase().endsWith(".pdf")) {
                log.warn("Resume validation failed. Only PDF files are allowed.");
                throw new IllegalArgumentException("Only PDF files are allowed");
            }
        }

        if (file.getSize() > maxFileSizeBytes) {
            log.warn("Resume validation failed. File size exceeded limit. size={}, max={}",
                    file.getSize(), maxFileSizeBytes);
            throw new IllegalArgumentException("Resume file size must be less than " + maxFileSizeBytes + " bytes");
        }

        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(PDF_MAGIC.length);

            if (header.length != PDF_MAGIC.length || !Arrays.equals(header, PDF_MAGIC)) {
                log.warn("Resume validation failed. Invalid PDF signature.");
                throw new IllegalArgumentException("Uploaded file is not a valid PDF");
            }
        } catch (IOException ex) {
            log.error("Failed to validate uploaded resume file.", ex);
            throw new IllegalStateException("Failed to inspect uploaded resume file", ex);
        }
    }

    private String resolveOriginalFilename(MultipartFile file) {
        String cleanedFilename = normalizeOriginalFilename(file);
        return StringUtils.hasText(cleanedFilename) ? cleanedFilename : "resume.pdf";
    }

    private String normalizeOriginalFilename(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        return StringUtils.cleanPath(originalFilename == null ? "" : originalFilename.trim());
    }

    private String storeFile(MultipartFile file, Long userId) {
        String storedFileName = userId + "_" + UUID.randomUUID() + ".pdf";

        try {
            Path uploadPath = resolveUploadDirectory();
            Files.createDirectories(uploadPath);
            Files.copy(file.getInputStream(), uploadPath.resolve(storedFileName), StandardCopyOption.REPLACE_EXISTING);

            log.info("Resume file stored successfully for userId={}", userId);
            return storedFileName;
        } catch (IOException ex) {
            log.error("Failed to store resume file for userId={}", userId, ex);
            throw new IllegalStateException("Failed to store resume file", ex);
        }
    }

    private Path resolveUploadDirectory() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    private void deleteStoredFileIfExists(String storagePath) {
        try {
            boolean deleted = Files.deleteIfExists(resolveUploadDirectory().resolve(storagePath).normalize());

            if (!deleted) {
                log.warn("Resume file delete skipped. File not found.");
            }
        } catch (IOException ex) {
            log.error("Failed to delete stored resume file.", ex);
            throw new IllegalStateException("Failed to delete stored resume file", ex);
        }
    }

    private void clearPrimary(Long userId) {
        resumeRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .forEach(existing -> existing.setPrimaryResume(false));

        log.info("Existing primary resumes cleared for userId={}", userId);
    }

    private void ensureJobSeekerOrAdmin(JwtUserPrincipal principal) {
        if (!"JOB_SEEKER".equals(principal.role()) && !"ADMIN".equals(principal.role())) {
            log.warn("Unauthorized resume action. userId={}, role={}",
                    principal.userId(), principal.role());
            throw new UnauthorizedActionException("Only job seekers can manage resumes");
        }
    }

    private void ensureOwnerOrAdmin(Long userId, JwtUserPrincipal principal) {
        if (principal == null || (!principal.userId().equals(userId) && !"ADMIN".equals(principal.role()))) {
            log.warn("Unauthorized access to resume. requesterId={}, ownerId={}",
                    principal == null ? null : principal.userId(), userId);
            throw new UnauthorizedActionException("You can access only your own resumes");
        }
    }

    private ResumeResponse toResponse(Resume resume) {
        return ResumeResponse.builder()
                .id(resume.getId())
                .userId(resume.getUserId())
                .fileName(resume.getFileName())
                .fileUrl(resume.getFileUrl())
                .fileSize(resume.getFileSize())
                .primary(resume.isPrimaryResume())
                .createdAt(resume.getCreatedAt())
                .build();
    }
}