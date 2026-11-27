package com.jean325.slotops.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Exercises signup/login/refresh through the real security filter chain
 * (no @WithMockUser), so the issued tokens are verified end to end.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql("/sql/clean.sql")
class AuthControllerTest {

    private static final String SIGNUP_BODY = """
            { "email": "owner@example.com", "password": "password123", "name": "Owner", "phone": "010-1111-2222" }
            """;
    private static final String LOGIN_BODY = """
            { "email": "owner@example.com", "password": "password123" }
            """;

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    private void signup() throws Exception {
        mockMvc.perform(post("/auth/signup").contentType("application/json").content(SIGNUP_BODY))
                .andExpect(status().isCreated());
    }

    private JsonNode login() throws Exception {
        String json = mockMvc.perform(post("/auth/login").contentType("application/json").content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("data");
    }

    // --- signup ---

    @Test
    void signupCreatesActiveUserAndDoesNotExposePassword() throws Exception {
        mockMvc.perform(post("/auth/signup").contentType("application/json").content(SIGNUP_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").isNumber())
                .andExpect(jsonPath("$.data.email").value("owner@example.com"))
                .andExpect(jsonPath("$.data.name").value("Owner"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        String status = jdbcTemplate.queryForObject(
                "select status from users where email = 'owner@example.com'", String.class);
        String phone = jdbcTemplate.queryForObject(
                "select phone from users where email = 'owner@example.com'", String.class);
        assertThat(status).isEqualTo("ACTIVE");
        assertThat(phone).isEqualTo("010-1111-2222");
    }

    @Test
    void signupStoresPasswordHashedNotPlaintext() throws Exception {
        signup();

        String stored = jdbcTemplate.queryForObject(
                "select password_hash from users where email = 'owner@example.com'", String.class);
        assertThat(stored).isNotEqualTo("password123").doesNotContain("password123");
    }

    @Test
    void signupRejectsDuplicateEmail() throws Exception {
        signup();

        mockMvc.perform(post("/auth/signup").contentType("application/json").content(SIGNUP_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));

        Integer count = jdbcTemplate.queryForObject("select count(*) from users", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void signupRejectsShortPassword() throws Exception {
        String body = """
                { "email": "owner@example.com", "password": "short", "name": "Owner" }
                """;
        mockMvc.perform(post("/auth/signup").contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"));
    }

    @Test
    void signupRejectsMalformedEmail() throws Exception {
        String body = """
                { "email": "not-an-email", "password": "password123", "name": "Owner" }
                """;
        mockMvc.perform(post("/auth/signup").contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
    }

    @Test
    void signupRejectsBlankName() throws Exception {
        String body = """
                { "email": "owner@example.com", "password": "password123", "name": " " }
                """;
        mockMvc.perform(post("/auth/signup").contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Disabled("FINDINGS.md #3: SignupRequest.email has @Email but no @NotBlank, so a missing email "
            + "passes validation and fails later at the DB instead of returning 400")
    @Test
    void signupRejectsMissingEmail() throws Exception {
        String body = """
                { "password": "password123", "name": "Owner" }
                """;
        mockMvc.perform(post("/auth/signup").contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // --- login ---

    @Test
    void loginReturnsTokensThatAuthenticateProtectedEndpoints() throws Exception {
        signup();

        JsonNode data = login();

        assertThat(data.get("email").asText()).isEqualTo("owner@example.com");
        assertThat(data.get("name").asText()).isEqualTo("Owner");
        assertThat(data.get("expiresIn").asLong()).isEqualTo(3_600_000L);
        assertThat(data.get("refreshToken").asText()).isNotBlank();

        mockMvc.perform(get("/organizations")
                        .header("Authorization", "Bearer " + data.get("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void loginRejectsWrongPassword() throws Exception {
        signup();

        String body = """
                { "email": "owner@example.com", "password": "wrong-password" }
                """;
        mockMvc.perform(post("/auth/login").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    void loginDoesNotRevealWhetherEmailExists() throws Exception {
        signup();

        String wrongPassword = mockMvc.perform(post("/auth/login").contentType("application/json").content("""
                        { "email": "owner@example.com", "password": "wrong-password" }
                        """))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        String unknownEmail = mockMvc.perform(post("/auth/login").contentType("application/json").content("""
                        { "email": "nobody@example.com", "password": "password123" }
                        """))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(unknownEmail).isEqualTo(wrongPassword);
    }

    @Test
    void loginRejectsBlankPassword() throws Exception {
        String body = """
                { "email": "owner@example.com", "password": "" }
                """;
        mockMvc.perform(post("/auth/login").contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Disabled("FINDINGS.md #2: AuthService.login never checks UserStatus, so SUSPENDED/INACTIVE "
            + "users still receive tokens")
    @Test
    void loginRejectsSuspendedUser() throws Exception {
        signup();
        jdbcTemplate.update("update users set status = 'SUSPENDED' where email = 'owner@example.com'");

        mockMvc.perform(post("/auth/login").contentType("application/json").content(LOGIN_BODY))
                .andExpect(status().is4xxClientError());
    }

    // --- refresh ---

    @Test
    void refreshIssuesWorkingTokensForSameUser() throws Exception {
        signup();
        JsonNode loggedIn = login();

        String body = objectMapper.writeValueAsString(
                java.util.Map.of("refreshToken", loggedIn.get("refreshToken").asText()));
        String json = mockMvc.perform(post("/auth/refresh").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(loggedIn.get("userId").asLong()))
                .andExpect(jsonPath("$.data.email").value("owner@example.com"))
                .andReturn().getResponse().getContentAsString();
        String newAccessToken = objectMapper.readTree(json).get("data").get("accessToken").asText();

        mockMvc.perform(get("/organizations").header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isOk());
    }

    @Test
    void refreshRejectsInvalidToken() throws Exception {
        String body = """
                { "refreshToken": "not-a-real-token" }
                """;
        mockMvc.perform(post("/auth/refresh").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void refreshRejectsBlankToken() throws Exception {
        String body = """
                { "refreshToken": "" }
                """;
        mockMvc.perform(post("/auth/refresh").contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void refreshFailsWhenUserNoLongerExists() throws Exception {
        signup();
        JsonNode loggedIn = login();
        jdbcTemplate.update("delete from users");

        String body = objectMapper.writeValueAsString(
                java.util.Map.of("refreshToken", loggedIn.get("refreshToken").asText()));
        mockMvc.perform(post("/auth/refresh").contentType("application/json").content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Disabled("FINDINGS.md #1: access and refresh tokens are indistinguishable (no token-type claim), "
            + "so a short-lived access token can be exchanged for a fresh 7-day refresh token")
    @Test
    void refreshRejectsAccessTokenUsedAsRefreshToken() throws Exception {
        signup();
        JsonNode loggedIn = login();

        String body = objectMapper.writeValueAsString(
                java.util.Map.of("refreshToken", loggedIn.get("accessToken").asText()));
        mockMvc.perform(post("/auth/refresh").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }
}
