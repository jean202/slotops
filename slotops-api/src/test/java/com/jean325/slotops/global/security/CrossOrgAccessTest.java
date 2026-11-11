package com.jean325.slotops.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Tenant isolation, end to end through the real JWT filter and
 * {@link OrgAccessAspect}: user 2001 belongs only to org 2001 and must never
 * be able to read or change anything that belongs to org 2002.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql({"/sql/clean.sql", "/sql/two-orgs.sql"})
class CrossOrgAccessTest {

    private static final String RULE = """
            { "dayOfWeek": 2, "startTime": "09:00", "endTime": "18:00", "isOpen": true, "leadTimeMinutes": 0 }
            """;
    private static final String BLACKOUT = """
            { "startAt": "2026-08-01T00:00:00+09:00", "endAt": "2026-08-02T00:00:00+09:00", "reason": "x" }
            """;
    private static final String LOCATION = """
            { "name": "Hijacked", "timezone": "Asia/Seoul" }
            """;
    private static final String RESOURCE = """
            { "resourceType": "ROOM", "name": "Hijacked", "capacity": 2, "slotUnitMinutes": 60,
              "minBookingMinutes": 60, "maxBookingMinutes": 120, "bufferMinutes": 0,
              "basePrice": 1000, "currency": "KRW" }
            """;

    record Endpoint(HttpMethod method, String path, String body) {
        @Override
        public String toString() {
            return method + " " + path;
        }
    }

    private static Endpoint of(HttpMethod method, String path) {
        return new Endpoint(method, path, null);
    }

    private static Endpoint of(HttpMethod method, String path, String body) {
        return new Endpoint(method, path, body);
    }

    /** Every org-scoped endpoint, parameterized by the org in the path and the ids of its children. */
    private static List<Endpoint> orgScopedEndpoints(long org, long child) {
        String api = "/api/v1/organizations/" + org;
        String rsv = api + "/reservations/" + child;
        String res = api + "/resources/" + child;
        String loc = "/organizations/" + org + "/locations";
        String locRes = loc + "/" + child + "/resources";
        return List.of(
                of(HttpMethod.GET, api + "/reservations"),
                of(HttpMethod.POST, rsv + "/confirm"),
                of(HttpMethod.POST, rsv + "/cancel"),
                of(HttpMethod.POST, rsv + "/complete"),
                of(HttpMethod.POST, rsv + "/no-show"),
                of(HttpMethod.GET, rsv + "/payment"),
                of(HttpMethod.PUT, rsv + "/payment", "{ \"status\": \"PAID\" }"),
                of(HttpMethod.GET, rsv + "/notes"),
                of(HttpMethod.POST, rsv + "/notes", "{ \"content\": \"intruder\" }"),
                of(HttpMethod.GET, api + "/addons"),
                of(HttpMethod.POST, api + "/addons", "{ \"name\": \"Hijacked\", \"price\": 1 }"),
                of(HttpMethod.GET, api + "/customers/" + child + "/reservations"),
                of(HttpMethod.GET, api + "/dashboard/summary"),
                of(HttpMethod.GET, res + "/availability-rules"),
                of(HttpMethod.POST, res + "/availability-rules", RULE),
                of(HttpMethod.PUT, res + "/availability-rules/" + child, RULE),
                of(HttpMethod.DELETE, res + "/availability-rules/" + child),
                of(HttpMethod.GET, res + "/blackout-periods"),
                of(HttpMethod.POST, res + "/blackout-periods", BLACKOUT),
                of(HttpMethod.DELETE, res + "/blackout-periods/" + child),
                of(HttpMethod.GET, loc),
                of(HttpMethod.POST, loc, LOCATION),
                of(HttpMethod.GET, loc + "/" + child),
                of(HttpMethod.PUT, loc + "/" + child, "{ \"name\": \"Hijacked\" }"),
                of(HttpMethod.DELETE, loc + "/" + child),
                of(HttpMethod.GET, locRes),
                of(HttpMethod.POST, locRes, RESOURCE),
                of(HttpMethod.GET, locRes + "/" + child),
                of(HttpMethod.PUT, locRes + "/" + child, "{ \"name\": \"Hijacked\" }"),
                of(HttpMethod.DELETE, locRes + "/" + child)
        );
    }

