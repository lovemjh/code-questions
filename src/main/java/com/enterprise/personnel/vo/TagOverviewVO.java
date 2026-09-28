package com.enterprise.personnel.vo;

import lombok.Data;

/**
 * 人员打标大盘统计（文字汇总 + 饼图）
 * 统计口径：人员维度，一人仅统计一次（绑定多个标签仍算 1 人）
 */
@Data
public class TagOverviewVO {

    /** 人员总数 */
    private int total;

    /** 已打标人数（绑定过至少一个标签，去重） */
    private int hasTagCount;

    /** 未打标人数 */
    private int noTagCount;
}
