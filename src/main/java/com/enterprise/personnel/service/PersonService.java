package com.enterprise.personnel.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.personnel.entity.Person;
import com.enterprise.personnel.vo.PersonTreeNode;
import com.enterprise.personnel.vo.PersonVO;

import java.util.List;

/**
 * 人员服务：基础信息只读（外部系统同步），仅维护上级关系与标签绑定
 */
public interface PersonService extends IService<Person> {

    /**
     * 分页查询人员
     *
     * @param name     姓名关键词（可选，模糊匹配）
     * @param tagId    标签筛选（可选，绑定该标签的人员）
     */
    Page<PersonVO> pageQuery(long pageNum, long pageSize, String name, Long tagId);

    /** 人员详情（含上级姓名、标签列表） */
    PersonVO getDetail(Long id);

    /** 组织架构树（顶级节点为根） */
    List<PersonTreeNode> getTree();

    /**
     * 调整上级关系
     *
     * @param superiorId 新上级ID；null 表示设为顶级节点；含组织循环校验
     */
    void updateSuperior(Long id, Long superiorId);

    /** 全量替换人员标签绑定 */
    void bindTags(Long id, List<Long> tagIds);
}
