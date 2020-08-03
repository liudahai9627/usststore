package com.usststore.item.mapper;

import com.usststore.item.pojo.Category;
import tk.mybatis.mapper.additional.idlist.IdListMapper;
import tk.mybatis.mapper.common.Mapper;


public interface CategoryMapper extends Mapper<Category>,IdListMapper<Category,Long> {
}
