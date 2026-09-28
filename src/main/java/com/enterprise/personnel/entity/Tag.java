package com.enterprise.personnel.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 全局标签
 */
@Data
@TableName("tag")
public class Tag {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 标签名称（全局唯一） */
    private String name;

    /** 系统标签（如「离职」）：由同步按 resigned 自动绑定/解绑，接口不可删改 */
    private Boolean isSystem;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
