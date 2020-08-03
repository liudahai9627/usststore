package com.usststore.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.usststore.common.utils.JsonUtils;
import com.usststore.common.vo.PageResult;
import com.usststore.item.pojo.*;
import com.usststore.search.client.BrandClient;
import com.usststore.search.client.CategoryClient;
import com.usststore.search.client.GoodsClient;
import com.usststore.search.client.SpecificationClient;
import com.usststore.search.pojo.Goods;
import com.usststore.search.pojo.SearchRequest;
import com.usststore.search.pojo.SearchResult;
import com.usststore.search.repository.GoodsRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {

    @Autowired
    private CategoryClient categoryClient;
    @Autowired
    private BrandClient brandClient;
    @Autowired
    private GoodsClient goodsClient;
    @Autowired
    private SpecificationClient specificationClient;

    @Autowired
    private GoodsRepository goodsRepository;
    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    public Goods buildGoods(Spu spu){
        //查询分类
        List<Category> categorys = categoryClient.queryCategoryByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
        List<String> names = categorys.stream().map(Category::getName).collect(Collectors.toList());
        //查询品牌
        Brand brand = brandClient.queryBrandById(spu.getBrandId());
        //搜索字段
        String all=spu.getTitle()+ StringUtils.join(names," ")+brand.getName();
        //查询sku
        List<Sku> skuList = goodsClient.querySkuBySpuId(spu.getId());
        List<Map<String,Object>> skus=new ArrayList<>();
        Set<Long> priceSet = new HashSet<>();
        for (Sku sku : skuList) {
            Map<String, Object> map = new HashMap<>();
            map.put("id",sku.getId());
            map.put("title",sku.getTitle());
            map.put("price",sku.getPrice());
            map.put("image",StringUtils.substringBefore(sku.getImages(),","));
            skus.add(map);
            priceSet.add(sku.getPrice());
        }
        //查询规格参数
        List<SpecParam> specParams = specificationClient.queryParamsList(null, spu.getCid3(), true);
        //查询商品详情
        SpuDetail spuDetail = goodsClient.queryDetailById(spu.getId());
        //获取通用规格参数
        String genericSpecJson = spuDetail.getGenericSpec();
        Map<Long, String> genericSpec = JsonUtils.toMap(genericSpecJson, Long.class, String.class);
        //获取特有规格参数
        String specialSpecJson = spuDetail.getSpecialSpec();
        Map<Long, List<String>> specialSpec = JsonUtils.nativeRead(specialSpecJson, new TypeReference<Map<Long, List<String>>>() {
        });
        //规格参数，key是规格参数的名字，value是规格参数的值
        Map<String, Object> specs = new HashMap<>();
        for (SpecParam specParam : specParams) {
            String key=specParam.getName();
            Object value="";
            //判断是否是通用规格
            if (specParam.getGeneric()){
                value=genericSpec.get(specParam.getId());
                //判断是否是数值类型
                if (specParam.getNumeric()){
                    value=chooseSegment(value.toString(),specParam);
                }
            }else {
                value=specialSpec.get(specParam.getId());
            }
            specs.put(key,value);
        }
        //构建goods对象
        Goods goods = new Goods();
        goods.setBrandId(spu.getBrandId());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setCreateTime(spu.getCreateTime());
        goods.setId(spu.getId());
        goods.setAll(all);//搜索字段,标题+分类+品牌+规格等
        goods.setPrice(priceSet);//所有sku价格的集合
        goods.setSkus(JsonUtils.toString(skuList));//所有sku的集合的json格式
        goods.setSpecs(specs);//所有的可搜索的规格字段
        goods.setSubTitle(spu.getSubTitle());
        return goods;
    }

    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if(segs.length == 2){
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if(val >= begin && val < end){
                if(segs.length == 1){
                    result = segs[0] + p.getUnit() + "以上";
                }else if(begin == 0){
                    result = segs[1] + p.getUnit() + "以下";
                }else{
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }

    public PageResult<Goods> search(SearchRequest searchRequest) {
        String key = searchRequest.getKey();
        if (StringUtils.isBlank(key)){
            return null;
        }
        int page=searchRequest.getPage()-1;
        int size=searchRequest.getSize();
        //创建查询构建器
        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder();
        //结果过滤
        searchQueryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id","subTitle","skus"},null));
        //分页
        searchQueryBuilder.withPageable(PageRequest.of(page,size));
        //过滤
        //searchQueryBuilder.withQuery(QueryBuilders.matchQuery("all",searchRequest.getKey()));
        //搜索条件
        //QueryBuilder basicQuery = QueryBuilders.matchQuery("all", key);
        QueryBuilder basicQuery =buildBasicQuery(searchRequest);
        searchQueryBuilder.withQuery(basicQuery);
        //聚合分类和品牌
        String categoryAggName="category_agg";
        searchQueryBuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("cid3"));
        String brandAggName="brand_agg";
        searchQueryBuilder.addAggregation(AggregationBuilders.terms(brandAggName).field("brandId"));
        //查询
        //Page<Goods> goodsPageResult = goodsRepository.search(searchQueryBuilder.build());
        AggregatedPage<Goods> goodsPageResult = elasticsearchTemplate.queryForPage(searchQueryBuilder.build(), Goods.class);
        //解析分页结果
        long totalElements = goodsPageResult.getTotalElements();
        int totalPages = goodsPageResult.getTotalPages();
        List<Goods> goodsList = goodsPageResult.getContent();
        //解析聚合结果
        Aggregations aggregations = goodsPageResult.getAggregations();
        List<Category> categories=parseCategoryAgg(aggregations.get(categoryAggName));
        List<Brand> brands=parseBrandAgg(aggregations.get(brandAggName));
        //规格参数聚合
        List<Map<String,Object>> specs=null;
        if (categories!=null && categories.size()==1){
            specs=buildSpecificationAgg(categories.get(0).getId(),basicQuery);
        }

        //return new PageResult<>(totalElements,totalPages,goodsList);
        return new SearchResult(totalElements,totalPages,goodsList,categories,brands,specs);
    }

    private QueryBuilder buildBasicQuery(SearchRequest searchRequest) {
        //创建布尔查询
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //查询条件
        boolQueryBuilder.must(QueryBuilders.matchQuery("all",searchRequest.getKey()));
        //过滤条件
        Map<String,String> map=searchRequest.getFilter();
        for (Map.Entry<String,String> entry : map.entrySet()){
            String key = entry.getKey();
            if (!"cid3".equals(key) && !"brandId".equals(key)){
                key="specs."+key+".keyword";
            }
            String value = entry.getValue();
            boolQueryBuilder.filter(QueryBuilders.termQuery(key,value));
            System.out.println("======================================");
            System.out.println(key);
            System.out.println(value);
            System.out.println("======================================");
        }
        return boolQueryBuilder;
    }

    private List<Map<String,Object>> buildSpecificationAgg(Long cid, QueryBuilder basicQuery) {
        List<Map<String,Object>> specs=new ArrayList<>();
        //查询需要聚合的规格参数
        List<SpecParam> specParams = specificationClient.queryParamsList(null, cid, true);
        //聚合
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        queryBuilder.withQuery(basicQuery);
        for (SpecParam specParam : specParams) {
            String name = specParam.getName();
            queryBuilder.addAggregation(AggregationBuilders.terms(name).field("specs."+name+".keyword"));
        }
        //获取结果
        AggregatedPage<Goods> result = elasticsearchTemplate.queryForPage(queryBuilder.build(), Goods.class);
        //解析结果
        Aggregations aggregations = result.getAggregations();
        for (SpecParam specParam : specParams) {
            String name = specParam.getName();
            StringTerms terms = aggregations.get(name);
            List<String> options = terms.getBuckets().stream().map(b -> b.getKeyAsString()).collect(Collectors.toList());
            Map<String, Object> map = new HashMap<>();
            map.put("k",name);
            map.put("options",options);
            specs.add(map);
        }
        return specs;
    }

    private List<Category> parseCategoryAgg(LongTerms terms){
        try{
            List<Long> ids = terms.getBuckets().stream().map(b -> b.getKeyAsNumber().longValue()).collect(Collectors.toList());
            List<Category> categoryList = categoryClient.queryCategoryByIds(ids);
            return categoryList;
        }catch (Exception e){
            return null;
        }
    }
    private List<Brand> parseBrandAgg(LongTerms terms){
        try{
            List<Long> ids = terms.getBuckets().stream().map(b -> b.getKeyAsNumber().longValue()).collect(Collectors.toList());
            List<Brand> brandList = brandClient.queryBrandsByIds(ids);
            return brandList;
        }catch (Exception e){
            return null;
        }
    }

    public void creatOrUpdateIndex(Long spuId) {
        //查询spu
        Spu spu = goodsClient.querySpuById(spuId);
        //构建goods
        Goods goods = buildGoods(spu);
        //存入索引
        goodsRepository.save(goods);
    }

    public void deleteIndex(Long spuId) {
        goodsRepository.deleteById(spuId);
    }
}
