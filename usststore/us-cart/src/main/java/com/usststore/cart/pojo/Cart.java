package com.usststore.cart.pojo;

import lombok.Data;

@Data
public class Cart {
    private Long skuId;
    private Long price;
    private String title;
    private String image;
    private String ownSpec;
    private Integer num;
}
