package com.rohobie.billing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    private String basicAuth(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Unauthenticated request to POST /api/billing/run returns 401 Unauthorized")
    void billingRun_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/billing/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleId\": 1, \"billingMonth\": \"2026-01\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Finance user calling POST /api/billing/run returns 403 Forbidden")
    void billingRun_financeRole_returns403() throws Exception {
        mockMvc.perform(post("/api/billing/run")
                        .header("Authorization", basicAuth("finance", "finance123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleId\": 1, \"billingMonth\": \"2026-01\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin user calling POST /api/billing/run is authorized (passes security filter)")
    void billingRun_adminRole_isAuthorized() throws Exception {
        // Sends empty body to verify security passes and controller validation rejects with 400 Bad Request
        mockMvc.perform(post("/api/billing/run")
                        .header("Authorization", basicAuth("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Finance user can access GET /api/billing/run endpoints (returns 404 for unknown run)")
    void billingGet_financeRole_isAuthorized() throws Exception {
        mockMvc.perform(get("/api/billing/run/99999")
                        .header("Authorization", basicAuth("finance", "finance123")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Finance user cannot POST to master data endpoints (returns 403 Forbidden)")
    void postMasterData_financeRole_returns403() throws Exception {
        mockMvc.perform(post("/api/vendors")
                        .header("Authorization", basicAuth("finance", "finance123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Unauthorized Vendor\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin user can POST to master data endpoints")
    void postMasterData_adminRole_isAuthorized() throws Exception {
        mockMvc.perform(post("/api/vendors")
                        .header("Authorization", basicAuth("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Authorized Admin Vendor\", \"contactEmail\": \"admin@vendor.com\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("H2 console path is accessible without credentials (does not return 401 or 403)")
    void h2Console_unauthenticated_isPermitted() throws Exception {
        mockMvc.perform(get("/h2-console"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status)
                            .isNotIn(401, 403);
                });
    }

    @Test
    @DisplayName("Static assets and Actuator health are accessible without credentials")
    void staticAssetsAndHealth_unauthenticated_isPermitted() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CORS pre-flight OPTIONS request returns 200 with appropriate CORS headers")
    void corsPreflight_returnsOkWithHeaders() throws Exception {
        mockMvc.perform(options("/api/monitoring/stats")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
}
