package com.enterprise.personnel.dto;

import lombok.Data;

/**
 * 调整上级请求
 */
@Data
public class SuperiorUpdateRequest {

    /** 新上级人员ID；为 null 表示设为顶级节点（无上级） */
    private Long superiorId;
}
