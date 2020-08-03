package com.usststore.item.service;

import com.usststore.common.enums.ExceptionEnum;
import com.usststore.common.exception.UsException;
import com.usststore.item.mapper.CategoryMapper;
import com.usststore.item.pojo.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    public List<Category> queryCategoryListByPid(Long pid) {
        Category category = new Category();
        category.setParentId(pid);
        //categoryMapper依据category中的非空属性pid查找category
        List<Category> categoryList = categoryMapper.select(category);
        if (CollectionUtils.isEmpty(categoryList)){
            throw new UsException(ExceptionEnum.CATEGORY_NOT_FOUND);
        }
        return categoryList;
    }

    public List<Category> queryByIds(List<Long> ids){
        List<Category> categories = categoryMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(categories)){
            throw new UsException(ExceptionEnum.CATEGORY_NOT_FOUND);
        }
        return categories;
    }
}
