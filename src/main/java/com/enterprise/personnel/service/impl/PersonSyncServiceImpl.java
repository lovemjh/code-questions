package com.enterprise.personnel.service.impl;

import com.enterprise.personnel.common.BusinessException;
import com.enterprise.personnel.dto.PersonSyncItem;
import com.enterprise.personnel.entity.SyncLog;
import com.enterprise.personnel.mapper.SyncLogMapper;
import com.enterprise.personnel.service.PersonService;
import com.enterprise.personnel.service.PersonSyncService;
import com.enterprise.personnel.vo.SyncErrorItem;
import com.enterprise.personnel.vo.SyncResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 同步编排：逐条 Bean Validation 校验 → 跨 Bean 调用 upsertFromSync
 * （独立事务生效，同类内部调用会失效）→ 审计落库。
 * 单条失败仅记入 errors 清单，不影响其余条（见《人员同步接口设计方案.md》4.1）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PersonSyncServiceImpl implements PersonSyncService {

    /** 审计错误信息字段上限（与 sync_log.error_msg 长度一致） */
    private static final int ERROR_MSG_MAX_LENGTH = 500;

    private final PersonService personService;
    private final SyncLogMapper syncLogMapper;
    private final Validator validator;

    @Override
    public SyncResultVO syncBatch(List<PersonSyncItem> items, String sourceIp) {
        String batchNo = UUID.randomUUID().toString();
        SyncResultVO result = new SyncResultVO();
        result.setTotal(items.size());
        for (PersonSyncItem item : items) {
            String violation = validate(item);
            if (violation != null) {
                recordFailure(result, batchNo, item.getId(), violation, sourceIp);
                continue;
            }
            try {
                String action = personService.upsertFromSync(item);
                insertAudit(batchNo, item.getId(), action, true, null, sourceIp);
                result.setSuccess(result.getSuccess() + 1);
            } catch (BusinessException e) {
                recordFailure(result, batchNo, item.getId(), e.getMessage(), sourceIp);
            } catch (DataIntegrityViolationException e) {
                // 主键冲突已在 upsert 内兜底降级，走到这里基本是工号唯一约束冲突
                recordFailure(result, batchNo, item.getId(), conflictMessage(e, item), sourceIp);
            } catch (Exception e) {
                log.error("人员同步异常 batchNo={} id={}", batchNo, item.getId(), e);
                recordFailure(result, batchNo, item.getId(), "系统异常，请稍后重试", sourceIp);
            }
        }
        log.info("人员同步完成 batchNo={} total={} success={} fail={} sourceIp={}",
                batchNo, result.getTotal(), result.getSuccess(), result.getFail(), sourceIp);
        return result;
    }

    private void recordFailure(SyncResultVO result, String batchNo, Long personId,
                               String message, String sourceIp) {
        log.warn("人员同步失败 batchNo={} id={}: {}", batchNo, personId, message);
        result.getErrors().add(new SyncErrorItem(personId, message));
        insertAudit(batchNo, personId, null, false, message, sourceIp);
        result.setFail(result.getFail() + 1);
    }

    /** Bean Validation 逐条校验，返回全部违规信息；通过返回 null */
    private String validate(PersonSyncItem item) {
        Set<ConstraintViolation<PersonSyncItem>> violations = validator.validate(item);
        if (violations.isEmpty()) {
            return null;
        }
        return violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
    }

    /** 唯一约束冲突转明确提示，便于外部系统按约束定位问题 */
    private String conflictMessage(DataIntegrityViolationException e, PersonSyncItem item) {
        String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if (message.contains("uk_person_employee_no")) {
            return "工号已被其他人员占用（employeeNo=" + item.getEmployeeNo() + "）";
        }
        return "数据冲突，请检查数据后重试";
    }

    /** 审计落库失败只告警，不影响同步主流程 */
    private void insertAudit(String batchNo, Long personId, String action,
                             boolean success, String errorMsg, String sourceIp) {
        try {
            SyncLog entry = new SyncLog();
            entry.setBatchNo(batchNo);
            entry.setPersonId(personId);
            entry.setAction(action);
            entry.setSuccess(success);
            entry.setErrorMsg(truncate(errorMsg));
            entry.setSourceIp(sourceIp);
            syncLogMapper.insert(entry);
        } catch (Exception e) {
            log.warn("写入同步审计日志失败 batchNo={} personId={}: {}", batchNo, personId, e.getMessage());
        }
    }

    private String truncate(String text) {
        if (text == null || text.length() <= ERROR_MSG_MAX_LENGTH) {
            return text;
        }
        return text.substring(0, ERROR_MSG_MAX_LENGTH);
    }
}
