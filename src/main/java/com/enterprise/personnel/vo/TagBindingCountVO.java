package com.enterprise.personnel.vo;

import lombok.Data;

/**
 * 标签绑定人次统计（柱状图）
 * 统计口径：标签绑定人次维度，同一人员绑定多个标签会在多个标签下重复计数
 */
@Data
public class TagBindingCountVO {

    /** 标签名称 */
    private String tagName;

    /** 绑定人次 */
    private int count;
}
