package com.enterprise.personnel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.personnel.common.BusinessException;
import com.enterprise.personnel.common.CsvUtils;
import com.enterprise.personnel.entity.PersonTag;
import com.enterprise.personnel.entity.Tag;
import com.enterprise.personnel.mapper.PersonMapper;
import com.enterprise.personnel.mapper.PersonTagMapper;
import com.enterprise.personnel.mapper.TagMapper;
import com.enterprise.personnel.service.TagService;
import com.enterprise.personnel.vo.TagBindingCountVO;
import com.enterprise.personnel.vo.TagImportResultVO;
import com.enterprise.personnel.vo.TagOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    /** 行级错误最多返回的条数（错误总条数见 errorTotal） */
    private static final int MAX_REPORTED_ERRORS = 50;

    private static final int NAME_MAX_LENGTH = 50;

    private static final DateTimeFormatter EXPORT_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PersonTagMapper personTagMapper;

    private final PersonMapper personMapper;

    @Override
    public List<Tag> listAll() {
        return list(new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getId));
    }

    @Override
    public String exportCsv() {
        StringBuilder csv = new StringBuilder("ID,标签名称,创建时间\r\n");
        for (Tag tag : listAll()) {
            csv.append(CsvUtils.escape(String.valueOf(tag.getId())))
                    .append(',')
                    .append(CsvUtils.escape(tag.getName()))
                    .append(',')
                    .append(tag.getCreatedAt() == null ? "" : EXPORT_TIME_FORMAT.format(tag.getCreatedAt()))
                    .append("\r\n");
        }
        return csv.toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TagImportResultVO importCsv(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new BusinessException("导入文件为空，请选择包含数据的 CSV 文件");
        }
        List<List<String>> rows = CsvUtils.parse(decode(bytes));

        // ---------- 1. 定位并校验表头 ----------
        int headerIndex = -1;
        for (int i = 0; i < rows.size(); i++) {
            if (!CsvUtils.isBlankRow(rows.get(i))) {
                headerIndex = i;
                break;
            }
        }
        if (headerIndex < 0) {
            throw new BusinessException("导入文件为空，请选择包含数据的 CSV 文件");
        }
        List<String> header = rows.get(headerIndex);
        int idColumn = -1;
        int nameColumn = -1;
        for (int i = 0; i < header.size(); i++) {
            String cell = cell(header, i);
            if (idColumn < 0 && cell.equalsIgnoreCase("id")) {
                idColumn = i;
            }
            if (nameColumn < 0 && (cell.equals("标签名称") || cell.equals("名称") || cell.equalsIgnoreCase("name"))) {
                nameColumn = i;
            }
        }
        if (idColumn < 0 || nameColumn < 0) {
            throw new BusinessException("文件格式不正确：首行表头需包含「ID」和「标签名称」两列，例如：ID,标签名称");
        }

        // ---------- 2. 行级格式校验（文件内唯一性） ----------
        List<TagImportResultVO.RowError> errors = new ArrayList<>();
        int errorTotal = 0;
        List<Tag> pending = new ArrayList<>();
        List<Integer> pendingRows = new ArrayList<>();
        Map<Long, Integer> seenIds = new HashMap<>();
        Map<String, Integer> seenNames = new HashMap<>();

        for (int i = headerIndex + 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (CsvUtils.isBlankRow(row)) {
                continue;
            }
            int rowNo = i + 1;
            String idText = cell(row, idColumn);
            String name = cell(row, nameColumn);

            Long id = null;
            if (!idText.isEmpty()) {
                if (!idText.matches("\\d{1,18}")) {
                    if (errors.size() < MAX_REPORTED_ERRORS) {
                        errors.add(new TagImportResultVO.RowError(rowNo, "ID『" + idText + "』不是有效的数字"));
                    }
                    errorTotal++;
                    continue;
                }
                id = Long.parseLong(idText);
                Integer firstRow = seenIds.putIfAbsent(id, rowNo);
                if (firstRow != null) {
                    if (errors.size() < MAX_REPORTED_ERRORS) {
                        errors.add(new TagImportResultVO.RowError(rowNo, "ID " + id + " 与第 " + firstRow + " 行重复"));
                    }
                    errorTotal++;
                }
            }
            if (name.isEmpty()) {
                if (errors.size() < MAX_REPORTED_ERRORS) {
                    errors.add(new TagImportResultVO.RowError(rowNo, "标签名称不能为空"));
                }
                errorTotal++;
                continue;
            }
            if (name.length() > NAME_MAX_LENGTH) {
                if (errors.size() < MAX_REPORTED_ERRORS) {
                    errors.add(new TagImportResultVO.RowError(rowNo,
                            "标签名称超过 " + NAME_MAX_LENGTH + " 个字符（当前 " + name.length() + " 个）"));
                }
                errorTotal++;
                continue;
            }
            Integer firstRow = seenNames.putIfAbsent(name, rowNo);
            if (firstRow != null) {
                if (errors.size() < MAX_REPORTED_ERRORS) {
                    errors.add(new TagImportResultVO.RowError(rowNo, "标签名称「" + name + "」与第 " + firstRow + " 行重复"));
                }
                errorTotal++;
            }

            Tag tag = new Tag();
            tag.setId(id);
            tag.setName(name);
            pending.add(tag);
            pendingRows.add(rowNo);
        }

        if (pending.isEmpty() && errorTotal == 0) {
            throw new BusinessException("文件中没有可导入的数据行，表头之下需至少一行标签数据");
        }

        // ---------- 3. 与库中数据冲突校验（按 ID 判断更新 / 新增） ----------
        Map<Long, Tag> existingById = new HashMap<>();
        Map<String, Long> nameOwner = new HashMap<>();
        for (Tag existing : listAll()) {
            existingById.put(existing.getId(), existing);
            nameOwner.put(existing.getName(), existing.getId());
        }

        List<Tag> toInsert = new ArrayList<>();
        List<Tag> toUpdate = new ArrayList<>();
        int unchanged = 0;
        for (int i = 0; i < pending.size(); i++) {
            Tag rowTag = pending.get(i);
            int rowNo = pendingRows.get(i);

            Long owner = nameOwner.get(rowTag.getName());
            if (owner != null && !owner.equals(rowTag.getId())) {
                if (errors.size() < MAX_REPORTED_ERRORS) {
                    errors.add(new TagImportResultVO.RowError(rowNo,
                            "标签名称「" + rowTag.getName() + "」已被 ID=" + owner + " 的标签占用"));
                }
                errorTotal++;
                continue;
            }

            Tag exist = rowTag.getId() == null ? null : existingById.get(rowTag.getId());
            if (exist == null) {
                // ID 为空或库中不存在：新增（ID 为空时由雪花算法生成）
                toInsert.add(rowTag);
            } else if (exist.getName().equals(rowTag.getName())) {
                unchanged++;
            } else {
                // 同 ID 更新：只改名称并刷新 updated_at，保留原始创建时间
                Tag update = new Tag();
                update.setId(exist.getId());
                update.setName(rowTag.getName());
                toUpdate.add(update);
            }
        }

        // ---------- 4. 存在任一错误：整体不落库，返回错误明细 ----------
        if (errorTotal > 0) {
            TagImportResultVO result = new TagImportResultVO();
            result.setSuccess(false);
            result.setErrors(errors);
            result.setErrorTotal(errorTotal);
            result.setTotal(count());
            return result;
        }

        // ---------- 5. 落库（同一事务，异常整体回滚） ----------
        if (!toInsert.isEmpty()) {
            saveBatch(toInsert);
        }
        if (!toUpdate.isEmpty()) {
            updateBatchById(toUpdate);
        }

        TagImportResultVO result = new TagImportResultVO();
        result.setSuccess(true);
        result.setCreated(toInsert.size());
        result.setUpdated(toUpdate.size());
        result.setUnchanged(unchanged);
        result.setTotal(count());
        return result;
    }

    /**
     * 读取 CSV 内容：优先严格 UTF-8 解码（自动去 BOM），失败则按 GBK 解码，
     * 兼容 Excel「另存为 ANSI(CSV)」产生的中文编码。
     */
    private String decode(byte[] bytes) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
            return text.startsWith("\uFEFF") ? text.substring(1) : text;
        } catch (CharacterCodingException e) {
            return new String(bytes, Charset.forName("GBK"));
        }
    }

    private String cell(List<String> row, int index) {
        if (index >= row.size()) {
            return "";
        }
        String value = row.get(index);
        return value == null ? "" : value.trim();
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

    @Override
    public TagOverviewVO overview() {
        // 已打标人数按 person_id 去重：一人绑定多个标签仍只算 1 人
        long hasTag = personTagMapper.selectList(null).stream()
                .map(PersonTag::getPersonId)
                .distinct()
                .count();
        long total = personMapper.selectCount(null);

        TagOverviewVO vo = new TagOverviewVO();
        vo.setTotal((int) total);
        vo.setHasTagCount((int) hasTag);
        vo.setNoTagCount((int) (total - hasTag));
        return vo;
    }

    @Override
    public List<TagBindingCountVO> bindingCounts() {
        // 有绑定关系的标签：tagId -> 绑定人次（同一人员绑定多个标签会重复计数）
        Map<Long, Long> countByTag = personTagMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(PersonTag::getTagId, Collectors.counting()));

        // 全量标签都保留（无绑定的标签计 0），标签删除后自然不再出现
        List<Tag> tags = listAll();
        List<TagBindingCountVO> result = new ArrayList<>(tags.size());
        for (Tag tag : tags) {
            TagBindingCountVO vo = new TagBindingCountVO();
            vo.setTagName(tag.getName());
            vo.setCount(countByTag.getOrDefault(tag.getId(), 0L).intValue());
            result.add(vo);
        }
        // 按人次降序；次数相同保持标签 ID 升序（稳定排序）
        result.sort(Comparator.comparingInt(TagBindingCountVO::getCount).reversed());
        return result;
    }
}
