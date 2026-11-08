package com.websecurity.platform.service;

import com.websecurity.platform.entity.SysCategory;

import java.util.List;

public interface CategoryService {

    List<SysCategory> listAll();

    SysCategory getById(Long id);

    void add(SysCategory category);

    void update(SysCategory category);

    void delete(Long id);
}