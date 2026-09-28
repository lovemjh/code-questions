package com.enterprise.personnel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.personnel.common.BusinessException;
import com.enterprise.personnel.entity.PersonTag;
import com.enterprise.personnel.entity.Tag;
import com.enterprise.personnel.mapper.PersonTagMapper;
import com.enterprise.personnel.mapper.TagMapper;
import com.enterprise.personnel.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    private final PersonTagMapper personTagMapper;

    @Override
    public List<Tag> listAll() {
        return list(new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getId));
    }

    @Override
    public Tag create(String name) {
        checkNameConflict(null, name);
        Tag tag = new Tag();
        tag.setName(name);
        save(tag);
        return tag;
    }

    @Override
    public Tag rename(Long id, String name) {
        Tag tag = getById(id);
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }
        checkNameConflict(id, name);
        tag.setName(name);
        // 置空以触发 MetaObjectHandler 的更新时间填充
        tag.setUpdatedAt(null);
        updateById(tag);
        return tag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (getById(id) == null) {
            throw new BusinessException("标签不存在");
        }
        removeById(id);
        // 级联删除该标签的所有人员绑定关系
        personTagMapper.delete(new LambdaQueryWrapper<PersonTag>().eq(PersonTag::getTagId, id));
    }

    private void checkNameConflict(Long excludeId, String name) {
        Long count = lambdaQuery()
                .eq(Tag::getName, name)
                .ne(excludeId != null, Tag::getId, excludeId)
                .count();
        if (count != null && count > 0) {
            throw new BusinessException("标签名称已存在：" + name);
        }
    }
}
