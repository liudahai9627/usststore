package com.usststore.auth;

import com.usststore.auth.entiy.UserInfo;
import com.usststore.auth.utils.JwtUtils;
import com.usststore.auth.utils.RsaUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.security.PrivateKey;
import java.security.PublicKey;

@RunWith(SpringRunner.class)
@SpringBootTest
public class JwtTest {
    private static final String pubKeyPath = "F:\\usststore\\rsa\\rsa.pub";

    private static final String priKeyPath = "F:\\usststore\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "234");
    }

    @Before
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        // 生成token
        String token = JwtUtils.generateToken(new UserInfo(20L, "jack"), privateKey, 5);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6MjAsInVzZXJuYW1lIjoiamFjayIsImV4cCI6MTU5NTk5MzQ5OH0.HAefbpmVM6BRPy2sVVIJlYkYYATQo_Ar3bwk-qXQ5dP6v42TBdorOe-SZpOPhZb8SE9Ul2ucxKep9zJu68LR6IUE5D1N52UkvjL5xNVXcBzmoXvQBgbQGv4PvAsQwbVubojrf_rZBmXi9OvYZtgBMgoLnsqMy54oCGGjmEsV6HE";

        // 解析token
        UserInfo user = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + user.getId());
        System.out.println("userName: " + user.getUsername());
    }

}
