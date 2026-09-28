package com.enterprise.personnel.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 用户新增/编辑请求
 */
@Data
public class UserSaveRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度需在 3-50 个字符之间")
    private String username;

    /** 密码（新增时必填，编辑时可选） */
    private String password;

    /** 显示名称 */
    private String displayName;

    /** 角色ID列表 */
    private List<Long> roleIds;
}
