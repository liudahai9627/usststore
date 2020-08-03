package com.usststore.item.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Date;
import java.util.List;

@Table(name = "tb_spu")
@Data
public class Spu {
    @Id
    @KeySql(useGeneratedKeys = true)
    private Long id;
    private Long brandId;//品牌id
    private Long cid1;// 1级分类目录
    private Long cid2;// 2级分类目录
    private Long cid3;// 3级分类目录
    private String title;// 标题
    private String subTitle;// 子标题
    private Boolean saleable;// 是否上架
    private Boolean valid;// 是否有效，逻辑删除用

    @JsonIgnore
    private Date createTime;// 创建时间
    @JsonIgnore//返回页面时忽略该字段
    private Date lastUpdateTime;// 最后修改时间

    @Transient  //数据库没有该字段，忽略
    private String cname;
    @Transient
    private String bname;
    @Transient
    private List<Sku> skus;
    @Transient
    private SpuDetail spuDetail;

}

