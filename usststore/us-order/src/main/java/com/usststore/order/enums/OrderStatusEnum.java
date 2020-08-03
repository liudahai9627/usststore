package com.usststore.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum  OrderStatusEnum {
    UN_PAY(1,"未付款"),
    PAYED(2,"已付款，未发货"),
    DELIVERED(3,"已发货，未确认"),
    SUCCESS(4,"已确认，未评价"),
    CLOSE(5,"已关闭，交易失败"),
    RATED(6,"已评价"),
    ;
    private Integer code;
    private String msg;

    public Integer value(){
        return this.code;
    }
}
