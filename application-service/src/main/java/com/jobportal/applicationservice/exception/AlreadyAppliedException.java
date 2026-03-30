package com.jobportal.applicationservice.exception;

public class AlreadyAppliedException extends JobPortalException {
    public AlreadyAppliedException(Long jobId) {
        super("Already applied to job: " + jobId);
    }
}
