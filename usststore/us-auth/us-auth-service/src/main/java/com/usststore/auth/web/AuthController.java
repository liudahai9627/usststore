package com.usststore.auth.web;

import com.usststore.auth.config.JwtProperties;
import com.usststore.auth.entiy.UserInfo;
import com.usststore.auth.service.AuthService;
import com.usststore.auth.utils.JwtUtils;
import com.usststore.common.enums.ExceptionEnum;
import com.usststore.common.exception.UsException;
import com.usststore.common.utils.CookieUtils;
import com.usststore.user.pojo.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@EnableConfigurationProperties(JwtProperties.class)
public class AuthController {
    @Autowired
    private AuthService authService;

    @Value("${us.jwt.cookieName}")
    private String cookieName;

    @Autowired
    private JwtProperties prop;

    @PostMapping("login")
    public ResponseEntity<Void> login(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            HttpServletRequest request,
            HttpServletResponse response){
        //登录,并返回token
        String token=authService.login(username,password);
        //写入cookie
        CookieUtils.newBuilder(response).httpOnly().request(request)
                .build(cookieName,token);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("verify")
    public ResponseEntity<UserInfo> verify(
            @CookieValue("US_TOKEN") String token,
            HttpServletRequest request,
            HttpServletResponse response){
        if (StringUtils.isBlank(token)){
            throw new UsException(ExceptionEnum.UNAUTHORIZED);
        }
        try{
            //解析token
            UserInfo info = JwtUtils.getInfoFromToken(token, prop.getPublicKey());
            //刷新token，重新生成token
            String newToken = JwtUtils.generateToken(info, prop.getPrivateKey(), prop.getExpire());
            //写入cookie
            CookieUtils.newBuilder(response).httpOnly().request(request);
            return ResponseEntity.ok(info);
        }catch (Exception e){
            //token过期或token被篡改
            throw new UsException(ExceptionEnum.UNAUTHORIZED);
        }
    }
}
