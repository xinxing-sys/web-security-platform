package com.websecurity.platform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.websecurity.platform.entity.Experiment;

public interface ExperimentService {

    IPage<Experiment> query(
            Integer pageNum,
            Integer pageSize,
            String keyword,
            Long categoryId,
            String difficulty,
            Integer status
    );

    Experiment getById(Long id);

    void add(Experiment experiment);

    void update(Experiment experiment);

    void delete(Long id);
}