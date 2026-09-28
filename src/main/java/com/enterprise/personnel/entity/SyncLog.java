package com.enterprise.personnel.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 外部同步审计日志：每次请求一个 batch_no，逐条记录结果。
 * 只记录人员 id 与处理结果，不落姓名/手机号/邮箱等字段值（脱敏）。
 */
@Data
@TableName("sync_log")
public class SyncLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 批次号（每次同步请求生成 UUID） */
    private String batchNo;

    /** 人员ID（外部主键）；请求体校验失败时可为空 */
    private Long personId;

    /** 动作：CREATE / UPDATE；失败行为空 */
    private String action;

    /** 本条是否处理成功 */
    private Boolean success;

    /** 失败原因（上限 500 字符） */
    private String errorMsg;

    /** 调用方 IP */
    private String sourceIp;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
