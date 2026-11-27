package com.jean325.slotops.resource.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
class ResourceControllerTest {

    private static final long MEMBER = 3001L;
    private static final long OUTSIDER = 3002L;
    private static final String BASE = "/organizations/3001/locations/3001/resources";
    private static final String VALID = """
            { "resourceType": "STUDIO", "name": "Studio B", "description": "Corner studio", "capacity": 6,
              "slotUnitMinutes": 30, "minBookingMinutes": 60, "maxBookingMinutes": 240, "bufferMinutes": 15,
              "basePrice": 25000.00, "currency": "KRW" }
            """;

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
                insert into locations (id, organization_id, name, timezone, status, created_at, updated_at) values
                    (3001, 3001, 'Gangnam', 'Asia/Seoul', 'ACTIVE', current_timestamp, current_timestamp),
                    (3003, 3001, 'Hongdae', 'Asia/Seoul', 'ACTIVE', current_timestamp, current_timestamp),
                    (3002, 3002, 'Their Place', 'Asia/Seoul', 'ACTIVE', current_timestamp, current_timestamp)
                """);
        // 3001: in Gangnam, 3003: in Hongdae (same org), 3002: other org.
        seedResource(3001, 3001, 3001, "Room A");
        seedResource(3003, 3001, 3003, "Hongdae Room");
        seedResource(3002, 3002, 3002, "Their Room");
    }

    private void seedResource(long id, long orgId, long locationId, String name) {
        jdbcTemplate.update("""
                insert into resources (
                    id, organization_id, location_id, resource_type, name, description, capacity,
                    slot_unit_minutes, min_booking_minutes, max_booking_minutes, buffer_minutes,
                    base_price, currency, status, created_at, updated_at
                ) values (
                    ?, ?, ?, 'ROOM', ?, 'Original description', 4, 60, 60, 180, 10,
                    10000, 'KRW', 'ACTIVE', current_timestamp, current_timestamp
                )
                """, id, orgId, locationId, name);
    }

    private long createResource(String body) throws Exception {
        String json = mockMvc.perform(post(BASE).requestAttr("userId", MEMBER)
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("data").get("id").asLong();
    }

    // --- create ---

    @Test
    void createsResourceWithBookingConfiguration() throws Exception {
        mockMvc.perform(post(BASE).requestAttr("userId", MEMBER).contentType("application/json").content(VALID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.organizationId").value(3001))
                .andExpect(jsonPath("$.data.locationId").value(3001))
                .andExpect(jsonPath("$.data.resourceType").value("STUDIO"))
                .andExpect(jsonPath("$.data.name").value("Studio B"))
                .andExpect(jsonPath("$.data.description").value("Corner studio"))
                .andExpect(jsonPath("$.data.capacity").value(6))
                .andExpect(jsonPath("$.data.slotUnitMinutes").value(30))
                .andExpect(jsonPath("$.data.minBookingMinutes").value(60))
                .andExpect(jsonPath("$.data.maxBookingMinutes").value(240))
                .andExpect(jsonPath("$.data.bufferMinutes").value(15))
                .andExpect(jsonPath("$.data.basePrice").value(25000.0))
                .andExpect(jsonPath("$.data.currency").value("KRW"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.imageUrls.length()").value(0));
    }

    @Test
    void createsResourceWithImagesInGivenOrder() throws Exception {
        ObjectNode body = (ObjectNode) objectMapper.readTree(VALID);
        body.putArray("imageUrls")
                .add("https://img.example.com/z-first.jpg")
                .add("https://img.example.com/a-second.jpg")
                .add("https://img.example.com/m-third.jpg");

        long id = createResource(body.toString());

        mockMvc.perform(get(BASE + "/" + id).requestAttr("userId", MEMBER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imageUrls.length()").value(3))
                .andExpect(jsonPath("$.data.imageUrls[0]").value("https://img.example.com/z-first.jpg"))
                .andExpect(jsonPath("$.data.imageUrls[1]").value("https://img.example.com/a-second.jpg"))
                .andExpect(jsonPath("$.data.imageUrls[2]").value("https://img.example.com/m-third.jpg"));
    }

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(nullValues = "null", value = {
            "resourceType, null",
            "name, ''",
            "capacity, 0",
            "capacity, null",
            "slotUnitMinutes, 14",
            "minBookingMinutes, 0",
            "maxBookingMinutes, 14",
            "bufferMinutes, -1",
            "basePrice, 0",
            "basePrice, -100",
            "currency, ''",
    })
    void rejectsInvalidField(String field, String value) throws Exception {
        ObjectNode body = (ObjectNode) objectMapper.readTree(VALID);
        if (value == null) {
            body.putNull(field);
        } else if (value.isEmpty() || !value.matches("-?\\d+")) {
            body.put(field, value);
        } else {
            body.put(field, Integer.parseInt(value));
        }

        mockMvc.perform(post(BASE).requestAttr("userId", MEMBER)
                        .contentType("application/json").content(body.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value(field));

        Integer count = jdbcTemplate.queryForObject("select count(*) from resources", Integer.class);
        assertThat(count).isEqualTo(3);
    }

    @Test
    void rejectsUnknownResourceType() throws Exception {
        ObjectNode body = (ObjectNode) objectMapper.readTree(VALID);
        body.put("resourceType", "SPACESHIP");

        mockMvc.perform(post(BASE).requestAttr("userId", MEMBER)
                        .contentType("application/json").content(body.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReturnsNotFoundForUnknownLocation() throws Exception {
        mockMvc.perform(post("/organizations/3001/locations/999999/resources").requestAttr("userId", MEMBER)
                        .contentType("application/json").content(VALID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LOCATION_NOT_FOUND"));
    }

    @Test
    void cannotCreateResourceInAnotherOrganizationsLocation() throws Exception {
        mockMvc.perform(post("/organizations/3001/locations/3002/resources").requestAttr("userId", MEMBER)
                        .contentType("application/json").content(VALID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LOCATION_NOT_FOUND"));

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from resources where location_id = 3002", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    // --- read ---

    @Test
    void getsResource() throws Exception {
        mockMvc.perform(get(BASE + "/3001").requestAttr("userId", MEMBER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(3001))
                .andExpect(jsonPath("$.data.name").value("Room A"))
                .andExpect(jsonPath("$.data.locationId").value(3001));
    }

    @Test
    void returnsNotFoundForUnknownResource() throws Exception {
        mockMvc.perform(get(BASE + "/999999").requestAttr("userId", MEMBER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void returnsNotFoundWhenResourceBelongsToDifferentLocation() throws Exception {
        mockMvc.perform(get(BASE + "/3003").requestAttr("userId", MEMBER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void listsOnlyResourcesOfTheLocation() throws Exception {
        mockMvc.perform(get(BASE).requestAttr("userId", MEMBER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Room A"));
    }

    @Test
    void listReturnsNotFoundForLocationOfAnotherOrganization() throws Exception {
        mockMvc.perform(get("/organizations/3001/locations/3002/resources").requestAttr("userId", MEMBER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LOCATION_NOT_FOUND"));
    }

    // --- update ---

    @Test
    void updateChangesOnlyProvidedFields() throws Exception {
        mockMvc.perform(put(BASE + "/3001").requestAttr("userId", MEMBER).contentType("application/json")
                        .content("{ \"name\": \"Room A+\", \"basePrice\": 12000, \"status\": \"HIDDEN\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Room A+"))
                .andExpect(jsonPath("$.data.basePrice").value(12000))
                .andExpect(jsonPath("$.data.status").value("HIDDEN"));

        mockMvc.perform(get(BASE + "/3001").requestAttr("userId", MEMBER))
                .andExpect(jsonPath("$.data.name").value("Room A+"))
                .andExpect(jsonPath("$.data.status").value("HIDDEN"))
                .andExpect(jsonPath("$.data.description").value("Original description"))
                .andExpect(jsonPath("$.data.capacity").value(4))
                .andExpect(jsonPath("$.data.slotUnitMinutes").value(60))
                .andExpect(jsonPath("$.data.bufferMinutes").value(10));
    }

    @Test
    void updateReturnsNotFoundForUnknownResource() throws Exception {
        mockMvc.perform(put(BASE + "/999999").requestAttr("userId", MEMBER).contentType("application/json")
                        .content("{ \"name\": \"Ghost\" }"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    // --- delete ---

    @Test
    void deletesResourceTogetherWithItsImages() throws Exception {
        ObjectNode body = (ObjectNode) objectMapper.readTree(VALID);
        body.putArray("imageUrls").add("https://img.example.com/1.jpg").add("https://img.example.com/2.jpg");
        long id = createResource(body.toString());

        mockMvc.perform(delete(BASE + "/" + id).requestAttr("userId", MEMBER))
                .andExpect(status().isOk());

        mockMvc.perform(get(BASE + "/" + id).requestAttr("userId", MEMBER))
                .andExpect(status().isNotFound());
        Integer images = jdbcTemplate.queryForObject(
                "select count(*) from resource_images where resource_id = ?", Integer.class, id);
        assertThat(images).isZero();
    }

    @Test
    void deleteReturnsNotFoundForUnknownResource() throws Exception {
        mockMvc.perform(delete(BASE + "/999999").requestAttr("userId", MEMBER))
                .andExpect(status().isNotFound());
    }

    // --- access ---

    @Test
    void rejectsNonMember() throws Exception {
        mockMvc.perform(get(BASE).requestAttr("userId", OUTSIDER))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE).requestAttr("userId", OUTSIDER).contentType("application/json").content(VALID))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(BASE + "/3001").requestAttr("userId", OUTSIDER))
                .andExpect(status().isForbidden());

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from resources where location_id = 3001", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    // --- known gaps ---

    @Disabled("FINDINGS.md #9: no cross-field validation, so a resource whose minimum booking is longer "
            + "than its maximum is accepted and can then never be booked")
    @Test
    void rejectsMinBookingLongerThanMaxBooking() throws Exception {
        ObjectNode body = (ObjectNode) objectMapper.readTree(VALID);
        body.put("minBookingMinutes", 240);
        body.put("maxBookingMinutes", 60);

        mockMvc.perform(post(BASE).requestAttr("userId", MEMBER)
                        .contentType("application/json").content(body.toString()))
                .andExpect(status().isBadRequest());
    }

    @Disabled("FINDINGS.md #7: UpdateResourceRequest has no validation, so capacity 0 or a negative "
            + "price can be written through PUT even though POST rejects them")
    @Test
    void updateRejectsInvalidValues() throws Exception {
        mockMvc.perform(put(BASE + "/3001").requestAttr("userId", MEMBER).contentType("application/json")
                        .content("{ \"capacity\": 0, \"basePrice\": -1 }"))
                .andExpect(status().isBadRequest());
    }

    @Disabled("FINDINGS.md #8: deleting a resource that has reservations (or availability rules) hits "
            + "the FK constraint and surfaces as an unhandled 500 instead of a 409")
    @Test
    void deletingResourceWithReservationsIsRejectedCleanly() throws Exception {
        jdbcTemplate.execute("""
                insert into customers (id, organization_id, name, email, phone, created_at, updated_at)
                values (3001, 3001, 'Kim', 'kim@example.com', '010-1234-5678', current_timestamp, current_timestamp)
                """);
        jdbcTemplate.execute("""
                insert into reservations (
                    id, organization_id, location_id, resource_id, customer_id, reservation_code, status,
                    start_at, end_at, guest_count, base_amount, addon_amount, discount_amount, total_amount,
                    created_at, updated_at
                ) values (
                    3001, 3001, 3001, 3001, 3001, 'RSV-3001', 'CONFIRMED',
                    TIMESTAMP WITH TIME ZONE '2026-06-15 10:00:00+09:00',
                    TIMESTAMP WITH TIME ZONE '2026-06-15 12:00:00+09:00',
                    2, 20000.00, 0, 0, 20000.00, current_timestamp, current_timestamp
                )
                """);

        mockMvc.perform(delete(BASE + "/3001").requestAttr("userId", MEMBER))
                .andExpect(status().isConflict());
    }
}
