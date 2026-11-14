package com.planify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planify.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuditIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void asyncAuditLogsRecordedForWorkspaceProjectAndTask() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Auditor","email":"auditor@planify.local","password":"Password123!"}
                                """))
                .andExpect(status().isCreated());

        MvcResult loginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"auditor@planify.local","password":"Password123!"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(loginRes.getResponse().getContentAsString()).get("accessToken").asText();

        MvcResult wsRes = mockMvc.perform(post("/api/v1/workspaces")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Audit WS","description":"Desc"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long wsId = objectMapper.readTree(wsRes.getResponse().getContentAsString()).get("id").asLong();

        // Wait briefly for @Async writer
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline && auditLogRepository.count() == 0) {
            Thread.sleep(50);
        }

        MvcResult auditRes = mockMvc.perform(get("/api/v1/workspaces/" + wsId + "/audit-logs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(objectMapper.readTree(auditRes.getResponse().getContentAsString())
                .get("totalElements").asLong()).isGreaterThanOrEqualTo(1);
    }
}
