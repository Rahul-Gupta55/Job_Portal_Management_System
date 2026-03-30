package com.jobportal.resumeservice.exception;

public class UnauthorizedActionException extends JobPortalException { public UnauthorizedActionException(String action) { super("Unauthorized: " + action); } }
