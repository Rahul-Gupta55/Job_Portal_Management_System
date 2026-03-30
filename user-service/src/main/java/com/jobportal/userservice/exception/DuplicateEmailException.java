package com.jobportal.userservice.exception;

public class DuplicateEmailException extends JobPortalException {
    public DuplicateEmailException(String email) {
        super("User already exists with email: " + email);
    }
}
