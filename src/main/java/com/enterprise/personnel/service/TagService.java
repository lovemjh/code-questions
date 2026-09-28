package com.enterprise.personnel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.personnel.entity.Tag;
import com.enterprise.personnel.vo.TagBindingCountVO;
import com.enterprise.personnel.vo.TagImportResultVO;
import com.enterprise.personnel.vo.TagOverviewVO;

import java.util.List;

/**
 * 全局标签库服务
 */
public interface TagService extends IService<Tag> {

    /** 全量标签列表 */
    List<Tag> listAll();

    /** 导出全量标签 CSV 文本（表头：ID,标签名称,创建时间） */
    String exportCsv();

    /**
     * 导入标签 CSV：以 ID 为唯一键，ID 已存在则更新、不存在则新增。
     * 先整体校验（格式 / 文件内重复 / 与库中名称冲突），任一行有误则不落库并返回错误明细。
     */
    TagImportResultVO importCsv(byte[] bytes);

    /** 新增标签（名称全局唯一） */
    Tag create(String name);

    /** 重命名标签 */
    Tag rename(Long id, String name);

    /** 删除标签，并级联删除人员绑定关系 */
    void delete(Long id);

    /** 人员打标大盘统计：总人数、已打标人数（去重）、未打标人数 */
    TagOverviewVO overview();

    /** 各标签绑定人次统计：全量标签（含 0 人次），按人次降序 */
    List<TagBindingCountVO> bindingCounts();
}
