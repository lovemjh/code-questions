package com.enterprise.personnel.controller;

import com.enterprise.personnel.common.BusinessException;
import com.enterprise.personnel.common.Result;
import com.enterprise.personnel.dto.PersonSyncItem;
import com.enterprise.personnel.service.PersonSyncService;
import com.enterprise.personnel.vo.SyncResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 外部人员同步 API：独立业务 Token 鉴权（SyncTokenFilter），不依赖登录态。
 * 仅暴露人员 upsert；组织架构调整、标签维护等业务接口不在此开放。
 */
@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    /** 单批上限：更大数据量由外部系统分批串行推送 */
    private static final int MAX_BATCH_SIZE = 500;

    private final PersonSyncService personSyncService;

    /** 批量人员同步（单条同步 = 数组传 1 个元素），逐条处理并返回结果清单 */
    @PostMapping("/persons")
    public Result<SyncResultVO> syncPersons(@RequestBody List<PersonSyncItem> items,
                                            HttpServletRequest request) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("同步列表为空");
        }
        if (items.size() > MAX_BATCH_SIZE) {
            throw new BusinessException("单批不能超过 " + MAX_BATCH_SIZE + " 条");
        }
        return Result.ok(personSyncService.syncBatch(items, request.getRemoteAddr()));
    }
}
