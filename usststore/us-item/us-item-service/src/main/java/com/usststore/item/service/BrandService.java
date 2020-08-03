package com.usststore.item.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.usststore.common.enums.ExceptionEnum;
import com.usststore.common.exception.UsException;
import com.usststore.common.vo.PageResult;
import com.usststore.item.mapper.BrandMapper;
import com.usststore.item.pojo.Brand;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class BrandService {

    @Autowired
    private BrandMapper brandMapper;

    public PageResult<Brand> queryBrandByPage(Integer page, Integer rows, String sortBy, Boolean desc, String key) {

        //分页
        PageHelper.startPage(page,rows);

        //过滤
        Example example = new Example(Brand.class);
        if (StringUtils.isNoneBlank(key)){
            example.createCriteria().orLike("name","%"+key+"%")
                    .orEqualTo("letter",key.toUpperCase());
        }

        //排序
        if (StringUtils.isNoneBlank(sortBy)){
            String orderByClause=sortBy+(desc ? " DESC" : " ASC");
            example.setOrderByClause(orderByClause);
        }

        //查询
        List<Brand> list=brandMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(list)){
            throw new UsException(ExceptionEnum.BRAND_NOT_FOUND);
        }

        //解析分页结果
        PageInfo<Brand> brandPageInfo = new PageInfo<>(list);

        return new PageResult<>(brandPageInfo.getTotal(),list);
    }

    @Transactional
    public void saveBrand(Brand brand, List<Long> cids) {
        brand.setId(null);
        int count = brandMapper.insert(brand);
        if (count!=1){
            throw new UsException(ExceptionEnum.BRAND_SAVE_ERROR);
        }
        for (Long cid : cids){
            count=brandMapper.insertCategoryBrand(cid,brand.getId());
            if (count !=1 ){
                throw new UsException(ExceptionEnum.BRAND_SAVE_ERROR);
            }
        }
    }

    public Brand queryById(Long id){
        Brand brand = brandMapper.selectByPrimaryKey(id);
        if (brand==null){
            throw new UsException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return brand;
    }


    public List<Brand> queryBrandByCid(Long cid) {
        List<Brand> list = brandMapper.queryByCategoryId(cid);
        if (CollectionUtils.isEmpty(list)){
            throw new UsException(ExceptionEnum.BRAND_NOT_FOUND);
        }

        return list;
    }

    public List<Brand> queryBrandsByIds(List<Long> ids) {

        List<Brand> brands = brandMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(brands)){
            throw new UsException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return brands;
    }
}
