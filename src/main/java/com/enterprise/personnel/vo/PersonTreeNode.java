package com.enterprise.personnel.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 组织架构树节点
 */
@Data
public class PersonTreeNode {

    private Long id;

    private String name;

    private String employeeNo;

    private String department;

    private String position;

    private List<PersonTreeNode> children = new ArrayList<>();
}
