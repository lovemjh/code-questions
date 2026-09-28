package com.enterprise.personnel.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 标签导入结果
 *
 * 行级校验失败时 success=false 且整体不落库（整体回滚策略），
 * 通过 200 + 结果体返回错误明细，便于前端逐行友好展示。
 */
@Data
public class TagImportResultVO {

    /** 是否导入成功（false 表示校验失败，数据库未做任何修改） */
    private boolean success;

    /** 新增条数 */
    private int created;

    /** 更新条数（ID 已存在且名称发生变化） */
    private int updated;

    /** 未变化条数（ID 与名称均一致，跳过写入） */
    private int unchanged;

    /** 导入后标签总数 */
    private long total;

    /** 错误总条数（可能大于 errors 的条数，errors 最多返回前若干条） */
    private int errorTotal;

    /** 行级错误明细（success=false 时非空） */
    private List<RowError> errors = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowError {

        /** 文件中的行号（含表头，表头为第 1 行） */
        private int row;

        /** 错误原因 */
        private String reason;
    }
}
