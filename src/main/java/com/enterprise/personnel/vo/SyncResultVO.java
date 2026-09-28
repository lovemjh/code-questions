package com.enterprise.personnel.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 批量同步结果：逐条独立处理，失败明细见 errors，不影响其余条 */
@Data
public class SyncResultVO {

    private int total;

    private int success;

    private int fail;

    private List<SyncErrorItem> errors = new ArrayList<>();
}
