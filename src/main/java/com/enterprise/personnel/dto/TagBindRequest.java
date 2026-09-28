package com.enterprise.personnel.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 人员标签绑定请求（全量替换）
 */
@Data
public class TagBindRequest {

    /** 标签ID全量列表；空数组表示清空绑定 */
    @NotNull(message = "tagIds 不能为空")
    private List<Long> tagIds;
}
