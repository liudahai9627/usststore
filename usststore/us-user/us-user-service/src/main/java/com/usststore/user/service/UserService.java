package com.usststore.user.service;

import com.usststore.common.enums.ExceptionEnum;
import com.usststore.common.exception.UsException;
import com.usststore.common.utils.NumberUtils;
import com.usststore.user.mapper.UserMapper;
import com.usststore.user.pojo.User;
import com.usststore.user.utils.CodecUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX="user:verify:phone:";

    private Logger logger = LoggerFactory.getLogger(UserService.class);

    public Boolean checkData(String data, Integer type) {
        User user = new User();

        switch (type){
            case 1:
                user.setUsername(data);
                break;
            case 2:
                user.setPhone(data);
                break;
            default:
                throw new UsException(ExceptionEnum.INVALID_USER_DATA_TYPE);
        }

        int count = userMapper.selectCount(user);

        return count==0;
    }

    public void sendCode(String phone) {
        String key=KEY_PREFIX+phone;
        String code = NumberUtils.generateCode(6);
        Map<String, String> msg = new HashMap<>();
        msg.put("phone",phone);
        msg.put("code",code);
        amqpTemplate.convertAndSend("us.sms.exchange","sms.verify.code",msg);
        redisTemplate.opsForValue().set(key,code,5, TimeUnit.MINUTES);
    }

    public void register(User user, String code) {
        String key = KEY_PREFIX + user.getPhone();
        // 从redis取出验证码
        String cacheCode = redisTemplate.opsForValue().get(key);
        // 检查验证码是否正确
        if (!StringUtils.equals(code,cacheCode)){
            throw new UsException(ExceptionEnum.INVALID_VERIFY_CODE);
        }
        user.setCreated(new Date());
        // 生成盐
        String salt = CodecUtils.generateSalt();
        user.setSalt(salt);
        // 对密码进行加密
        user.setPassword(CodecUtils.md5Hex(user.getPassword(), salt));
        // 写入数据库
        userMapper.insert(user);
    }

    public User queryUser(String username, String password) {
        // 查询
        User record = new User();
        record.setUsername(username);
        User user = this.userMapper.selectOne(record);
        // 校验用户名
        if (user == null) {
            throw new UsException(ExceptionEnum.INVALID_USERNAME_PASSWORD);
        }
        // 校验密码
        if (!StringUtils.equals(user.getPassword(),CodecUtils.md5Hex(password, user.getSalt()))){
            throw new UsException(ExceptionEnum.INVALID_USERNAME_PASSWORD);
        }
        // 用户名密码都正确
        return user;
    }
}
