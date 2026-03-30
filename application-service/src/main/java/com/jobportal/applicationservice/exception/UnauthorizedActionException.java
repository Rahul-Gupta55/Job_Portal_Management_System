package com.jobportal.applicationservice.exception;

public class UnauthorizedActionException extends JobPortalException {
    public UnauthorizedActionException(String action) {
        super("Unauthorized: " + action);
    }
}
