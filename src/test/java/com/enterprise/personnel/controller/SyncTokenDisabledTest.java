package com.enterprise.personnel.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * fail-closed 验收：sync.api.token 未配置时，同步接口整体禁用（403），
 * 即使请求方携带任意 Token 也不放行（防止"空 Token 匹配空请求头"穿透）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:sync_disabled_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "sync.api.token="
})
class SyncTokenDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void tokenNotConfigured_syncDisabledWith403() throws Exception {
        mockMvc.perform(post("/api/sync/persons")
                        .header("X-Sync-Token", "any-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("同步接口未配置 Token，已禁用"));
    }
}
