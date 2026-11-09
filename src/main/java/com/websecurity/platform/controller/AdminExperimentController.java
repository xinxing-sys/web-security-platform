package com.websecurity.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.websecurity.platform.common.ApiResponse;
import com.websecurity.platform.entity.Experiment;
import com.websecurity.platform.service.ExperimentService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/experiments")
public class AdminExperimentController {

    private final ExperimentService experimentService;

    public AdminExperimentController(
            ExperimentService experimentService) {
        this.experimentService = experimentService;
    }

    /**
     * 管理员分页查询实验资源
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
            String difficulty,

            @RequestParam(required = false)
            Integer status) {

        try {
            IPage<Experiment> page = experimentService.query(
                    pageNum,
                    pageSize,
                    keyword,
                    categoryId,
                    difficulty,
                    status
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
     * 查询实验详情
     */
    @GetMapping("/{id}")
    public ApiResponse<Experiment> getById(
            @PathVariable Long id) {

        try {
            return ApiResponse.success(
                    "获取实验详情成功",
                    experimentService.getById(id)
            );

        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());

        } catch (Exception e) {
            return ApiResponse.error("获取实验详情失败");
        }
    }

    /**
     * 新增实验
     */
    @PostMapping
    public ApiResponse<?> add(
            @RequestBody Experiment experiment) {

        try {
            experimentService.add(experiment);

            return ApiResponse.success(
                    "新增实验成功",
                    null
            );

        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());

        } catch (Exception e) {
            return ApiResponse.error("新增实验失败");
        }
    }

    /**
     * 修改实验
     */
    @PutMapping("/{id}")
    public ApiResponse<?> update(
            @PathVariable Long id,
            @RequestBody Experiment experiment) {

        try {
            experiment.setId(id);

            experimentService.update(experiment);

            return ApiResponse.success(
                    "修改实验成功",
                    experimentService.getById(id)
            );

        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());

        } catch (Exception e) {
            return ApiResponse.error("修改实验失败");
        }
    }

    /**
     * 删除实验
     */
    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(
            @PathVariable Long id) {

        try {
            experimentService.delete(id);

            return ApiResponse.success(
                    "删除实验成功",
                    null
            );

        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());

        } catch (Exception e) {
            return ApiResponse.error("删除实验失败");
        }
    }
}