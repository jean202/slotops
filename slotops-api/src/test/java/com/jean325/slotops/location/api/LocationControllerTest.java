package com.jean325.slotops.location.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
@Sql("/sql/clean.sql")
class LocationControllerTest {

    private static final long MEMBER = 3001L;
    private static final long OUTSIDER = 3002L;
    private static final String BASE = "/organizations/3001/locations";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("""
                insert into users (id, email, password_hash, name, status, created_at, updated_at) values
                    (3001, 'member@example.com', 'x', 'Member', 'ACTIVE', current_timestamp, current_timestamp),
                    (3002, 'outsider@example.com', 'x', 'Outsider', 'ACTIVE', current_timestamp, current_timestamp)
                """);
        jdbcTemplate.execute("""
                insert into organizations (id, name, slug, status, created_at, updated_at) values
                    (3001, 'Mine', 'mine', 'ACTIVE', current_timestamp, current_timestamp),
                    (3002, 'Theirs', 'theirs', 'ACTIVE', current_timestamp, current_timestamp)
                """);
        jdbcTemplate.execute("""
                insert into organization_members (organization_id, user_id, role, joined_at, created_at, updated_at) values
                    (3001, 3001, 'OWNER', current_timestamp, current_timestamp, current_timestamp),
                    (3002, 3002, 'OWNER', current_timestamp, current_timestamp, current_timestamp)
                """);
        jdbcTemplate.execute("""
                insert into locations (
                    id, organization_id, name, timezone, phone, address_line1, address_line2, status, created_at, updated_at
                ) values
                    (3001, 3001, 'Gangnam', 'Asia/Seoul', '02-111-2222', '1 Teheran-ro', '3F', 'ACTIVE',
                     current_timestamp, current_timestamp),
                    (3002, 3002, 'Their Place', 'Asia/Tokyo', null, null, null, 'ACTIVE',
                     current_timestamp, current_timestamp)
                """);
    }

    private long createLocation(String name) throws Exception {
        String json = mockMvc.perform(post(BASE)
                        .requestAttr("userId", MEMBER)
                        .contentType("application/json")
                        .content("{ \"name\": \"%s\", \"timezone\": \"Asia/Seoul\" }".formatted(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("data").get("id").asLong();
    }

    @Test
    void createsLocationInTheOrganization() throws Exception {
        String body = """
                { "name": "Hongdae", "timezone": "Asia/Seoul", "phone": "02-333-4444",
                  "addressLine1": "10 Yanghwa-ro", "addressLine2": "B1" }
                """;
        mockMvc.perform(post(BASE).requestAttr("userId", MEMBER).contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.organizationId").value(3001))
                .andExpect(jsonPath("$.data.name").value("Hongdae"))
                .andExpect(jsonPath("$.data.timezone").value("Asia/Seoul"))
                .andExpect(jsonPath("$.data.phone").value("02-333-4444"))
                .andExpect(jsonPath("$.data.addressLine1").value("10 Yanghwa-ro"))
                .andExpect(jsonPath("$.data.addressLine2").value("B1"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void createdLocationIsRetrievableAndListed() throws Exception {
        long id = createLocation("Hongdae");

        mockMvc.perform(get(BASE + "/" + id).requestAttr("userId", MEMBER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Hongdae"));
        mockMvc.perform(get(BASE).requestAttr("userId", MEMBER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void rejectsBlankName() throws Exception {
        mockMvc.perform(post(BASE).requestAttr("userId", MEMBER).contentType("application/json")
                        .content("{ \"name\": \"\", \"timezone\": \"Asia/Seoul\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void rejectsMissingTimezone() throws Exception {
        mockMvc.perform(post(BASE).requestAttr("userId", MEMBER).contentType("application/json")
                        .content("{ \"name\": \"Hongdae\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("timezone"));
    }

    @Test
    void rejectsTimezoneWithIllegalCharacters() throws Exception {
        mockMvc.perform(post(BASE).requestAttr("userId", MEMBER).contentType("application/json")
                        .content("{ \"name\": \"Hongdae\", \"timezone\": \"Asia/Seoul 9\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("timezone"));
    }

    @Test
    void getsLocation() throws Exception {
        mockMvc.perform(get(BASE + "/3001").requestAttr("userId", MEMBER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(3001))
                .andExpect(jsonPath("$.data.name").value("Gangnam"))
                .andExpect(jsonPath("$.data.timezone").value("Asia/Seoul"));
    }

    @Test
    void returnsNotFoundForUnknownLocation() throws Exception {
        mockMvc.perform(get(BASE + "/999999").requestAttr("userId", MEMBER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LOCATION_NOT_FOUND"));
    }

    @Test
    void listsOnlyLocationsOfTheOrganization() throws Exception {
        mockMvc.perform(get(BASE).requestAttr("userId", MEMBER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Gangnam"));
    }

    @Test
    void updateChangesOnlyProvidedFields() throws Exception {
        mockMvc.perform(put(BASE + "/3001").requestAttr("userId", MEMBER).contentType("application/json")
                        .content("{ \"name\": \"Gangnam HQ\", \"status\": \"INACTIVE\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Gangnam HQ"))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        mockMvc.perform(get(BASE + "/3001").requestAttr("userId", MEMBER))
                .andExpect(jsonPath("$.data.name").value("Gangnam HQ"))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"))
                .andExpect(jsonPath("$.data.timezone").value("Asia/Seoul"))
                .andExpect(jsonPath("$.data.phone").value("02-111-2222"))
                .andExpect(jsonPath("$.data.addressLine1").value("1 Teheran-ro"))
                .andExpect(jsonPath("$.data.addressLine2").value("3F"));
    }

    @Test
    void updateReturnsNotFoundForUnknownLocation() throws Exception {
        mockMvc.perform(put(BASE + "/999999").requestAttr("userId", MEMBER).contentType("application/json")
                        .content("{ \"name\": \"Ghost\" }"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LOCATION_NOT_FOUND"));
    }

    @Test
    void deletesLocation() throws Exception {
        mockMvc.perform(delete(BASE + "/3001").requestAttr("userId", MEMBER))
                .andExpect(status().isOk());

        mockMvc.perform(get(BASE + "/3001").requestAttr("userId", MEMBER))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(BASE).requestAttr("userId", MEMBER))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void deleteReturnsNotFoundForUnknownLocation() throws Exception {
        mockMvc.perform(delete(BASE + "/999999").requestAttr("userId", MEMBER))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotTouchAnotherOrganizationsLocationThroughOwnPath() throws Exception {
        mockMvc.perform(get(BASE + "/3002").requestAttr("userId", MEMBER))
                .andExpect(status().isNotFound());
        mockMvc.perform(put(BASE + "/3002").requestAttr("userId", MEMBER).contentType("application/json")
                        .content("{ \"name\": \"Hijacked\" }"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(BASE + "/3002").requestAttr("userId", MEMBER))
                .andExpect(status().isNotFound());

        String name = jdbcTemplate.queryForObject("select name from locations where id = 3002", String.class);
        assertThat(name).isEqualTo("Their Place");
    }

    @Test
    void rejectsNonMember() throws Exception {
        mockMvc.perform(get(BASE).requestAttr("userId", OUTSIDER))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE).requestAttr("userId", OUTSIDER).contentType("application/json")
                        .content("{ \"name\": \"Squat\", \"timezone\": \"Asia/Seoul\" }"))
                .andExpect(status().isForbidden());

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from locations where organization_id = 3001", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Disabled("FINDINGS.md #6: timezone is only pattern-checked ([A-Za-z_/]+), so a non-existent zone id "
            + "is stored and later breaks ZoneId.of() in slot calculation and booking")
    @Test
    void rejectsTimezoneThatIsNotARealZoneId() throws Exception {
        mockMvc.perform(post(BASE).requestAttr("userId", MEMBER).contentType("application/json")
                        .content("{ \"name\": \"Mars Base\", \"timezone\": \"Mars/Olympus_Mons\" }"))
                .andExpect(status().isBadRequest());
    }

    @Disabled("FINDINGS.md #7: UpdateLocationRequest has no validation, so an existing location can be "
            + "renamed to a blank name")
    @Test
    void updateRejectsBlankName() throws Exception {
        mockMvc.perform(put(BASE + "/3001").requestAttr("userId", MEMBER).contentType("application/json")
                        .content("{ \"name\": \"\" }"))
                .andExpect(status().isBadRequest());
    }

    @Disabled("FINDINGS.md #8: deleting a location that still has resources hits the FK constraint and "
            + "surfaces as an unhandled 500 instead of a 409 (or a defined cascade)")
    @Test
    void deletingLocationWithResourcesIsRejectedCleanly() throws Exception {
        jdbcTemplate.execute("""
                insert into resources (
                    id, organization_id, location_id, resource_type, name, capacity,
                    slot_unit_minutes, min_booking_minutes, max_booking_minutes, buffer_minutes,
                    base_price, currency, status, created_at, updated_at
                ) values (
                    3001, 3001, 3001, 'ROOM', 'Room A', 4, 60, 60, 180, 0,
                    10000, 'KRW', 'ACTIVE', current_timestamp, current_timestamp
                )
                """);

        mockMvc.perform(delete(BASE + "/3001").requestAttr("userId", MEMBER))
                .andExpect(status().isConflict());
    }
}
