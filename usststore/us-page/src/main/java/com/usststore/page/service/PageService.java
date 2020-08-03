package com.usststore.page.service;

import com.usststore.item.pojo.*;
import com.usststore.page.client.BrandClient;
import com.usststore.page.client.CategoryClient;
import com.usststore.page.client.GoodsClient;
import com.usststore.page.client.SpecificationClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;

import org.thymeleaf.context.Context;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PageService {

    @Autowired
    private BrandClient brandClient;
    @Autowired
    private CategoryClient categoryClient;
    @Autowired
    private SpecificationClient specificationClient;
    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private TemplateEngine templateEngine;

    public Map<String, Object> loadModel(Long spuId) {

        Map<String, Object> model = new HashMap<>();

        //查询spu
        Spu spu = goodsClient.querySpuById(spuId);
        //查sku
        List<Sku> skus = spu.getSkus();
        //查spudetail
        SpuDetail detail = spu.getSpuDetail();
        //查brand
        Brand brand = brandClient.queryBrandById(spu.getBrandId());
        //查categories
        List<Category> categories = categoryClient.queryCategoryByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
        //查规格参数
        List<SpecGroup> specs = specificationClient.queryGroupsAndParamsByCid(spu.getCid3());

        model.put("spu",spu);
        model.put("skus",skus);
        model.put("detail",detail);
        model.put("brand",brand);
        model.put("categories",categories);
        model.put("specs",specs);
        return model;
    }

    public void createHtml(Long spuId){
        //上下文
        Context context = new Context();
        context.setVariables(loadModel(spuId));
        //输出流
        File file = new File("F:\\IdeaProjects", spuId + ".html");

        if (file.exists()){
            file.delete();
        }

        try (PrintWriter writer = new PrintWriter(file, "UTF-8")){
            //生产html
            templateEngine.process("item",context,writer);
        }catch (Exception e){
            log.error("[静态页服务] 生成静态页异常!",e);
        }
    }

    public void deleteHtml(Long spuId) {
        File file = new File("F:\\IdeaProjects", spuId + ".html");

        if (file.exists()){
            file.delete();
        }
    }
}
