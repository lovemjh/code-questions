package com.enterprise.personnel.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

/**
 * 外部人员同步单项：字段即同步协议（见《人员同步接口设计方案.md》2.2）。
 * 校验由同步服务逐条执行（失败进 errors 清单，不做整体 400）。
 */
@Data
public class PersonSyncItem {

    /** 外部系统人员主键，唯一匹配键：不可变、不复用 */
    @NotNull(message = "id 不能为空")
    @Positive(message = "id 必须为正整数")
    private Long id;

    @NotBlank(message = "name 不能为空")
    @Size(max = 50, message = "name 长度不能超过 50")
    private String name;

    @NotBlank(message = "employeeNo 不能为空")
    @Size(max = 30, message = "employeeNo 长度不能超过 30")
    private String employeeNo;

    @Size(max = 100, message = "department 长度不能超过 100")
    private String department;

    @Size(max = 100, message = "position 长度不能超过 100")
    private String position;

    @Size(max = 100, message = "email 长度不能超过 100")
    private String email;

    @Size(max = 30, message = "phone 长度不能超过 30")
    private String phone;

    /** 离职标记：true=离职，false=在职；null = 忽略该字段 */
    private Boolean resigned;
}
