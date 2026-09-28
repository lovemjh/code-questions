package com.enterprise.personnel;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * 上下文装配验证：使用独立内存库，
 * 避免与正在运行的开发服务（占用 ./data H2 文件锁）互相冲突。
 */
@SpringBootTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:context_test;MODE=MySQL;DB_CLOSE_DELAY=-1")
class PersonnelApplicationTests {

    @Test
    void contextLoads() {
    }
}