    static Stream<Endpoint> otherOrgEndpoints() {
        return Stream.concat(
                orgScopedEndpoints(2002, 2002).stream(),
                Stream.of(
                        of(HttpMethod.GET, "/organizations/2002"),
                        of(HttpMethod.GET, "/organizations/2002/members"),
                        of(HttpMethod.POST, "/organizations/2002/members", "{ \"userId\": 2001, \"role\": \"OWNER\" }")
                ));
    }

    /** Own org in the path, but child ids that belong to the other org. */
    static Stream<Endpoint> ownOrgPathWithForeignChildIds() {
        return orgScopedEndpoints(2001, 2002).stream()
                // Collection endpoints without a child id legitimately succeed for a member.
                .filter(e -> e.path().contains("2002"));
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JwtTokenProvider tokenProvider;

    private MockHttpServletRequestBuilder call(Endpoint endpoint) {
        MockHttpServletRequestBuilder builder = request(endpoint.method(), endpoint.path());
        if (endpoint.body() != null) {
            builder.contentType("application/json").content(endpoint.body());
        }
        return builder;
    }

    private MockHttpServletRequestBuilder asAlphaStaff(MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization",
                "Bearer " + tokenProvider.generateAccessToken(2001L, "alpha@example.com"));
    }

    /** Snapshot of everything org 2002 owns that a successful attack would change. */
    private String betaState() {
        return String.join("|",
                jdbcTemplate.queryForObject("select status from reservations where id = 2002", String.class),
                jdbcTemplate.queryForObject("select payment_status from payments where id = 2002", String.class),
                jdbcTemplate.queryForObject("select name from locations where id = 2002", String.class),
                jdbcTemplate.queryForObject("select name from resources where id = 2002", String.class),
                count("locations where organization_id = 2002"),
                count("resources where organization_id = 2002"),
                count("addons where organization_id = 2002"),
                count("staff_notes where reservation_id = 2002"),
                count("availability_rules where resource_id = 2002"),
                count("blackout_periods where resource_id = 2002"),
                count("organization_members where organization_id = 2002"));
    }

    private String count(String fromWhere) {
        return String.valueOf(jdbcTemplate.queryForObject("select count(*) from " + fromWhere, Integer.class));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("otherOrgEndpoints")
    void memberOfAnotherOrgIsForbidden(Endpoint endpoint) throws Exception {
        String before = betaState();

        mockMvc.perform(asAlphaStaff(call(endpoint)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());

        assertThat(betaState()).isEqualTo(before);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("otherOrgEndpoints")
    void unauthenticatedCallerIsRejected(Endpoint endpoint) throws Exception {
        String before = betaState();

        mockMvc.perform(call(endpoint))
                .andExpect(status().isUnauthorized());

        assertThat(betaState()).isEqualTo(before);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("ownOrgPathWithForeignChildIds")
    void cannotReachAnotherOrgsDataThroughOwnOrgPath(Endpoint endpoint) throws Exception {
        String before = betaState();

        // Denied either as "not found in your org" (404) or, for a resource addressed
        // through its real foreign location, as "access denied" (403). Never 2xx.
        int status = mockMvc.perform(asAlphaStaff(call(endpoint)))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn().getResponse().getStatus();
        assertThat(status).isIn(403, 404);

        assertThat(betaState()).isEqualTo(before);
    }

    @Test
    void memberCanUseOwnOrgAndSeesOnlyOwnData() throws Exception {
        mockMvc.perform(asAlphaStaff(get("/api/v1/organizations/2001/reservations")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].reservationCode").value("RSV-ALPHA"));

        mockMvc.perform(asAlphaStaff(get("/api/v1/organizations/2001/addons")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Alpha Projector"));

        mockMvc.perform(asAlphaStaff(get("/organizations/2001/locations")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Alpha Studio"));

        mockMvc.perform(asAlphaStaff(get("/organizations/2001/locations/2001/resources")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Alpha Room"));
    }

    @Test
    void userWithoutAnyMembershipIsForbidden() throws Exception {
        String token = tokenProvider.generateAccessToken(9999L, "nobody@example.com");

        mockMvc.perform(get("/api/v1/organizations/2001/reservations").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/organizations/2001/locations").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
