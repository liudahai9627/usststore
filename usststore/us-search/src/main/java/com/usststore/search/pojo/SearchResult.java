package com.usststore.search.pojo;

import com.usststore.common.vo.PageResult;
import com.usststore.item.pojo.Brand;
import com.usststore.item.pojo.Category;
import org.elasticsearch.common.MacAddressProvider;

import java.util.List;
import java.util.Map;

public class SearchResult extends PageResult<Goods> {

    private List<Category> categoryList;
    private List<Brand> brandList;
    private List<Map<String,Object>> specs;//规格参数 key及待选项

    public SearchResult(){}
    public SearchResult(Long total,Integer totalPage,List<Goods> items,List<Category> categoryList,List<Brand> brandList){
        super(total,totalPage,items);
        this.categoryList=categoryList;
        this.brandList=brandList;
    }
    public SearchResult(Long total,Integer totalPage,List<Goods> items,List<Category> categoryList,List<Brand> brandList,List<Map<String,Object>> specs){
        super(total,totalPage,items);
        this.categoryList=categoryList;
        this.brandList=brandList;
        this.specs=specs;
    }

    public List<Category> getCategoryList() {
        return categoryList;
    }

    public void setCategoryList(List<Category> categoryList) {
        this.categoryList = categoryList;
    }

    public List<Brand> getBrandList() {
        return brandList;
    }

    public void setBrandList(List<Brand> brandList) {
        this.brandList = brandList;
    }

    public List<Map<String, Object>> getSpecs() {
        return specs;
    }

    public void setSpecs(List<Map<String, Object>> specs) {
        this.specs = specs;
    }
}
