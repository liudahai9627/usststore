package com.usststore.auth.service;

import com.usststore.auth.client.UserClient;
import com.usststore.auth.config.JwtProperties;
import com.usststore.auth.entiy.UserInfo;
import com.usststore.auth.utils.JwtUtils;
import com.usststore.common.enums.ExceptionEnum;
import com.usststore.common.exception.UsException;
import com.usststore.user.pojo.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@EnableConfigurationProperties(JwtProperties.class)
public class AuthService {

    @Autowired
    private UserClient userClient;

    @Autowired
    private JwtProperties prop;

    public String login(String username, String password) {
        try{
            //校验用户名和密码
            User user = userClient.queryUser(username, password);
            if (user==null){
                throw new UsException(ExceptionEnum.INVALID_USERNAME_PASSWORD);
            }
            //生成token
            String token = JwtUtils.generateToken(new UserInfo(user.getId(), username), prop.getPrivateKey(), prop.getExpire());

            return token;
        }catch (Exception e){
            log.error("[授权中心] 用户名称或密码错误，用户名称:{}",username,e);
            throw new UsException(ExceptionEnum.INVALID_USERNAME_PASSWORD);
        }
    }
}
