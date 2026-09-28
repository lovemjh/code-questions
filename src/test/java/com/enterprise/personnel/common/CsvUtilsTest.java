package com.enterprise.personnel.common;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvUtilsTest {

    @Test
    void parsePlainRows() {
        List<List<String>> rows = CsvUtils.parse("ID,标签名称\r\n1,核心成员\r\n2,骨干\r\n");
        assertEquals(4, rows.size());
        assertEquals(List.of("ID", "标签名称"), rows.get(0));
        assertEquals(List.of("1", "核心成员"), rows.get(1));
        assertEquals(List.of("2", "骨干"), rows.get(2));
        // 末尾换行产生一个全空行
        assertTrue(CsvUtils.isBlankRow(rows.get(3)));
    }

    @Test
    void parseQuotedFields() {
        List<List<String>> rows = CsvUtils.parse("1,\"含,逗号的标签\"\n2,\"带\"\"引号\"\"的标签\"\n3,\"多\n行标签\"");
        assertEquals("含,逗号的标签", rows.get(0).get(1));
        assertEquals("带\"引号\"的标签", rows.get(1).get(1));
        assertEquals("多\n行标签", rows.get(2).get(1));
    }

    @Test
    void escapeFields() {
        assertEquals("普通标签", CsvUtils.escape("普通标签"));
        assertEquals("\"含,逗号\"", CsvUtils.escape("含,逗号"));
        assertEquals("\"含\"\"引号\"", CsvUtils.escape("含\"引号"));
        assertEquals("", CsvUtils.escape(null));
        // 转义后可被 parse 还原
        String origin = "a,\"b\"c,d";
        assertEquals(origin, CsvUtils.parse(CsvUtils.escape(origin)).get(0).get(0));
    }

    @Test
    void blankRowDetection() {
        assertTrue(CsvUtils.isBlankRow(null));
        assertTrue(CsvUtils.isBlankRow(Arrays.asList("", "  ", null)));
        assertFalse(CsvUtils.isBlankRow(List.of("", "值")));
    }
}
