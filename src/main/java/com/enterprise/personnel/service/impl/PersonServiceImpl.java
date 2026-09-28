package com.enterprise.personnel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.personnel.common.BusinessException;
import com.enterprise.personnel.dto.PersonSyncItem;
import com.enterprise.personnel.entity.Person;
import com.enterprise.personnel.entity.PersonTag;
import com.enterprise.personnel.entity.Tag;
import com.enterprise.personnel.mapper.PersonMapper;
import com.enterprise.personnel.mapper.PersonTagMapper;
import com.enterprise.personnel.mapper.TagMapper;
import com.enterprise.personnel.service.PersonService;
import com.enterprise.personnel.vo.PersonTreeNode;
import com.enterprise.personnel.vo.PersonVO;
import com.enterprise.personnel.vo.TagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonServiceImpl extends ServiceImpl<PersonMapper, Person> implements PersonService {

    /** 上级链回溯的最大深度，超过视为循环引用 */
    private static final int MAX_SUPERIOR_DEPTH = 1000;

    /** 系统标签「离职」：由同步按 resigned 字段自动绑定/解绑 */
    private static final String RESIGNED_TAG_NAME = "离职";

    private final TagMapper tagMapper;
    private final PersonTagMapper personTagMapper;

    @Override
    public Page<PersonVO> pageQuery(long pageNum, long pageSize, String name, Long tagId) {
        LambdaQueryWrapper<Person> wrapper = new LambdaQueryWrapper<Person>()
                .like(StringUtils.hasText(name), Person::getName, name)
                // 标签筛选：存在指向该标签的绑定记录
                .apply(tagId != null,
                        "EXISTS (SELECT 1 FROM person_tag pt WHERE pt.person_id = person.id AND pt.tag_id = {0})",
                        tagId)
                .orderByAsc(Person::getId);
        Page<Person> page = page(new Page<>(pageNum, pageSize), wrapper);

        Page<PersonVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(toVOList(page.getRecords()));
        return voPage;
    }

    @Override
    public PersonVO getDetail(Long id) {
        Person person = requirePerson(id);
        return toVOList(Collections.singletonList(person)).get(0);
    }

    @Override
    public List<PersonTreeNode> getTree() {
        // 离职人员不进入组织架构；其下属因"上级不在树内"走下方根节点兜底，自动提升为顶级节点
        List<Person> all = list(new LambdaQueryWrapper<Person>()
                .eq(Person::getResigned, false)
                .orderByAsc(Person::getId));
        Map<Long, PersonTreeNode> nodeById = new LinkedHashMap<>();
        for (Person p : all) {
            PersonTreeNode node = new PersonTreeNode();
            node.setId(p.getId());
            node.setName(p.getName());
            node.setEmployeeNo(p.getEmployeeNo());
            node.setDepartment(p.getDepartment());
            node.setPosition(p.getPosition());
            nodeById.put(p.getId(), node);
        }
        List<PersonTreeNode> roots = new ArrayList<>();
        for (Person p : all) {
            PersonTreeNode node = nodeById.get(p.getId());
            PersonTreeNode parent = p.getSuperiorId() == null ? null : nodeById.get(p.getSuperiorId());
            if (parent != null) {
                parent.getChildren().add(node);
            } else {
                // 顶级节点；上级不在系统内时也按根节点展示，避免整树丢失
                roots.add(node);
            }
        }
        return roots;
    }

    @Override
    public void updateSuperior(Long id, Long superiorId) {
        requirePerson(id);
        if (superiorId != null) {
            if (superiorId.equals(id)) {
                throw new BusinessException("不能将自己设置为自己的上级");
            }
            checkNoCycle(id, superiorId);
        }
        // superiorId 可为 null（设为顶级节点），需用 set 显式置空
        lambdaUpdate()
                .eq(Person::getId, id)
                .set(Person::getSuperiorId, superiorId)
                .set(Person::getUpdatedAt, LocalDateTime.now())
                .update();
    }

    /** 沿新上级逐级向上回溯，若遇到本人则调整后会形成组织循环 */
    private void checkNoCycle(Long personId, Long superiorId) {
        Person superior = requirePerson(superiorId);
        Long cursor = superior.getId();
        int depth = 0;
        while (cursor != null) {
            if (cursor.equals(personId)) {
                throw new BusinessException("设置该上级会形成组织循环，请调整后重试");
            }
            Person current = getById(cursor);
            if (current == null) {
                throw new BusinessException("上级关系链异常，人员不存在：" + cursor);
            }
            cursor = current.getSuperiorId();
            if (++depth > MAX_SUPERIOR_DEPTH) {
                throw new BusinessException("组织层级过深，疑似存在循环引用");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindTags(Long id, List<Long> tagIds) {
        requirePerson(id);
        List<Long> distinctTagIds = tagIds == null ? Collections.emptyList()
                : tagIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (!distinctTagIds.isEmpty()) {
            int found = tagMapper.selectBatchIds(distinctTagIds).size();
            if (found != distinctTagIds.size()) {
                throw new BusinessException("存在无效的标签ID");
            }
        }
        // 「离职」绑定由同步按 resigned 维护，不在手动替换范围，替换时原样保留
        Long resignedTagId = requireResignedTagId();
        if (distinctTagIds.contains(resignedTagId)) {
            throw new BusinessException("「离职」标签由系统自动维护，不能手动绑定");
        }
        // 全量替换：先清空旧绑定（排除系统标签），再写入新绑定
        personTagMapper.delete(new LambdaQueryWrapper<PersonTag>()
                .eq(PersonTag::getPersonId, id)
                .ne(PersonTag::getTagId, resignedTagId));
        for (Long tagId : distinctTagIds) {
            PersonTag binding = new PersonTag();
            binding.setPersonId(id);
            binding.setTagId(tagId);
            personTagMapper.insert(binding);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String upsertFromSync(PersonSyncItem item) {
        String action = getById(item.getId()) == null ? insertFromSync(item) : applySyncUpdate(item);
        // 「离职」绑定是 resigned 的投影，随本条同步在同一事务内对齐
        syncResignedTagBinding(item.getId(), Boolean.TRUE.equals(item.getResigned()));
        return action;
    }

    /** 新增人员（id 使用外部值）；并发重复推送撞主键时降级为更新（幂等兜底） */
    private String insertFromSync(PersonSyncItem item) {
        Person person = new Person();
        person.setId(item.getId());
        person.setName(item.getName());
        person.setEmployeeNo(item.getEmployeeNo());
        person.setDepartment(item.getDepartment());
        person.setPosition(item.getPosition());
        person.setEmail(item.getEmail());
        person.setPhone(item.getPhone());
        person.setResigned(Boolean.TRUE.equals(item.getResigned()));
        try {
            save(person);
            return "CREATE";
        } catch (DuplicateKeyException e) {
            if (getById(item.getId()) == null) {
                // 非主键冲突：employee_no 唯一约束冲突
                throw new BusinessException("工号已被其他人员占用（employeeNo=" + item.getEmployeeNo() + "）");
            }
            return applySyncUpdate(item);
        }
    }

    /**
     * 仅更新同步字段；MyBatis-Plus 默认忽略 null 字段，实现协议的"null = 忽略"语义。
     * 不触碰 superiorId 与业务标签绑定。
     */
    private String applySyncUpdate(PersonSyncItem item) {
        Person patch = new Person();
        patch.setId(item.getId());
        patch.setName(item.getName());
        patch.setEmployeeNo(item.getEmployeeNo());
        patch.setDepartment(item.getDepartment());
        patch.setPosition(item.getPosition());
        patch.setEmail(item.getEmail());
        patch.setPhone(item.getPhone());
        if (item.getResigned() != null) {
            patch.setResigned(item.getResigned());
        }
        updateById(patch);
        return "UPDATE";
    }

    /** 使「离职」标签绑定与 resigned 状态一致：true 补绑、false 解绑，已一致则不动 */
    private void syncResignedTagBinding(Long personId, boolean resigned) {
        Long resignedTagId = requireResignedTagId();
        LambdaQueryWrapper<PersonTag> bindingQuery = new LambdaQueryWrapper<PersonTag>()
                .eq(PersonTag::getPersonId, personId)
                .eq(PersonTag::getTagId, resignedTagId);
        boolean bound = personTagMapper.selectCount(bindingQuery) > 0;
        if (resigned && !bound) {
            PersonTag binding = new PersonTag();
            binding.setPersonId(personId);
            binding.setTagId(resignedTagId);
            personTagMapper.insert(binding);
        } else if (!resigned && bound) {
            personTagMapper.delete(bindingQuery);
        }
    }

    /** 取「离职」系统标签 id；缺失时补建，历史同名标签补打 is_system 标记 */
    private Long requireResignedTagId() {
        Tag tag = tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, RESIGNED_TAG_NAME));
        if (tag == null) {
            tag = new Tag();
            tag.setName(RESIGNED_TAG_NAME);
            tag.setIsSystem(true);
            tagMapper.insert(tag);
        } else if (!Boolean.TRUE.equals(tag.getIsSystem())) {
            Tag patch = new Tag();
            patch.setId(tag.getId());
            patch.setIsSystem(true);
            tagMapper.updateById(patch);
        }
        return tag.getId();
    }

    private Person requirePerson(Long id) {
        Person person = getById(id);
        if (person == null) {
            throw new BusinessException("人员不存在：" + id);
        }
        return person;
    }

    /** 批量组装 VO：补充上级姓名与标签列表 */
    private List<PersonVO> toVOList(List<Person> persons) {
        if (CollectionUtils.isEmpty(persons)) {
            return Collections.emptyList();
        }
        Set<Long> superiorIds = persons.stream()
                .map(Person::getSuperiorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> nameById = superiorIds.isEmpty() ? Collections.emptyMap()
                : listByIds(superiorIds).stream()
                        .collect(Collectors.toMap(Person::getId, Person::getName));

        Set<Long> personIds = persons.stream().map(Person::getId).collect(Collectors.toSet());
        Map<Long, List<TagVO>> tagsByPersonId = loadTagsByPersonIds(personIds);

        return persons.stream().map(p -> {
            PersonVO vo = new PersonVO();
            vo.setId(p.getId());
            vo.setName(p.getName());
            vo.setEmployeeNo(p.getEmployeeNo());
            vo.setDepartment(p.getDepartment());
            vo.setPosition(p.getPosition());
            vo.setEmail(p.getEmail());
            vo.setPhone(p.getPhone());
            vo.setSuperiorId(p.getSuperiorId());
            vo.setSuperiorName(p.getSuperiorId() == null ? null : nameById.get(p.getSuperiorId()));
            vo.setTags(tagsByPersonId.getOrDefault(p.getId(), Collections.emptyList()));
            return vo;
        }).collect(Collectors.toList());
    }

    /** 按人员ID批量加载标签绑定 */
    private Map<Long, List<TagVO>> loadTagsByPersonIds(Set<Long> personIds) {
        if (personIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<PersonTag> bindings = personTagMapper.selectList(
                new LambdaQueryWrapper<PersonTag>().in(PersonTag::getPersonId, personIds));
        if (bindings.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> tagIds = bindings.stream().map(PersonTag::getTagId).collect(Collectors.toSet());
        Map<Long, String> tagNameById = tagMapper.selectBatchIds(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, Tag::getName));

        return bindings.stream()
                .filter(b -> tagNameById.containsKey(b.getTagId()))
                .collect(Collectors.groupingBy(PersonTag::getPersonId,
                        Collectors.mapping(
                                b -> new TagVO(b.getTagId(), tagNameById.get(b.getTagId())),
                                Collectors.toList())));
    }
}
