package com.makers.loans;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.makers.loans.application.ports.out.LoanRepositoryPort;
import com.makers.loans.domain.model.Loan;
import com.makers.loans.infrastructure.adapters.out.persistence.repository.LoanJpaRepository;
import com.makers.loans.infrastructure.adapters.out.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LoanAndUserFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private LoanJpaRepository loanJpaRepository;

    @Autowired
    private LoanRepositoryPort loanRepositoryPort;

    private String loginAndExtractToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.get("token").asText();
    }

    @Test
    @Order(1)
    void seedsOnlyTwoInitialUsersWithZeroLoansAndBlocksPublicRegistration() throws Exception {
        assertThat(userJpaRepository.count()).isEqualTo(2);
        assertThat(loanJpaRepository.count()).isZero();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\",\"email\":\"a@b.com\",\"password\":\"123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(2)
    void fullEndToEndAdminUsersLoansCacheIdorAndOptimisticLockFlow() throws Exception {
        String adminToken = loginAndExtractToken("admin@test.com", "123");
        String userToken = loginAndExtractToken("usuario@test.com", "123");

        // 1. Admin creates a second client and a second admin
        MvcResult createClientRes = mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cliente Dos\",\"email\":\"cliente2@test.com\",\"password\":\"123\",\"role\":\"USER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"))
                .andReturn();
        long client2Id = objectMapper.readTree(createClientRes.getResponse().getContentAsString()).get("id").asLong();

        MvcResult createTempUserRes = mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Admin Temporal\",\"email\":\"tempadmin@test.com\",\"password\":\"123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();
        long tempAdminId = objectMapper.readTree(createTempUserRes.getResponse().getContentAsString()).get("id").asLong();

        // Admin updates client2
        mockMvc.perform(put("/api/users/" + client2Id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cliente Dos Editado\",\"email\":\"cliente2@test.com\",\"role\":\"USER\",\"password\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cliente Dos Editado"));

        // Admin cannot demote or delete self
        long adminId = userJpaRepository.findByEmail("admin@test.com").orElseThrow().getId();
        mockMvc.perform(put("/api/users/" + adminId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Admin\",\"email\":\"admin@test.com\",\"role\":\"USER\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/users/" + adminId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isConflict());

        // 2. Client checks profile and requests two loans
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("usuario@test.com"));

        MvcResult loan1Res = mockMvc.perform(post("/api/loans")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1000,\"termMonths\":12}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        long loan1Id = objectMapper.readTree(loan1Res.getResponse().getContentAsString()).get("id").asLong();

        MvcResult loan2Res = mockMvc.perform(post("/api/loans")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":2000,\"termMonths\":24}"))
                .andExpect(status().isCreated())
                .andReturn();
        long loan2Id = objectMapper.readTree(loan2Res.getResponse().getContentAsString()).get("id").asLong();

        // 3. Populate Caffeine cache and verify entry exists
        mockMvc.perform(get("/api/loans/my")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        long user1Id = userJpaRepository.findByEmail("usuario@test.com").orElseThrow().getId();
        Cache loansCache = cacheManager.getCache("loans");
        assertThat(loansCache).isNotNull();
        assertThat(loansCache.get("user:" + user1Id)).isNotNull();

        // 4. Anti-IDOR: client2 cannot read or cancel user1's loan
        String client2Token = loginAndExtractToken("cliente2@test.com", "123");
        mockMvc.perform(get("/api/loans/" + loan1Id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + client2Token))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/loans/" + loan1Id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + client2Token))
                .andExpect(status().isForbidden());

        // 5. User1 cancels loan2 (logical delete PENDING -> CANCELLED) and cache is evicted
        mockMvc.perform(delete("/api/loans/" + loan2Id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(loansCache.get("user:" + user1Id)).isNull();

        // Cannot re-cancel or approve cancelled loan
        mockMvc.perform(delete("/api/loans/" + loan2Id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isConflict());
        mockMvc.perform(patch("/api/loans/" + loan2Id + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isConflict());

        // 6. Admin approves loan1; re-rejecting fails with 409
        mockMvc.perform(patch("/api/loans/" + loan1Id + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(patch("/api/loans/" + loan1Id + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isConflict());

        // 7. User with loans cannot be deleted; user without loans can be deleted
        mockMvc.perform(delete("/api/users/" + user1Id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/users/" + tempAdminId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // 8. Optimistic Locking (@Version) verification on stale Loan snapshot
        Loan freshLoan = Loan.request(client2Id, "cliente2@test.com", new BigDecimal("5000"), 10);
        Loan saved = loanRepositoryPort.save(freshLoan);
        Loan staleSnapshot = loanRepositoryPort.findById(saved.getId()).orElseThrow().copy();
        Loan concurrentUpdate = loanRepositoryPort.findById(saved.getId()).orElseThrow().copy();

        concurrentUpdate.approve();
        loanRepositoryPort.save(concurrentUpdate);

        staleSnapshot.reject();
        assertThatThrownBy(() -> loanRepositoryPort.save(staleSnapshot))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
