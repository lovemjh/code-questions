package com.enterprise.personnel.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.personnel.common.Result;
import com.enterprise.personnel.dto.SuperiorUpdateRequest;
import com.enterprise.personnel.dto.TagBindRequest;
import com.enterprise.personnel.service.PersonService;
import com.enterprise.personnel.vo.PersonTreeNode;
import com.enterprise.personnel.vo.PersonVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 人员 API：基础信息由外部系统同步、只读，
 * 不提供新增/删除/修改基础信息接口，仅支持查询、上级调整与标签绑定。
 */
@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    /** 分页查询：姓名关键词 + 标签筛选 */
    @GetMapping
    public Result<Page<PersonVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                       @RequestParam(defaultValue = "10") long pageSize,
                                       @RequestParam(required = false) String name,
                                       @RequestParam(required = false) Long tagId) {
        return Result.ok(personService.pageQuery(pageNum, pageSize, name, tagId));
    }

    /** 人员详情（含上级姓名、标签列表） */
    @GetMapping("/{id}")
    public Result<PersonVO> detail(@PathVariable Long id) {
        return Result.ok(personService.getDetail(id));
    }

    /** 组织架构树 */
    @GetMapping("/tree")
    public Result<List<PersonTreeNode>> tree() {
        return Result.ok(personService.getTree());
    }

    /** 调整上级：superiorId 为 null 表示设为顶级节点，后端校验组织循环 */
    @PutMapping("/{id}/superior")
    public Result<Void> updateSuperior(@PathVariable Long id,
                                       @Validated @RequestBody SuperiorUpdateRequest request) {
        personService.updateSuperior(id, request.getSuperiorId());
        return Result.ok();
    }

    /** 全量替换人员标签绑定（可绑定多个标签） */
    @PutMapping("/{id}/tags")
    public Result<Void> bindTags(@PathVariable Long id,
                                 @Validated @RequestBody TagBindRequest request) {
        personService.bindTags(id, request.getTagIds());
        return Result.ok();
    }
}
