package com.enterprise.personnel.controller;

import com.enterprise.personnel.common.Result;
import com.enterprise.personnel.dto.TagSaveRequest;
import com.enterprise.personnel.entity.Tag;
import com.enterprise.personnel.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 标签库管理 API
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /** 标签列表 */
    @GetMapping
    public Result<List<Tag>> list() {
        return Result.ok(tagService.listAll());
    }

    /** 新增标签 */
    @PostMapping
    public Result<Tag> create(@Validated @RequestBody TagSaveRequest request) {
        return Result.ok(tagService.create(request.getName()));
    }

    /** 重命名标签 */
    @PutMapping("/{id}")
    public Result<Tag> rename(@PathVariable Long id, @Validated @RequestBody TagSaveRequest request) {
        return Result.ok(tagService.rename(id, request.getName()));
    }

    /** 删除标签（级联解除人员绑定） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return Result.ok();
    }
}
