package com.b0ss.crm.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression guard: the Vite dev server is reachable both as localhost and 127.0.0.1,
 * and Spring rejects any request whose Origin is not allow-listed with a bare 403.
 * That once made the login form fail with "Request failed with status code 403".
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowsBothDevOriginsOnSimpleRequest() throws Exception {
        for (String origin : new String[] {"http://localhost:5173", "http://127.0.0.1:5173"}) {
            mockMvc.perform(get("/api/customers").header("Origin", origin))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string("Access-Control-Allow-Origin", origin));
        }
    }

    @Test
    void allowsBothDevOriginsOnPreflight() throws Exception {
        for (String origin : new String[] {"http://localhost:5173", "http://127.0.0.1:5173"}) {
            mockMvc.perform(options("/api/customers")
                            .header("Origin", origin)
                            .header("Access-Control-Request-Method", "GET"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", origin));
        }
    }

    @Test
    void rejectsUnknownOrigin() throws Exception {
        mockMvc.perform(get("/api/customers").header("Origin", "http://evil.example"))
                .andExpect(status().isForbidden());
    }
}
