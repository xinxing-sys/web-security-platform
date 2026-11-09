package com.websecurity.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.websecurity.platform.common.ApiResponse;
import com.websecurity.platform.entity.Experiment;
import com.websecurity.platform.service.ExperimentService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/experiments")
public class ExperimentController {

    private final ExperimentService experimentService;

    public ExperimentController(
            ExperimentService experimentService) {
        this.experimentService = experimentService;
    }

    /**
     * 学生查询已启用的实验资源
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> query(
            @RequestParam(required = false, defaultValue = "1")
            Integer pageNum,

            @RequestParam(required = false, defaultValue = "10")
            Integer pageSize,

            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            Long categoryId,

            @RequestParam(required = false)
            String difficulty) {

        try {
            IPage<Experiment> page = experimentService.query(
                    pageNum,
                    pageSize,
                    keyword,
                    categoryId,
                    difficulty,
                    1
            );

            Map<String, Object> result = new LinkedHashMap<>();

            result.put("pageNum", page.getCurrent());
            result.put("pageSize", page.getSize());
            result.put("total", page.getTotal());
            result.put("totalPages", page.getPages());
            result.put("records", page.getRecords());

            return ApiResponse.success(
                    "查询实验资源成功",
                    result
            );

        } catch (Exception e) {
            return ApiResponse.error("查询实验资源失败");
        }
    }

    /**
     * 学生查询实验详情
     */
    @GetMapping("/{id}")
    public ApiResponse<Experiment> getById(
            @PathVariable Long id) {

        try {
            Experiment experiment =
                    experimentService.getById(id);

            if (experiment.getStatus() == null
                    || experiment.getStatus() != 1) {
                return ApiResponse.error("该实验当前不可用");
            }

            return ApiResponse.success(
                    "获取实验详情成功",
                    experiment
            );

        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());

        } catch (Exception e) {
            return ApiResponse.error("获取实验详情失败");
        }
    }
}