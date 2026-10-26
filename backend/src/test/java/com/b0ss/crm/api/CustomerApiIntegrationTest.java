package com.b0ss.crm.api;

import com.b0ss.crm.customer.Customer;
import com.b0ss.crm.customer.CustomerRepository;
import com.b0ss.crm.user.Role;
import com.b0ss.crm.user.User;
import com.b0ss.crm.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CustomerApiIntegrationTest {

    private static final String PASSWORD = "Passw0rd!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void loginReturnsTokenAndUser() throws Exception {
        createUser("it_admin", Role.ADMIN);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("it_admin", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("it_admin"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        createUser("it_admin", Role.ADMIN);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("it_admin", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"));
    }

    @Test
    void listWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listWithTokenReturnsPage() throws Exception {
        createUser("it_admin", Role.ADMIN);
        saveCustomer("Acme Ltd");
        String token = login("it_admin");

        mockMvc.perform(get("/api/customers")
                        .param("q", "acme")
                        .param("page", "0")
                        .param("size", "5")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Acme Ltd"));
    }

    @Test
    void createThenReadCustomer() throws Exception {
        createUser("it_admin", Role.ADMIN);
        String token = login("it_admin");

        MvcResult created = mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Globex HK","phone":"+852 9000 1234",
                                 "email":"hello@globex.example","company":"Globex","note":"created by test"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdBy").value("it_admin"))
                .andReturn();

        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/customers/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Globex HK"))
                .andExpect(jsonPath("$.company").value("Globex"));
    }

    @Test
    void createWithInvalidPayloadReturnsFieldErrors() throws Exception {
        createUser("it_admin", Role.ADMIN);
        String token = login("it_admin");

        mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.name").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.email").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "sales_user", roles = "SALES")
    void salesCannotDeleteCustomer() throws Exception {
        Customer customer = saveCustomer("Delete Me Ltd");

        mockMvc.perform(delete("/api/customers/" + customer.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin_user", roles = "ADMIN")
    void adminCanDeleteCustomer() throws Exception {
        Customer customer = saveCustomer("Delete Me Ltd");

        mockMvc.perform(delete("/api/customers/" + customer.getId()))
                .andExpect(status().isNoContent());

        assertThat(customerRepository.findById(customer.getId())).isEmpty();
    }

    @Test
    void addFollowUpThenListIt() throws Exception {
        createUser("it_admin", Role.ADMIN);
        String token = login("it_admin");
        Customer customer = saveCustomer("Follow Up Ltd");

        mockMvc.perform(post("/api/customers/" + customer.getId() + "/follow-ups")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Demo completed\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Demo completed"));

        mockMvc.perform(get("/api/customers/" + customer.getId() + "/follow-ups")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].content").value("Demo completed"));
    }

    @Test
    void reportSummaryIsAvailable() throws Exception {
        createUser("it_admin", Role.ADMIN);
        saveCustomer("Report Ltd");
        String token = login("it_admin");

        mockMvc.perform(get("/api/reports/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCustomers").isNumber());
    }

    private void createUser(String username, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setDisplayName("Integration User");
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        userRepository.save(user);
    }

    private Customer saveCustomer(String name) {
        Customer customer = new Customer();
        customer.setName(name);
        customer.setCompany("Test Co");
        return customerRepository.save(customer);
    }

    private String loginBody(String username, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of("username", username, "password", password));
    }

    private String login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(username, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }
}
