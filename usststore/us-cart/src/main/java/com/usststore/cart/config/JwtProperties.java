package com.usststore.cart.config;

import com.usststore.auth.utils.RsaUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

@Data
@ConfigurationProperties(prefix = "us.jwt")
public class JwtProperties {

    private String pubKeyPath;
    private String cookieName;
    private PublicKey publicKey;

    //对象一实例化，就读取公钥
    @PostConstruct
    public void init () throws Exception{

        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);

    }
}