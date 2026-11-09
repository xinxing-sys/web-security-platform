
package com.websecurity.platform.controller;

import com.websecurity.platform.common.ApiResponse;
import com.websecurity.platform.entity.SysCategory;
import com.websecurity.platform.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * 查询全部分类
     * GET /api/admin/categories
     */
    @GetMapping
    public ApiResponse<List<SysCategory>> listCategories() {
        try {
            List<SysCategory> categories = categoryService.listAll();

            return ApiResponse.success(
                    "查询分类列表成功",
                    categories
            );
        } catch (Exception e) {
            return ApiResponse.error("查询分类列表失败");
        }
    }

    /**
     * 查询分类详情
     * GET /api/admin/categories/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<SysCategory> getCategory(
            @PathVariable Long id) {
        try {
            SysCategory category = categoryService.getById(id);

            return ApiResponse.success(
                    "获取分类详情成功",
                    category
            );
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("获取分类详情失败");
        }
    }

    /**
     * 新增分类
     * POST /api/admin/categories
     */
    @PostMapping
    public ApiResponse<?> addCategory(
            @RequestBody SysCategory category) {
        try {
            categoryService.add(category);

            return ApiResponse.success(
                    "新增分类成功",
                    null
            );
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("新增分类失败");
        }
    }

    /**
     * 修改分类
     * PUT /api/admin/categories/{id}
     */
    @PutMapping("/{id}")
    public ApiResponse<?> updateCategory(
            @PathVariable Long id,
            @RequestBody SysCategory category) {
        try {
            category.setId(id);

            categoryService.update(category);

            return ApiResponse.success(
                    "修改分类成功",
                    categoryService.getById(id)
            );
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("修改分类失败");
        }
    }

    /**
     * 删除分类
     * DELETE /api/admin/categories/{id}
     */
    @DeleteMapping("/{id}")
    public ApiResponse<?> deleteCategory(
            @PathVariable Long id) {
        try {
            categoryService.delete(id);

            return ApiResponse.success(
                    "删除分类成功",
                    null
            );
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("删除分类失败");
        }
    }
}
