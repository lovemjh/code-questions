package com.enterprise.personnel.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 标签新增/重命名请求
 */
@Data
public class TagSaveRequest {

    @NotBlank(message = "标签名称不能为空")
    @Size(max = 50, message = "标签名称不能超过 50 个字符")
    private String name;
}
