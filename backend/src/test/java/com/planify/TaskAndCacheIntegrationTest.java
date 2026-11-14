package com.planify;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TaskAndCacheIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void taskCrudSearchFilterPaginationAndCacheInvalidation() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Task User","email":"task-user@planify.local","password":"Password123!"}
                                """))
                .andExpect(status().isCreated());

        MvcResult loginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"task-user@planify.local","password":"Password123!"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(loginRes.getResponse().getContentAsString()).get("accessToken").asText();

        MvcResult wsRes = mockMvc.perform(post("/api/v1/workspaces")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Product","description":"Main"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long wsId = objectMapper.readTree(wsRes.getResponse().getContentAsString()).get("id").asLong();

        MvcResult projRes = mockMvc.perform(post("/api/v1/workspaces/" + wsId + "/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Roadmap","description":"Q4"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long projId = objectMapper.readTree(projRes.getResponse().getContentAsString()).get("id").asLong();

        MvcResult taskRes = mockMvc.perform(post("/api/v1/projects/" + projId + "/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Implement Redis Caching","description":"Versioned cache invalidation","priority":"HIGH"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long taskId = objectMapper.readTree(taskRes.getResponse().getContentAsString()).get("id").asLong();

        // Populate cache
        mockMvc.perform(get("/api/v1/projects/" + projId + "/tasks?q=Redis")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("TODO"));

        // Update status -> invalidates cache
        mockMvc.perform(patch("/api/v1/tasks/" + taskId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DONE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        // Re-fetch reflects updated status
        mockMvc.perform(get("/api/v1/projects/" + projId + "/tasks?q=Redis")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("DONE"));
    }
}
