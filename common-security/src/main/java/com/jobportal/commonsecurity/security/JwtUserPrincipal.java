package com.jobportal.commonsecurity.security;

public record JwtUserPrincipal(Long userId, String email, String role) {

}

