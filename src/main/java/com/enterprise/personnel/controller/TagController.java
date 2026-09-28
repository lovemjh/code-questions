package com.enterprise.personnel.controller;

import com.enterprise.personnel.common.BusinessException;
import com.enterprise.personnel.common.Result;
import com.enterprise.personnel.dto.TagSaveRequest;
import com.enterprise.personnel.entity.Tag;
import com.enterprise.personnel.service.TagService;
import com.enterprise.personnel.vo.TagImportResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

    /** 导出标签 CSV（管理员）：UTF-8 带 BOM，Excel / WPS 可直接打开 */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv() {
        String csv = "\uFEFF" + tagService.exportCsv();
        String filename = "标签库_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 导入标签 CSV（管理员）：以 ID 为唯一键，同 ID 更新、异 ID 新增。
     * 行级校验失败时整体不落库，通过 success=false + errors 返回错误明细。
     */
    @PostMapping("/import")
    public Result<TagImportResultVO> importCsv(@RequestParam("file") MultipartFile file) {
        try {
            return Result.ok(tagService.importCsv(file.getBytes()));
        } catch (IOException e) {
            throw new BusinessException("读取上传文件失败，请重试");
        }
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
