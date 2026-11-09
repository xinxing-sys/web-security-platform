package com.websecurity.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.websecurity.platform.entity.Experiment;
import com.websecurity.platform.entity.ExperimentTask;
import com.websecurity.platform.entity.SysCategory;
import com.websecurity.platform.mapper.ExperimentMapper;
import com.websecurity.platform.mapper.ExperimentTaskMapper;
import com.websecurity.platform.mapper.SysCategoryMapper;
import com.websecurity.platform.service.ExperimentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExperimentServiceImpl implements ExperimentService {

    private final ExperimentMapper experimentMapper;
    private final ExperimentTaskMapper taskMapper;
    private final SysCategoryMapper categoryMapper;

    public ExperimentServiceImpl(
            ExperimentMapper experimentMapper,
            ExperimentTaskMapper taskMapper,
            SysCategoryMapper categoryMapper) {
        this.experimentMapper = experimentMapper;
        this.taskMapper = taskMapper;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public IPage<Experiment> query(
            Integer pageNum,
            Integer pageSize,
            String keyword,
            Long categoryId,
            String difficulty,
            Integer status) {

        int current = (pageNum == null || pageNum < 1)
                ? 1 : pageNum;

        int size = (pageSize == null || pageSize < 1)
                ? 10 : Math.min(pageSize, 100);

        LambdaQueryWrapper<Experiment> wrapper =
                new LambdaQueryWrapper<>();

        wrapper.like(
                keyword != null && !keyword.trim().isEmpty(),
                Experiment::getName,
                keyword == null ? null : keyword.trim()
        );

        wrapper.eq(
                categoryId != null,
                Experiment::getCategoryId,
                categoryId
        );

        wrapper.eq(
                difficulty != null && !difficulty.trim().isEmpty(),
                Experiment::getDifficulty,
                difficulty == null ? null : difficulty.trim()
        );

        wrapper.eq(
                status != null,
                Experiment::getStatus,
                status
        );

        wrapper.orderByDesc(Experiment::getCreatedAt);

        return experimentMapper.selectPage(
                new Page<>(current, size),
                wrapper
        );
    }

    @Override
    public Experiment getById(Long id) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("实验ID不合法");
        }

        Experiment experiment = experimentMapper.selectById(id);

        if (experiment == null) {
            throw new IllegalArgumentException("实验资源不存在");
        }

        return experiment;
    }

    @Override
    @Transactional
    public void add(Experiment experiment) {

        validate(experiment);

        checkCategory(experiment.getCategoryId());

        experiment.setId(null);

        if (experiment.getStatus() == null) {
            experiment.setStatus(1);
        }

        if (experiment.getPlatformType() == null
                || experiment.getPlatformType().trim().isEmpty()) {
            experiment.setPlatformType("WEBGOAT");
        }

        experimentMapper.insert(experiment);
    }

    @Override
    @Transactional
    public void update(Experiment experiment) {

        if (experiment == null || experiment.getId() == null) {
            throw new IllegalArgumentException("实验ID不能为空");
        }

        getById(experiment.getId());

        validate(experiment);

        checkCategory(experiment.getCategoryId());

        experimentMapper.updateById(experiment);
    }

    @Override
    @Transactional
    public void delete(Long id) {

        getById(id);

        Long taskCount = taskMapper.selectCount(
                new LambdaQueryWrapper<ExperimentTask>()
                        .eq(ExperimentTask::getExperimentId, id)
        );

        if (taskCount != null && taskCount > 0) {
            throw new IllegalArgumentException(
                    "该实验已有学生领取，不能删除，请先停用该实验"
            );
        }

        experimentMapper.deleteById(id);
    }

    /**
     * 检查实验分类是否存在且属于实验分类
     */
    private void checkCategory(Long categoryId) {

        if (categoryId == null) {
            return;
        }

        SysCategory category = categoryMapper.selectById(categoryId);

        if (category == null) {
            throw new IllegalArgumentException("实验分类不存在");
        }

        if (!"EXPERIMENT".equals(category.getType())) {
            throw new IllegalArgumentException(
                    "只能选择实验类型的分类"
            );
        }
    }

    /**
     * 校验实验数据
     */
    private void validate(Experiment experiment) {

        if (experiment == null) {
            throw new IllegalArgumentException("实验数据不能为空");
        }

        if (experiment.getName() == null
                || experiment.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("实验名称不能为空");
        }

        if (experiment.getName().trim().length() > 200) {
            throw new IllegalArgumentException(
                    "实验名称不能超过200个字符"
            );
        }

        experiment.setName(experiment.getName().trim());

        String difficulty = experiment.getDifficulty();

        if (difficulty == null || difficulty.trim().isEmpty()) {
            experiment.setDifficulty("BEGINNER");
        } else {
            difficulty = difficulty.trim().toUpperCase();

            if (!"BEGINNER".equals(difficulty)
                    && !"INTERMEDIATE".equals(difficulty)
                    && !"ADVANCED".equals(difficulty)) {
                throw new IllegalArgumentException(
                        "实验难度只能是BEGINNER、INTERMEDIATE或ADVANCED"
                );
            }

            experiment.setDifficulty(difficulty);
        }

        if (experiment.getStatus() != null
                && experiment.getStatus() != 0
                && experiment.getStatus() != 1) {
            throw new IllegalArgumentException(
                    "实验状态只能是0或1"
            );
        }

        if (experiment.getPlatformType() != null
                && experiment.getPlatformType().trim().length() > 30) {
            throw new IllegalArgumentException(
                    "靶场类型不能超过30个字符"
            );
        }

        if (experiment.getPlatformUrl() != null
                && experiment.getPlatformUrl().length() > 500) {
            throw new IllegalArgumentException(
                    "靶场入口不能超过500个字符"
            );
        }

        if (experiment.getLessonCode() != null
                && experiment.getLessonCode().length() > 100) {
            throw new IllegalArgumentException(
                    "靶场关卡标识不能超过100个字符"
            );
        }
    }
}