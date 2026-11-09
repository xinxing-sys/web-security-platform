
package com.websecurity.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.websecurity.platform.entity.SysCategory;
import com.websecurity.platform.entity.Vulnerability;
import com.websecurity.platform.mapper.SysCategoryMapper;
import com.websecurity.platform.mapper.VulnerabilityMapper;
import com.websecurity.platform.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final SysCategoryMapper categoryMapper;
    private final VulnerabilityMapper vulnerabilityMapper;

    public CategoryServiceImpl(
            SysCategoryMapper categoryMapper,
            VulnerabilityMapper vulnerabilityMapper) {
        this.categoryMapper = categoryMapper;
        this.vulnerabilityMapper = vulnerabilityMapper;
    }

    @Override
    public List<SysCategory> listAll() {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<SysCategory>()
                        .orderByAsc(SysCategory::getSortOrder)
                        .orderByDesc(SysCategory::getId)
        );
    }

    @Override
    public SysCategory getById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("分类ID不合法");
        }

        SysCategory category = categoryMapper.selectById(id);

        if (category == null) {
            throw new IllegalArgumentException("分类不存在");
        }

        return category;
    }

    @Override
    @Transactional
    public void add(SysCategory category) {
        if (category == null || category.getName() == null
                || category.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("分类名称不能为空");
        }

        category.setName(category.getName().trim());

        Long count = categoryMapper.selectCount(
                new LambdaQueryWrapper<SysCategory>()
                        .eq(SysCategory::getName, category.getName())
        );

        if (count > 0) {
            throw new IllegalArgumentException("分类名称已存在");
        }

        if (category.getStatus() == null) {
            category.setStatus(1);
        }

        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }

        // 新增分类时不允许客户端指定数据库主键
        category.setId(null);

        categoryMapper.insert(category);
    }

    @Override
    @Transactional
    public void update(SysCategory category) {
        if (category == null || category.getId() == null) {
            throw new IllegalArgumentException("分类ID不能为空");
        }

        SysCategory oldCategory = getById(category.getId());

        if (category.getName() == null
                || category.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("分类名称不能为空");
        }

        String name = category.getName().trim();

        Long count = categoryMapper.selectCount(
                new LambdaQueryWrapper<SysCategory>()
                        .eq(SysCategory::getName, name)
                        .ne(SysCategory::getId, category.getId())
        );

        if (count > 0) {
            throw new IllegalArgumentException("分类名称已被使用");
        }

        oldCategory.setName(name);
        oldCategory.setType(category.getType());
        oldCategory.setDescription(category.getDescription());
        oldCategory.setSortOrder(category.getSortOrder());

        if (category.getStatus() != null) {
            oldCategory.setStatus(category.getStatus());
        }

        categoryMapper.updateById(oldCategory);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        // 先确认分类存在
        getById(id);

        // 检查是否有漏洞知识正在使用该分类
        Long count = vulnerabilityMapper.selectCount(
                new LambdaQueryWrapper<Vulnerability>()
                        .eq(Vulnerability::getCategoryId, id)
        );

        if (count != null && count > 0) {
            throw new IllegalArgumentException(
                    "该分类下存在漏洞知识，不能删除，请先处理关联的漏洞知识"
            );
        }

        categoryMapper.deleteById(id);
    }
}
