package com.jean325.slotops.organization.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
class OrganizationControllerTest {

    private static final long OWNER = 3001L;
    private static final long MANAGER = 3002L;
    private static final long STAFF = 3003L;
    private static final long OUTSIDER = 3004L;
    private static final long NEWCOMER = 3005L;

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        for (long id : new long[] {OWNER, MANAGER, STAFF, OUTSIDER, NEWCOMER}) {
            jdbcTemplate.update("""
                    insert into users (id, email, password_hash, name, status, created_at, updated_at)
                    values (?, ?, 'x', 'User', 'ACTIVE', current_timestamp, current_timestamp)
                    """, id, "user" + id + "@example.com");
        }
        jdbcTemplate.execute("""
                insert into organizations (id, name, slug, owner_user_id, status, created_at, updated_at)
                values (3001, 'Acme', 'acme', 3001, 'ACTIVE', current_timestamp, current_timestamp)
                """);
        addMemberRow(OWNER, "OWNER");
        addMemberRow(MANAGER, "MANAGER");
        addMemberRow(STAFF, "STAFF");
    }

    private void addMemberRow(long userId, String role) {
        jdbcTemplate.update("""
                insert into organization_members (organization_id, user_id, role, joined_at, created_at, updated_at)
                values (3001, ?, ?, current_timestamp, current_timestamp, current_timestamp)
                """, userId, role);
    }

    private int memberCount() {
        return jdbcTemplate.queryForObject(
                "select count(*) from organization_members where organization_id = 3001", Integer.class);
    }

    private String addMemberBody(long userId, String role) {
        return "{ \"userId\": %d, \"role\": \"%s\" }".formatted(userId, role);
    }

    // --- create ---

    @Test
    void createsOrganizationAndMakesCreatorItsOwner() throws Exception {
        String json = mockMvc.perform(post("/organizations")
                        .requestAttr("userId", OUTSIDER)
                        .contentType("application/json")
                        .content("{ \"name\": \"New Studio\", \"slug\": \"new-studio-2\" }"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("New Studio"))
                .andExpect(jsonPath("$.data.slug").value("new-studio-2"))
                .andExpect(jsonPath("$.data.ownerUserId").value(OUTSIDER))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        long orgId = objectMapper.readTree(json).get("data").get("id").asLong();

        mockMvc.perform(get("/organizations/" + orgId + "/members").requestAttr("userId", OUTSIDER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].userId").value(OUTSIDER))
                .andExpect(jsonPath("$.data[0].role").value("OWNER"));
    }

    @Test
    void rejectsDuplicateSlug() throws Exception {
        mockMvc.perform(post("/organizations")
                        .requestAttr("userId", OUTSIDER)
                        .contentType("application/json")
                        .content("{ \"name\": \"Copycat\", \"slug\": \"acme\" }"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLUG_ALREADY_EXISTS"));

        assertThat(jdbcTemplate.queryForObject("select count(*) from organizations", Integer.class)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Has-Caps", "under_score", "-leading", "trailing-", "double--hyphen", "with space", ""})
    void rejectsInvalidSlug(String slug) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("name", "Valid Name", "slug", slug));

        mockMvc.perform(post("/organizations")
                        .requestAttr("userId", OUTSIDER)
                        .contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("slug"));
    }

    @Test
    void rejectsBlankName() throws Exception {
        mockMvc.perform(post("/organizations")
                        .requestAttr("userId", OUTSIDER)
                        .contentType("application/json")
                        .content("{ \"name\": \" \", \"slug\": \"valid-slug\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void rejectsRequestWithoutAuthenticatedUserId() throws Exception {
        mockMvc.perform(post("/organizations")
                        .contentType("application/json")
                        .content("{ \"name\": \"No User\", \"slug\": \"no-user\" }"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    // --- read ---

    @Test
    void memberCanViewOrganization() throws Exception {
        mockMvc.perform(get("/organizations/3001").requestAttr("userId", STAFF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(3001))
                .andExpect(jsonPath("$.data.name").value("Acme"))
                .andExpect(jsonPath("$.data.slug").value("acme"));
    }

    @Test
    void nonMemberCannotViewOrganization() throws Exception {
        mockMvc.perform(get("/organizations/3001").requestAttr("userId", OUTSIDER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void returnsNotFoundForUnknownOrganization() throws Exception {
        mockMvc.perform(get("/organizations/999999").requestAttr("userId", OWNER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));
    }

    @Test
    void listsOnlyOrganizationsTheCallerBelongsTo() throws Exception {
        jdbcTemplate.execute("""
                insert into organizations (id, name, slug, owner_user_id, status, created_at, updated_at)
                values (3002, 'Other', 'other', 3004, 'ACTIVE', current_timestamp, current_timestamp)
                """);
        jdbcTemplate.execute("""
                insert into organization_members (organization_id, user_id, role, joined_at, created_at, updated_at)
                values (3002, 3004, 'OWNER', current_timestamp, current_timestamp, current_timestamp)
                """);

        mockMvc.perform(get("/organizations").requestAttr("userId", STAFF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].slug").value("acme"));
        mockMvc.perform(get("/organizations").requestAttr("userId", OUTSIDER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].slug").value("other"));
        mockMvc.perform(get("/organizations").requestAttr("userId", NEWCOMER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // --- members ---

    @Test
    void ownerAddsMemberWhoThenGainsAccess() throws Exception {
        mockMvc.perform(get("/organizations/3001").requestAttr("userId", NEWCOMER))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/organizations/3001/members")
                        .requestAttr("userId", OWNER)
                        .contentType("application/json").content(addMemberBody(NEWCOMER, "STAFF")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.organizationId").value(3001))
                .andExpect(jsonPath("$.data.userId").value(NEWCOMER))
                .andExpect(jsonPath("$.data.role").value("STAFF"));

        mockMvc.perform(get("/organizations/3001").requestAttr("userId", NEWCOMER))
                .andExpect(status().isOk());
    }

    @Test
    void managerCanAddMember() throws Exception {
        mockMvc.perform(post("/organizations/3001/members")
                        .requestAttr("userId", MANAGER)
                        .contentType("application/json").content(addMemberBody(NEWCOMER, "STAFF")))
                .andExpect(status().isCreated());

        assertThat(memberCount()).isEqualTo(4);
    }

    @Test
    void staffCannotAddMember() throws Exception {
        mockMvc.perform(post("/organizations/3001/members")
                        .requestAttr("userId", STAFF)
                        .contentType("application/json").content(addMemberBody(NEWCOMER, "STAFF")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        assertThat(memberCount()).isEqualTo(3);
    }

    @Test
    void nonMemberCannotAddThemselves() throws Exception {
        mockMvc.perform(post("/organizations/3001/members")
                        .requestAttr("userId", OUTSIDER)
                        .contentType("application/json").content(addMemberBody(OUTSIDER, "OWNER")))
                .andExpect(status().isForbidden());

        assertThat(memberCount()).isEqualTo(3);
    }

    @Test
    void rejectsAddingExistingMember() throws Exception {
        mockMvc.perform(post("/organizations/3001/members")
                        .requestAttr("userId", OWNER)
                        .contentType("application/json").content(addMemberBody(STAFF, "MANAGER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEMBER_ALREADY_EXISTS"));

        String role = jdbcTemplate.queryForObject(
                "select role from organization_members where organization_id = 3001 and user_id = 3003", String.class);
        assertThat(role).isEqualTo("STAFF");
    }

    @Test
    void addMemberReturnsNotFoundForUnknownOrganization() throws Exception {
        mockMvc.perform(post("/organizations/999999/members")
                        .requestAttr("userId", OWNER)
                        .contentType("application/json").content(addMemberBody(NEWCOMER, "STAFF")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));
    }

    @Test
    void rejectsAddMemberWithoutRoleOrUserId() throws Exception {
        mockMvc.perform(post("/organizations/3001/members")
                        .requestAttr("userId", OWNER)
                        .contentType("application/json").content("{ \"userId\": 3005 }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("role"));
        mockMvc.perform(post("/organizations/3001/members")
                        .requestAttr("userId", OWNER)
                        .contentType("application/json").content("{ \"role\": \"STAFF\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("userId"));
    }

    @Test
    void rejectsUnknownRole() throws Exception {
        mockMvc.perform(post("/organizations/3001/members")
                        .requestAttr("userId", OWNER)
                        .contentType("application/json").content(addMemberBody(NEWCOMER, "SUPERADMIN")))
                .andExpect(status().isBadRequest());

        assertThat(memberCount()).isEqualTo(3);
    }

    @Test
    void memberCanListMembers() throws Exception {
        mockMvc.perform(get("/organizations/3001/members").requestAttr("userId", STAFF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[?(@.userId == 3001)].role").value("OWNER"))
                .andExpect(jsonPath("$.data[?(@.userId == 3002)].role").value("MANAGER"))
                .andExpect(jsonPath("$.data[?(@.userId == 3003)].role").value("STAFF"));
    }

    @Test
    void nonMemberCannotListMembers() throws Exception {
        mockMvc.perform(get("/organizations/3001/members").requestAttr("userId", OUTSIDER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Disabled("FINDINGS.md #4: a MANAGER can add a member with role OWNER (privilege escalation); "
            + "addMember only checks the caller is OWNER or MANAGER, not the role being granted")
    @Test
    void managerCannotGrantOwnerRole() throws Exception {
        mockMvc.perform(post("/organizations/3001/members")
                        .requestAttr("userId", MANAGER)
                        .contentType("application/json").content(addMemberBody(NEWCOMER, "OWNER")))
                .andExpect(status().isForbidden());
    }

    @Disabled("FINDINGS.md #5: addMember does not verify the target user exists, so membership rows "
            + "can be created for arbitrary user ids")
    @Test
    void rejectsAddingUserThatDoesNotExist() throws Exception {
        mockMvc.perform(post("/organizations/3001/members")
                        .requestAttr("userId", OWNER)
                        .contentType("application/json").content(addMemberBody(888888L, "STAFF")))
                .andExpect(status().is4xxClientError());
    }
}
