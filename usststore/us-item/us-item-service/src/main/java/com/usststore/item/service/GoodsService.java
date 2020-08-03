package com.usststore.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.usststore.common.dto.CartDto;
import com.usststore.common.enums.ExceptionEnum;
import com.usststore.common.exception.UsException;
import com.usststore.common.vo.PageResult;
import com.usststore.item.mapper.SkuMapper;
import com.usststore.item.mapper.SpuDetailMapper;
import com.usststore.item.mapper.SpuMapper;
import com.usststore.item.mapper.StockMapper;
import com.usststore.item.pojo.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.beans.Transient;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GoodsService {

    @Autowired
    private SpuMapper spuMapper;
    @Autowired
    private SpuDetailMapper spuDetailMapper;
    @Autowired
    private SkuMapper skuMapper;
    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private CategoryService categoryService;
    @Autowired
    private BrandService brandService;

    @Autowired
    private AmqpTemplate amqpTemplate;

    public PageResult<Spu> querySpuByPage(Integer page, Integer rows, Boolean saleable, String key) {
        //分页
        PageHelper.startPage(page,rows);
        //过滤
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        //搜索字段过滤
        if (StringUtils.isNoneBlank(key))
        {
            criteria.andLike("title","%"+key+"%");
        }
        //上下架过滤
        if (saleable!=null){
            criteria.andEqualTo("saleable",saleable);
        }
        /*if (saleable==false||saleable==true){
            criteria.andEqualTo("saleable",saleable);
        }*/

        //默认排序
        example.setOrderByClause("last_update_time DESC");
        //查询
        List<Spu> spus = spuMapper.selectByExample(example);
        //判断
        if (CollectionUtils.isEmpty(spus)){
            throw new UsException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        //解析分类和品牌名称
        loadCnameAndBname(spus);
        //解析分页结果
        PageInfo<Spu> spuPageInfo = new PageInfo<>(spus);

        return new PageResult<>(spuPageInfo.getTotal(),spus);
    }

    private void loadCnameAndBname(List<Spu> spus){
        for (Spu spu : spus){
            //分类名称
            List<Category> categories = categoryService.queryByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
            List<String> names = categories.stream().map(Category::getName).collect(Collectors.toList());
            spu.setCname(StringUtils.join(names,"/"));
            //品牌名称
            String name = brandService.queryById(spu.getBrandId()).getName();
            spu.setBname(name);

        }
    }

    @Transactional
    public void saveGoods(Spu spu) {
        //新增Spu
        spu.setId(null);
        spu.setCreateTime(new Date());
        spu.setLastUpdateTime(spu.getCreateTime());
        spu.setSaleable(true);
        spu.setValid(false);
        int count = spuMapper.insert(spu);
        if (count!=1){
            throw new UsException(ExceptionEnum.GOODS_SAVE_ERROR);
        }
        //新增SpuDetail
        SpuDetail spuDetail = spu.getSpuDetail();
        spuDetail.setSpuId(spu.getId());
        spuDetailMapper.insert(spuDetail);
        saveSkuAndStock(spu);

        //发送mq消息
        amqpTemplate.convertAndSend("item.insert",spu.getId());
    }

    public void saveSkuAndStock(Spu spu){
        int count=0;
        //新增Sku
        ArrayList<Stock> list = new ArrayList<>();
        List<Sku> skus = spu.getSkus();
        for (Sku sku:skus){
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());
            sku.setSpuId(spu.getId());
            count = skuMapper.insert(sku);
            if (count!=1){
                throw new UsException(ExceptionEnum.GOODS_SAVE_ERROR);
            }
            //新增Stock
            Stock stock = new Stock();
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            /*count = stockMapper.insert(stock);*/
            list.add(stock);
        }
        //批量新增库存
        count=stockMapper.insertList(list);
        if (count!=list.size()){
            throw new UsException(ExceptionEnum.GOODS_SAVE_ERROR);
        }
    }

    public SpuDetail queryDetailById(Long spuId) {
        SpuDetail spuDetail = spuDetailMapper.selectByPrimaryKey(spuId);
        if (spuDetail==null){
            throw new UsException(ExceptionEnum.GOODS_DETAIL_NOT_FOUND);
        }
        return spuDetail;
    }

    public List<Sku> querySkuBySpuId(Long spuId) {
        //查询SKU
        Sku sku = new Sku();
        sku.setSpuId(spuId);
        List<Sku> skus = skuMapper.select(sku);
        if (CollectionUtils.isEmpty(skus)){
            throw new UsException(ExceptionEnum.GOODS_SKU_NOT_FOUND);
        }
        //查询库存
        /*for (Sku s : skus){
            Stock stock = stockMapper.selectByPrimaryKey(s.getId());
            if (stock==null){
                throw new UsException(ExceptionEnum.GOODS_STOCK_NOT_FOUND);
            }
            s.setStock(stock.getStock());
        }*/
        List<Long> ids = skus.stream().map(Sku::getId).collect(Collectors.toList());
        loadStockInSku(ids, skus);
        return skus;
    }

    @Transactional
    public void updateGoods(Spu spu) {
        if (spu.getId()==0){
            throw new UsException(ExceptionEnum.GOODS_ID_CANNOT_BE_NULL);
        }
        Sku sku = new Sku();
        sku.setSpuId(spu.getId());
        List<Sku> skuList = skuMapper.select(sku);
        if (!CollectionUtils.isEmpty(skuList)){
            //删除sku
            skuMapper.delete(sku);
            //删除stock
            List<Long> ids = skuList.stream().map(Sku::getId).collect(Collectors.toList());
            stockMapper.deleteByIdList(ids);
        }
        //修改spu
        spu.setValid(null);
        spu.setSaleable(null);
        spu.setLastUpdateTime(new Date());
        spu.setCreateTime(null);
        int count = spuMapper.updateByPrimaryKeySelective(spu);
        if (count!=1){
            throw new UsException(ExceptionEnum.GOODS_UPDATE_ERROR);
        }
        //修改spuDetail
        SpuDetail spuDetail = spu.getSpuDetail();
        spuDetail.setSpuId(spu.getId());
        spuDetailMapper.updateByPrimaryKeySelective(spuDetail);
        //新增sku和stock
        saveSkuAndStock(spu);

        System.out.println("=========");
        System.out.println("到这了");
        System.out.println("=========");

        //发送mq消息
        amqpTemplate.convertAndSend("item.update",spu.getId());
    }

    public Spu querySpuById(Long id) {
        //查spu
        Spu spu = spuMapper.selectByPrimaryKey(id);
        if (spu==null){
            throw new UsException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        //查sku
        List<Sku> skuList = querySkuBySpuId(id);
        spu.setSkus(skuList);
        //查询spudetail
        SpuDetail spuDetail = queryDetailById(id);
        spu.setSpuDetail(spuDetail);
        return spu;
    }

    public List<Sku> querySkuByIds(List<Long> ids) {
        List<Sku> skuList = skuMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(skuList)){
            throw new UsException(ExceptionEnum.GOODS_SKU_NOT_FOUND);
        }

        loadStockInSku(ids, skuList);
        return skuList;
    }

    private void loadStockInSku(List<Long> ids, List<Sku> skuList) {
        List<Stock> stocks = stockMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(stocks)){
            throw new UsException(ExceptionEnum.GOODS_STOCK_NOT_FOUND);
        }
        //把stock变成一个map，key为:sku的id，value是库存值
        Map<Long, Integer> stockMap = stocks.stream().collect(Collectors.toMap(Stock::getSkuId, Stock::getStock));
        skuList.forEach(s->s.setStock(stockMap.get(s.getId())));
    }

    @Transactional
    public void decreaseStock(List<CartDto> cartList) {

        for (CartDto cartDto : cartList) {
            Integer num = cartDto.getNum();
            Long skuId = cartDto.getSkuId();
            //减库存
            int count = stockMapper.decreaseStock(skuId, num);
            if (count != 1){
                throw new UsException(ExceptionEnum.STOCK_NOT_ENOUGH);
            }
        }
    }
}
