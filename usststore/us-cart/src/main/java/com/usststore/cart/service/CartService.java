package com.usststore.cart.service;

import com.usststore.auth.entiy.UserInfo;
import com.usststore.cart.interceptor.UserInterceptor;
import com.usststore.cart.pojo.Cart;
import com.usststore.common.enums.ExceptionEnum;
import com.usststore.common.exception.UsException;
import com.usststore.common.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    private static final String KEY_PREFIX="cart:user:id:";

    @Autowired
    private RedisTemplate redisTemplate;

    public void addCart(Cart cart) {

        //获取登陆的用户
        UserInfo user = UserInterceptor.getUser();
        //获取用户id
        String key=KEY_PREFIX+user.getId();
        //获取商品id
        String hashKey = cart.getSkuId().toString();
        Integer num=cart.getNum();
        BoundHashOperations<String,Object,Object> ops = redisTemplate.boundHashOps(key);
        //判断当前购物车商品是否存在
        if (ops.hasKey(hashKey)){
            //商品已存在，修改数量
            String jsonCart = ops.get(hashKey).toString();
            cart = JsonUtils.toBean(jsonCart, Cart.class);
            cart.setNum(cart.getNum()+num);

        }
        //写回redis
            ops.put(hashKey,JsonUtils.toString(cart));
    }

    public List<Cart> queryCartList() {
        //获取登陆的用户
        UserInfo user = UserInterceptor.getUser();
        //获取用户id
        String key=KEY_PREFIX+user.getId();
        if (!redisTemplate.hasKey(key)){
            throw new UsException(ExceptionEnum.CART_NOT_FOUND);
        }
        BoundHashOperations<String,Object,Object> ops = redisTemplate.boundHashOps(key);
        List<Cart> cartList = ops.values().stream().map(o -> JsonUtils.toBean(o.toString(), Cart.class))
                .collect(Collectors.toList());
        return cartList;
    }

    public void updateCartNum(Long skuId, Integer num) {
        //获取登陆的用户
        UserInfo user = UserInterceptor.getUser();
        //获取用户id
        String key=KEY_PREFIX+user.getId();
        //获取商品id
        String hashKey = skuId.toString();
        BoundHashOperations<String,Object,Object> ops = redisTemplate.boundHashOps(key);
        if (!ops.hasKey(skuId.toString())){
            throw new UsException(ExceptionEnum.CART_NOT_FOUND);
        }
        //查询
        Cart cart = JsonUtils.toBean(ops.get(skuId.toString()).toString(), Cart.class);
        cart.setNum(num);
        //写入redis
        ops.put(hashKey,JsonUtils.toString(cart));
    }

    public void deleteCart(Long skuId) {
        //获取登陆的用户
        UserInfo user = UserInterceptor.getUser();
        //获取用户id
        String key=KEY_PREFIX+user.getId();
        //删除
        redisTemplate.opsForHash().delete(key,skuId.toString());
    }
}
