package com.usststore.order.service;

import com.usststore.auth.entiy.UserInfo;
import com.usststore.common.enums.ExceptionEnum;
import com.usststore.common.exception.UsException;
import com.usststore.common.utils.IdWorker;
import com.usststore.item.pojo.Sku;
import com.usststore.order.client.AddressClient;
import com.usststore.order.client.GoodsClient;
import com.usststore.order.dto.AddressDto;
import com.usststore.common.dto.CartDto;
import com.usststore.order.dto.OrderDto;
import com.usststore.order.enums.OrderStatusEnum;
import com.usststore.order.enums.PayState;
import com.usststore.order.interceptor.UserInterceptor;
import com.usststore.order.mapper.OrderDetailMapper;
import com.usststore.order.mapper.OrderMapper;
import com.usststore.order.mapper.OrderStatusMapper;
import com.usststore.order.pojo.Order;
import com.usststore.order.pojo.OrderDetail;
import com.usststore.order.pojo.OrderStatus;
import com.usststore.order.utils.PayHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private OrderStatusMapper orderStatusMapper;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private PayHelper payHelper;

    @Transactional
    public Long createOrder(OrderDto orderDto) {
        Order order = new Order();
        List<OrderDetail> detailList = new ArrayList<>();
        //创建订单编号
        long orderId = idWorker.nextId();
        order.setOrderId(orderId);
        order.setCreateTime(new Date());
        order.setPaymentType(orderDto.getPaymentType());
        //获取用户信息
        UserInfo user = UserInterceptor.getUser();
        order.setUserId(user.getId());
        order.setBuyerNick(user.getUsername());
        order.setBuyerRate(false);
        //收货人信息
        AddressDto addr = AddressClient.findById(orderDto.getAddressId());
        order.setReceiver(addr.getName());
        order.setReceiverAddress(addr.getAddress());
        order.setReceiverCity(addr.getCity());
        order.setReceiverDistrict(addr.getDistrict());
        order.setReceiverMobile(addr.getPhone());
        order.setReceiverState(addr.getState());
        order.setReceiverZip(addr.getZipCode());
        //金额
        Map<Long, Integer> numMap = orderDto.getCarts()
                .stream().collect(Collectors.toMap(CartDto::getSkuId, CartDto::getNum));
        Set<Long> ids = numMap.keySet();
        List<Sku> skuList = goodsClient.querySkuByIds(new ArrayList<>(ids));
        Long totalPay = 0L;
        for (Sku sku : skuList) {
            totalPay += sku.getPrice() * numMap.get(sku.getId());
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setImage(StringUtils.substringBefore(sku.getImages(),","));
            orderDetail.setNum(numMap.get(sku.getId()));
            orderDetail.setOrderId(orderId);
            orderDetail.setOwnSpec(sku.getOwnSpec());
            orderDetail.setPrice(sku.getPrice());
            orderDetail.setSkuId(sku.getId());
            orderDetail.setTitle(sku.getTitle());
            detailList.add(orderDetail);
        }
        order.setTotalPay(totalPay);
        order.setActualPay(totalPay + order.getPostFee() - 0);
        //写入数据库
        int count = orderMapper.insertSelective(order);
        if (count != 1){
            log.error("[创建订单] 创建订单失败，orderId:{}",orderId);
            throw new UsException(ExceptionEnum.CREATE_ORDER_ERROR);
        }
        //新增订单详情
        count = orderDetailMapper.insertList(detailList);
        if (count != detailList.size()){
            log.error("[创建订单] 创建订单详情失败，orderId:{}",orderId);
            throw new UsException(ExceptionEnum.CREATE_ORDER_DETAIL__ERROR);
        }
        //新增订单状态
        OrderStatus orderStatus = new OrderStatus();
        orderStatus.setCreateTime(order.getCreateTime());
        orderStatus.setOrderId(orderId);
        orderStatus.setStatus(OrderStatusEnum.UN_PAY.value());
        count = orderStatusMapper.insertSelective(orderStatus);
        if (count != 1){
            log.error("[创建订单] 创建订单状态失败，orderId:{}",orderId);
            throw new UsException(ExceptionEnum.CREATE_ORDER_STATUS__ERROR);
        }
        //减库存
        List<CartDto> cartList = orderDto.getCarts();
        goodsClient.decreaseStock(cartList);

        return orderId;
    }

    public Order queryOrderById(Long id) {
        //查询订单
        Order order = orderMapper.selectByPrimaryKey(id);
        if (order==null){
            throw new UsException(ExceptionEnum.ORDER_NOT_FOUND);
        }
        //查订单详情
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(id);
        List<OrderDetail> detailList = orderDetailMapper.select(orderDetail);
        if (CollectionUtils.isEmpty(detailList)){
            throw new UsException(ExceptionEnum.ORDER_DETAIL_NOT_FOUND);
        }
        order.setOrderDetails(detailList);
        //查订单状态
        OrderStatus status = orderStatusMapper.selectByPrimaryKey(id);
        if (status==null){
            throw new UsException(ExceptionEnum.ORDER_STATUS_NOT_FOUND);
        }
        order.setOrderStatus(status);

        return order;
    }

    public String createPayUrl(Long id) {
        //订单支付金额
        Order order = queryOrderById(id);
        //判断订单支付状态
        Integer status = order.getOrderStatus().getStatus();
        if (status != OrderStatusEnum.UN_PAY.value()){
            throw new UsException(ExceptionEnum.ORDER_STATUS_ERROR);
        }
        Long actualPay = order.getActualPay();
        actualPay=1L;
        //订单描述
        OrderDetail orderDetail = order.getOrderDetails().get(0);
        String desc = orderDetail.getTitle();
        String payUrl = payHelper.createOrder(id, actualPay, desc);
        return payUrl;
    }

    public void handleNotify(Map<String, String> result) {
        //1 数据校验
        payHelper.isSuccess(result);

        //2 校验签名
        payHelper.isValidSign(result);

        //3 校验金额
        String totalFeeStr = result.get("total_fee");
        //订单号
        String tradeNo = result.get("out_trade_no");
        if (StringUtils.isBlank(totalFeeStr) || StringUtils.isBlank(tradeNo)){
            throw new UsException(ExceptionEnum.INVALID_ORDER_PARAM);
        }
        //3.1 获取结果中的金额
        long totalFee = Long.valueOf(totalFeeStr);
        //获取订单号
        Long orderId = Long.valueOf(tradeNo);

        //4 查询订单
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if (totalFee != /*order.getActualPay()*/ 1L){
            //金额不符
            throw new UsException(ExceptionEnum.INVALID_ORDER_PARAM);
        }

        //5 修改订单状态
        OrderStatus status = new OrderStatus();
        status.setStatus(OrderStatusEnum.PAYED.value());
        status.setOrderId(orderId);
        status.setPaymentTime(new Date());
        int count =  orderStatusMapper.updateByPrimaryKeySelective(status);
        if (count != 1){
            throw new UsException(ExceptionEnum.UPDATE_ORDERSTATUS_ERROR);
        }

        log.info("[订单回调] 订单支付成功！订单编号:{}",orderId);
    }

    public PayState queryOrderState(Long id) {

        //查询订单状态
        OrderStatus orderStatus = orderStatusMapper.selectByPrimaryKey(id);
        Integer state = orderStatus.getStatus();
        //判断是否支付
        if(state != OrderStatusEnum.UN_PAY.value()){
            //如果已支付，就是真的支付了
            return PayState.SUCCESS;
        }
            /*Thread.sleep(2000L);
            orderStatus.setPaymentTime(new Date());
            orderStatusMapper.updateByPrimaryKeySelective(orderStatus);
            return PayState.SUCCESS;*/
        //如果未支付，需要到微信查询支付状态
        return payHelper.queryPayState(id);
    }
}
