package com.planify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PermissionMatrixIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndLogin(String name, String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","email":"%s","password":"Password123!"}
                                """.formatted(name, email)))
                .andExpect(status().isCreated());

        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Password123!"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test
    void workspaceMemberVisibilityAndRoleMatrix() throws Exception {
        String ownerToken = registerAndLogin("Owner", "owner-matrix@planify.local");
        String memberToken = registerAndLogin("Member", "member-matrix@planify.local");

        // Owner creates workspace
        MvcResult wsRes = mockMvc.perform(post("/api/v1/workspaces")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Engineering","description":"Core"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long wsId = objectMapper.readTree(wsRes.getResponse().getContentAsString()).get("id").asLong();

        // Owner adds Member as MEMBER
        MvcResult addRes = mockMvc.perform(post("/api/v1/workspaces/" + wsId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"member-matrix@planify.local","role":"MEMBER"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long memberUserId = objectMapper.readTree(addRes.getResponse().getContentAsString()).get("userId").asLong();

        // Owner creates two projects: P1 and P2
        MvcResult p1Res = mockMvc.perform(post("/api/v1/workspaces/" + wsId + "/projects")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Secret Project","description":"Hidden"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long p1Id = objectMapper.readTree(p1Res.getResponse().getContentAsString()).get("id").asLong();

        MvcResult p2Res = mockMvc.perform(post("/api/v1/workspaces/" + wsId + "/projects")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Shared Project","description":"Visible"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long p2Id = objectMapper.readTree(p2Res.getResponse().getContentAsString()).get("id").asLong();

        // Add Member to P2 as VIEWER
        mockMvc.perform(post("/api/v1/projects/" + p2Id + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d,"role":"VIEWER"}
                                """.formatted(memberUserId)))
                .andExpect(status().isCreated());

        // WS MEMBER sees only Shared Project (P2), not Secret Project (P1)
        MvcResult listRes = mockMvc.perform(get("/api/v1/workspaces/" + wsId + "/projects")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode listJson = objectMapper.readTree(listRes.getResponse().getContentAsString());
        assertThat(listJson.size()).isEqualTo(1);
        assertThat(listJson.get(0).get("id").asLong()).isEqualTo(p2Id);

        // VIEWER cannot create tasks in P2
        mockMvc.perform(post("/api/v1/projects/" + p2Id + "/tasks")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Blocked Task","priority":"MEDIUM"}
                                """))
                .andExpect(status().isForbidden());

        // WS MEMBER cannot delete workspace or view workspace audit logs
        mockMvc.perform(delete("/api/v1/workspaces/" + wsId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/workspaces/" + wsId + "/audit-logs")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }
}
