package com.jobportal.userservice.exception;

public class ResourceNotFoundException extends JobPortalException {
    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " not found with id: " + id);
    }
}
