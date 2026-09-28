package com.enterprise.personnel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.personnel.entity.Tag;

import java.util.List;

/**
 * 全局标签库服务
 */
public interface TagService extends IService<Tag> {

    /** 全量标签列表 */
    List<Tag> listAll();

    /** 新增标签（名称全局唯一） */
    Tag create(String name);

    /** 重命名标签 */
    Tag rename(Long id, String name);

    /** 删除标签，并级联删除人员绑定关系 */
    void delete(Long id);
}
