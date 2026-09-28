package com.enterprise.personnel.vo;

import lombok.Data;

import java.util.List;

/**
 * 人员视图对象（含上级姓名与标签列表）
 */
@Data
public class PersonVO {

    private Long id;

    private String name;

    private String employeeNo;

    private String department;

    private String position;

    private String email;

    private String phone;

    /** 上级人员ID，NULL 表示顶级节点 */
    private Long superiorId;

    /** 上级姓名 */
    private String superiorName;

    /** 已绑定标签 */
    private List<TagVO> tags;
}
