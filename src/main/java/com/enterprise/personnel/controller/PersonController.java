package com.enterprise.personnel.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.personnel.common.Result;
import com.enterprise.personnel.service.PersonService;
import com.enterprise.personnel.vo.PersonVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
