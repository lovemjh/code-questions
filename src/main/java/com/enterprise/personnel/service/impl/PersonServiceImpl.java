package com.enterprise.personnel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.personnel.common.BusinessException;
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
        List<Person> all = list(new LambdaQueryWrapper<Person>().orderByAsc(Person::getId));
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
        // 全量替换：先清空旧绑定，再写入新绑定
        personTagMapper.delete(new LambdaQueryWrapper<PersonTag>().eq(PersonTag::getPersonId, id));
        for (Long tagId : distinctTagIds) {
            PersonTag binding = new PersonTag();
            binding.setPersonId(id);
            binding.setTagId(tagId);
            personTagMapper.insert(binding);
        }
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
