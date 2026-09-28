package com.enterprise.personnel.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 单条同步失败明细 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncErrorItem {

    /** 人员ID（外部主键） */
    private Long id;

    /** 失败原因 */
    private String message;
}
