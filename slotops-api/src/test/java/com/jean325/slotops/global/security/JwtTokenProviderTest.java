package com.jean325.slotops.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET = "unit-test-secret-key-that-is-long-enough-for-hs256-signing";
    private static final String OTHER_SECRET = "another-secret-key-that-is-also-long-enough-for-hs256-use";

    private final JwtTokenProvider provider = new JwtTokenProvider(SECRET, 3_600_000, 604_800_000);

    @Test
    void accessTokenCarriesUserIdAndEmail() {
        String token = provider.generateAccessToken(42L, "owner@example.com");

        assertThat(provider.isTokenValid(token)).isTrue();
        assertThat(provider.extractUserId(token)).isEqualTo(42L);
        assertThat(provider.extractEmail(token)).isEqualTo("owner@example.com");
    }

    @Test
    void refreshTokenCarriesUserIdAndEmail() {
        String token = provider.generateRefreshToken(7L, "staff@example.com");

        assertThat(provider.isTokenValid(token)).isTrue();
        assertThat(provider.extractUserId(token)).isEqualTo(7L);
        assertThat(provider.extractEmail(token)).isEqualTo("staff@example.com");
    }

    @Test
    void rejectsGarbageAndEmptyTokens() {
        assertThat(provider.isTokenValid("not-a-jwt")).isFalse();
        assertThat(provider.isTokenValid("")).isFalse();
        assertThat(provider.isTokenValid(null)).isFalse();
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        JwtTokenProvider other = new JwtTokenProvider(OTHER_SECRET, 3_600_000, 604_800_000);

        assertThat(provider.isTokenValid(other.generateAccessToken(42L, "owner@example.com"))).isFalse();
    }

    @Test
    void rejectsExpiredToken() {
        JwtTokenProvider expired = new JwtTokenProvider(SECRET, -60_000, -60_000);

        assertThat(provider.isTokenValid(expired.generateAccessToken(42L, "owner@example.com"))).isFalse();
        assertThat(provider.isTokenValid(expired.generateRefreshToken(42L, "owner@example.com"))).isFalse();
    }

    @Test
    void rejectsTokenWithTamperedPayload() {
        String token = provider.generateAccessToken(42L, "owner@example.com");
        String forged = provider.generateAccessToken(1L, "admin@example.com");
        String[] original = token.split("\\.");
        String[] other = forged.split("\\.");

        // header + someone else's payload + original signature
        String tampered = original[0] + "." + other[1] + "." + original[2];

        assertThat(provider.isTokenValid(tampered)).isFalse();
    }
}
