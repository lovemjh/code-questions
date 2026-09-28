package com.enterprise.personnel.controller;

import com.enterprise.personnel.common.BusinessException;
import com.enterprise.personnel.dto.PersonSyncItem;
import com.enterprise.personnel.entity.Person;
import com.enterprise.personnel.entity.Tag;
import com.enterprise.personnel.mapper.SyncLogMapper;
import com.enterprise.personnel.service.PersonService;
import com.enterprise.personnel.service.TagService;
import com.enterprise.personnel.vo.PersonTreeNode;
import com.enterprise.personnel.vo.PersonVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 外部人员同步接口验收：《人员同步接口设计方案.md》第十节核心场景。
 * 使用独立内存库与固定业务 Token，不污染本地 ./data 开发库；
 * 人员 id 使用 990xxx 段，避免与 data.sql 种子数据冲突。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:sync_api_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "sync.api.token=test-sync-token"
})
class SyncApiTest {

    private static final String TOKEN = "test-sync-token";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PersonService personService;
    @Autowired
    private TagService tagService;
    @Autowired
    private SyncLogMapper syncLogMapper;

    // ---------- 鉴权 ----------

    @Test
    void missingOrWrongToken_rejectedWith401() throws Exception {
        mockMvc.perform(post("/api/sync/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(post("/api/sync/persons")
                        .header("X-Sync-Token", "wrong-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void emptyList_rejected() throws Exception {
        mockMvc.perform(post("/api/sync/persons")
                        .header("X-Sync-Token", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("同步列表为空"));
    }

    // ---------- 新增 + 幂等 ----------

    @Test
    void createThenRepublish_idempotent_noDirtyData() throws Exception {
        Map<String, Object> item = item(990001L, "同步新增", "E99001", null);
        for (int i = 0; i < 2; i++) {
            syncOk(List.of(item))
                    .andExpect(jsonPath("$.data.total").value(1))
                    .andExpect(jsonPath("$.data.success").value(1))
                    .andExpect(jsonPath("$.data.fail").value(0));
        }
        Person person = personService.getById(990001L);
        assertNotNull(person);
        assertNull(person.getSuperiorId());
        assertFalse(person.getResigned());
        assertTrue(personService.getDetail(990001L).getTags().isEmpty());
    }

    // ---------- 更新隔离：superior 与业务标签保留，null 忽略 ----------

    @Test
    void update_preservesSuperiorAndTags_nullIgnored() throws Exception {
        Map<String, Object> first = item(990011L, "更新隔离", "E99011", null);
        first.put("email", "before@test.com");
        syncOk(List.of(first));

        Tag businessTag = tagService.create("同步测试标签");
        personService.updateSuperior(990011L, 1001L);
        personService.bindTags(990011L, List.of(businessTag.getId()));

        // email 推 null = 忽略不清空；resigned 不传 = 忽略
        Map<String, Object> second = item(990011L, "更新隔离-新名", "E99011", null);
        second.put("email", null);
        syncOk(List.of(second));

        PersonVO vo = personService.getDetail(990011L);
        assertEquals("更新隔离-新名", vo.getName());
        assertEquals(Long.valueOf(1001L), vo.getSuperiorId());
        assertEquals(1, vo.getTags().size());
        assertEquals(businessTag.getId(), vo.getTags().get(0).getId());

        Person person = personService.getById(990011L);
        assertEquals("before@test.com", person.getEmail());
        assertFalse(person.getResigned());
    }

    // ---------- 离职联动 + 树过滤 ----------

    @Test
    void resigned_bindsTag_hiddenFromTree_childrenPromoted() throws Exception {
        syncOk(List.of(item(990021L, "离职主管", "E99021", null)));
        syncOk(List.of(item(990022L, "在职下属", "E99022", null)));
        personService.updateSuperior(990022L, 990021L);

        syncOk(List.of(item(990021L, "离职主管", "E99021", true)));

        assertTrue(personService.getById(990021L).getResigned());
        assertEquals(1, personService.getDetail(990021L).getTags().size());
        assertEquals("离职", personService.getDetail(990021L).getTags().get(0).getName());

        List<PersonTreeNode> tree = personService.getTree();
        assertTrue(tree.stream().noneMatch(n -> n.getId() == 990021L));
        // 离职主管的下属提升为顶级节点
        assertTrue(tree.stream().anyMatch(n -> n.getId() == 990022L));

        // 复职：标签自动解绑、恢复树内展示
        syncOk(List.of(item(990021L, "离职主管", "E99021", false)));
        assertFalse(personService.getById(990021L).getResigned());
        assertTrue(personService.getDetail(990021L).getTags().isEmpty());
        List<PersonTreeNode> treeAfter = personService.getTree();
        assertTrue(treeAfter.stream().anyMatch(n -> n.getId() == 990021L));
        assertTrue(treeAfter.stream().noneMatch(n -> n.getId() == 990022L));
    }

    // ---------- 系统标签保护 ----------

    @Test
    void bindTags_rejectsResignedSystemTag() throws Exception {
        syncOk(List.of(item(990031L, "绑定保护", "E99031", null)));
        Tag resigned = tagService.lambdaQuery().eq(Tag::getName, "离职").one();
        assertNotNull(resigned);
        assertTrue(resigned.getIsSystem());

        BusinessException e = assertThrows(BusinessException.class,
                () -> personService.bindTags(990031L, List.of(resigned.getId())));
        assertTrue(e.getMessage().contains("离职"));
    }

    @Test
    void systemTag_deleteAndRenameRejected() {
        Tag resigned = tagService.lambdaQuery().eq(Tag::getName, "离职").one();

        BusinessException deleteError = assertThrows(BusinessException.class,
                () -> tagService.delete(resigned.getId()));
        assertTrue(deleteError.getMessage().contains("不可删除"));

        BusinessException renameError = assertThrows(BusinessException.class,
                () -> tagService.rename(resigned.getId(), "已离职"));
        assertTrue(renameError.getMessage().contains("不可重命名"));
    }

    // ---------- 批量部分失败 + 工号冲突 ----------

    @Test
    void batch_partialFailure_collectedInErrors() throws Exception {
        Map<String, Object> valid = item(990041L, "批量成功", "E99041", null);
        Map<String, Object> blankName = item(990042L, "", "E99042", null);
        // E1001 已被种子人员 1001 占用
        Map<String, Object> employeeNoConflict = item(990043L, "工号冲突", "E1001", null);

        long auditBefore = syncLogMapper.selectCount(null);

        syncOk(List.of(valid, blankName, employeeNoConflict))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.success").value(1))
                .andExpect(jsonPath("$.data.fail").value(2))
                .andExpect(jsonPath("$.data.errors.length()").value(2))
                .andExpect(jsonPath("$.data.errors[0].message").value(containsString("name")))
                .andExpect(jsonPath("$.data.errors[1].message").value(containsString("工号")));

        // 每条同步（无论成败）各写一行审计记录
        long auditAfter = syncLogMapper.selectCount(null);
        assertEquals(auditBefore + 3, auditAfter);

        assertNotNull(personService.getById(990041L));
        assertNull(personService.getById(990042L));
        assertNull(personService.getById(990043L));
    }

    // ---------- 并发幂等（覆盖 4.2 撞主键降级为更新的兜底分支） ----------

    @Test
    void concurrentPush_sameId_noDirtyData() throws Exception {
        for (int round = 0; round < 3; round++) {
            long id = 990050L + round;
            int threads = 4;
            CyclicBarrier barrier = new CyclicBarrier(threads);
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            List<Future<String>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                futures.add(pool.submit(() -> {
                    PersonSyncItem item = new PersonSyncItem();
                    item.setId(id);
                    item.setName("并发-" + id);
                    item.setEmployeeNo("E" + id);
                    item.setResigned(false);
                    barrier.await(5, TimeUnit.SECONDS);
                    return personService.upsertFromSync(item);
                }));
            }

            List<String> actions = new ArrayList<>();
            for (Future<String> future : futures) {
                // 兜底分支若有缺陷，会以异常形式在这里暴露
                actions.add(future.get(10, TimeUnit.SECONDS));
            }
            pool.shutdown();

            // 主键唯一：恰好一次 CREATE，其余降级为 UPDATE
            assertEquals(1, actions.stream().filter("CREATE"::equals).count());
            Person person = personService.getById(id);
            assertNotNull(person);
            assertEquals("并发-" + id, person.getName());
            assertFalse(person.getResigned());
        }
    }

    // ---------- 参数校验与批量上限 ----------

    @Test
    void invalidId_reportedInErrors() throws Exception {
        Map<String, Object> negativeId = item(-1L, "非法ID", "E99061", null);
        Map<String, Object> nullId = new HashMap<>();
        nullId.put("name", "缺失ID");
        nullId.put("employeeNo", "E99062");

        syncOk(List.of(negativeId, nullId))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.success").value(0))
                .andExpect(jsonPath("$.data.fail").value(2))
                .andExpect(jsonPath("$.data.errors[0].message").value(containsString("id")))
                .andExpect(jsonPath("$.data.errors[1].message").value(containsString("id")));

        assertNull(personService.getById(-1L));
    }

    @Test
    void oversizeBatch_rejected() throws Exception {
        List<Map<String, Object>> items = new ArrayList<>();
        for (long i = 1; i <= 501; i++) {
            items.add(item(i, "批量超限", "EOVER" + i, null));
        }
        mockMvc.perform(post("/api/sync/persons")
                        .header("X-Sync-Token", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(items)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("单批不能超过 500 条"));
    }

    // ---------- 工具 ----------

    private ResultActions syncOk(List<Map<String, Object>> items) throws Exception {
        return mockMvc.perform(post("/api/sync/persons")
                        .header("X-Sync-Token", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(items)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private Map<String, Object> item(long id, String name, String employeeNo, Boolean resigned) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("employeeNo", employeeNo);
        if (resigned != null) {
            m.put("resigned", resigned);
        }
        return m;
    }
}
