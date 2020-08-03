package com.usststore.order.web;

import com.usststore.order.dto.OrderDto;
import com.usststore.order.pojo.Order;
import com.usststore.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 创建订单
     * @param orderDto
     * @return
     */
    @PostMapping
    public ResponseEntity<Long> createOrder(@RequestBody OrderDto orderDto){
        //创建订单并返回订单id
        Long id=orderService.createOrder(orderDto);
        return ResponseEntity.ok(id);
    }

    /**
     * 根据订单id查询订单
     * @param id
     * @return
     */
    @GetMapping("{id}")
    public ResponseEntity<Order> queryOrderById(@PathVariable("id") Long id){
        return ResponseEntity.ok(orderService.queryOrderById(id));
    }

    /**
     * 根据订单id创建微信支付
     * @param id
     * @return
     */
    @GetMapping("/url/{id}")
    public ResponseEntity<String> createPayUrl(@PathVariable("id") Long id){
        return ResponseEntity.ok(orderService.createPayUrl(id));
    }

    /**
     * 根据订单id查询订单状态
     * @param id
     * @return
     */
    @GetMapping("state/{id}")
    public ResponseEntity<Integer> queryOrderState(@PathVariable("id") Long id){
        return ResponseEntity.ok(orderService.queryOrderState(id).getValue());
    }

}
