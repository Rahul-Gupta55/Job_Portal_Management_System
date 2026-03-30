package com.jobportal.resumeservice.service;

import com.jobportal.resumeservice.dto.ResumeResponse;
import com.jobportal.commonsecurity.security.JwtUserPrincipal;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface ResumeService {
    ResumeResponse create(MultipartFile file, Boolean primary, JwtUserPrincipal principal);
    ResumeResponse getById(Long id, JwtUserPrincipal principal);
    List<ResumeResponse> getByUserId(Long userId, JwtUserPrincipal principal);
    ResumeResponse setPrimary(Long id, JwtUserPrincipal principal);
    Resource downloadFile(Long id, JwtUserPrincipal principal);
    void delete(Long id, JwtUserPrincipal principal);
}
