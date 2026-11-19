package com.pcc.identity.service;

/** A freshly issued refresh token: raw value (returned to client once) + persisted row id. */
public record IssuedRefreshToken(String rawValue, Long id) {
}
