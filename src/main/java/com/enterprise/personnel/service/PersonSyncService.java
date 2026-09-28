package com.enterprise.personnel.service;

import com.enterprise.personnel.dto.PersonSyncItem;
import com.enterprise.personnel.vo.SyncResultVO;

import java.util.List;

/**
 * 外部人员同步编排：逐条校验 → 逐条独立事务 upsert → 审计落库
 * （本服务自身不加事务，单条失败不影响其余条）
 */
public interface PersonSyncService {

    /**
     * 批量 upsert 人员
     *
     * @param items    人员列表（≤500 条，控制器已校验）
     * @param sourceIp 调用方 IP（审计）
     */
    SyncResultVO syncBatch(List<PersonSyncItem> items, String sourceIp);
}
