package com.enterprise.personnel.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 人员（基础信息与离职标记由外部系统同步写入；仅 superior_id 上级关系本地可维护）
 */
@Data
@TableName("person")
public class Person {

    /** 人员ID（外部系统主键，唯一匹配键，本地不生成） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 姓名 */
    private String name;

    /** 工号 */
    private String employeeNo;

    /** 部门 */
    private String department;

    /** 职位 */
    private String position;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 上级人员ID，NULL 表示顶级节点 */
    private Long superiorId;

    /** 离职标记（同步协议字段，事实来源；组织架构树不展示离职人员） */
    private Boolean resigned;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
