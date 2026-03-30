package com.jobportal.applicationservice.service;

import com.jobportal.applicationservice.dto.*;
import com.jobportal.commonsecurity.security.JwtUserPrincipal;

import java.util.List;

public interface ApplicationService {
    ApplicationResponse apply(ApplyRequest request, JwtUserPrincipal principal);
    ApplicationResponse getById(Long id, JwtUserPrincipal principal);
    List<ApplicationResponse> getByJobId(Long jobId, JwtUserPrincipal principal);
    List<ApplicationResponse> getByCandidateId(Long candidateId, JwtUserPrincipal principal);
    ApplicationResponse updateStatus(Long id, StatusUpdateRequest request, JwtUserPrincipal principal);
    ApplicationResponse withdraw(Long id, JwtUserPrincipal principal);
    List<ApplicationStatusHistoryResponse> getHistory(Long id, JwtUserPrincipal principal);
}
