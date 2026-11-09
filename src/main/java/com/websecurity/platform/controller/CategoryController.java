
package com.websecurity.platform.controller;

import com.websecurity.platform.common.ApiResponse;
import com.websecurity.platform.entity.SysCategory;
import com.websecurity.platform.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * 学生查询已启用的漏洞分类
     * GET /api/categories
     */
    @GetMapping
    public ApiResponse<List<SysCategory>> listEnabledCategories() {
        try {
            List<SysCategory> categories = categoryService.listAll()
                    .stream()
                    .filter(category ->
                            category.getStatus() != null
                                    && category.getStatus() == 1)
                    .collect(Collectors.toList());

            return ApiResponse.success(
                    "查询分类列表成功",
                    categories
            );

        } catch (Exception e) {
            return ApiResponse.error("查询分类列表失败");
        }
    }

    /**
     * 学生查询指定分类
     * GET /api/categories/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<SysCategory> getCategory(
            @PathVariable Long id) {
        try {
            SysCategory category = categoryService.getById(id);

            if (category.getStatus() == null
                    || category.getStatus() != 1) {
                return ApiResponse.error("该分类暂不可查看");
            }

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
}
