package com.enterprise.personnel.common;

import java.util.ArrayList;
import java.util.List;

/**
 * 轻量 CSV 工具（RFC 4180 常用子集）：
 * 支持双引号包裹字段、字段内逗号 / 换行、双引号转义。
 *
 * 项目未引入 Excel 依赖，标签导入导出统一使用 CSV（UTF-8，兼容 Excel 另存为 ANSI 的 GBK 文件）。
 */
public final class CsvUtils {

    private CsvUtils() {
    }

    /**
     * 解析 CSV 文本为行列表（每行为一个单元格列表），由调用方做表头与业务校验。
     * 文件末尾的换行会产生一个全空行，可用 {@link #isBlankRow(List)} 过滤。
     */
    public static List<List<String>> parse(String text) {
        List<List<String>> rows = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return rows;
        }
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    // "" 视为字面量引号
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                row.add(field.toString());
                field.setLength(0);
            } else if (c == '\r') {
                // \r\n 统一交给 \n 分支处理，孤立 \r 视为换行
                if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    continue;
                }
                row.add(field.toString());
                field.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            } else if (c == '\n') {
                row.add(field.toString());
                field.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            } else {
                field.append(c);
            }
        }
        row.add(field.toString());
        rows.add(row);
        return rows;
    }

    /** 单个字段转义：含分隔符 / 引号 / 换行时用双引号包裹，内部引号翻倍 */
    public static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        boolean needQuote = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needQuote ? "\"" + escaped + "\"" : escaped;
    }

    /** 行内单元格是否全部为空白（用于跳过空行） */
    public static boolean isBlankRow(List<String> row) {
        if (row == null || row.isEmpty()) {
            return true;
        }
        for (String cell : row) {
            if (cell != null && !cell.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
