package com.jean325.slotops.global.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Runs requests through the real security filter chain (no @WithMockUser) to
 * verify which requests the JWT filter authenticates and which are rejected.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql("/sql/clean.sql")
class JwtAuthenticationFilterTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JwtTokenProvider tokenProvider;
    @Value("${jwt.secret}") private String secret;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("""
                insert into users (id, email, password_hash, name, status, created_at, updated_at)
                values (1001, 'member@example.com', 'x', 'Member', 'ACTIVE', current_timestamp, current_timestamp)
                """);
        jdbcTemplate.execute("""
                insert into organizations (id, name, slug, status, created_at, updated_at)
                values (1001, 'Mine', 'mine', 'ACTIVE', current_timestamp, current_timestamp)
                """);
        jdbcTemplate.execute("""
                insert into organizations (id, name, slug, status, created_at, updated_at)
                values (1002, 'Theirs', 'theirs', 'ACTIVE', current_timestamp, current_timestamp)
                """);
        jdbcTemplate.execute("""
                insert into organization_members (organization_id, user_id, role, joined_at, created_at, updated_at)
                values (1001, 1001, 'OWNER', current_timestamp, current_timestamp, current_timestamp)
                """);
        jdbcTemplate.execute("""
                insert into organization_members (organization_id, user_id, role, joined_at, created_at, updated_at)
                values (1002, 1002, 'OWNER', current_timestamp, current_timestamp, current_timestamp)
                """);
    }

    private String bearer(long userId) {
        return "Bearer " + tokenProvider.generateAccessToken(userId, "member@example.com");
    }

    @Test
    void rejectsProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/organizations"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void rejectsMalformedToken() throws Exception {
        mockMvc.perform(get("/organizations").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTokenWithoutBearerPrefix() throws Exception {
        String rawToken = tokenProvider.generateAccessToken(1001L, "member@example.com");

        mockMvc.perform(get("/organizations").header("Authorization", rawToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/organizations").header("Authorization", "Basic " + rawToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        String expired = new JwtTokenProvider(secret, -60_000, -60_000)
                .generateAccessToken(1001L, "member@example.com");

        mockMvc.perform(get("/organizations").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() throws Exception {
        String forged = new JwtTokenProvider(
                "attacker-controlled-secret-key-long-enough-for-hs256-signing", 3_600_000, 3_600_000)
                .generateAccessToken(1001L, "member@example.com");

        mockMvc.perform(get("/organizations").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenIdentifiesTheCallerByUserId() throws Exception {
        mockMvc.perform(get("/organizations").header("Authorization", bearer(1001L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].slug").value("mine"));

        mockMvc.perform(get("/organizations").header("Authorization", bearer(1002L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].slug").value("theirs"));
    }

    @Test
    void authEndpointsAreReachableWithoutToken() throws Exception {
        // Reaches the controller (credentials rejected), not the 401 entry point.
        mockMvc.perform(post("/auth/login").contentType("application/json").content("""
                        { "email": "nobody@example.com", "password": "password123" }
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void publicEndpointsAreReachableWithoutToken() throws Exception {
        // Reaches the controller (unknown resource), not the 401 entry point.
        mockMvc.perform(get("/api/v1/public/resources/999999/available-slots")
                        .param("date", "2026-06-10")
                        .param("duration", "60"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void invalidTokenDoesNotBlockPublicEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/public/resources/999999/available-slots")
                        .header("Authorization", "Bearer not-a-jwt")
                        .param("date", "2026-06-10")
                        .param("duration", "60"))
                .andExpect(status().isNotFound());
    }
}
