package com.usststore.auth.config;

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

    private String secret;
    private String pubKeyPath;
    private String priKeyPath;
    private Integer expire;
    private String cookieName;
    private PublicKey publicKey;
    private PrivateKey privateKey;

    //对象一实例化，就读取公钥和私钥
    @PostConstruct
    public void init () throws Exception{
        //公钥私钥如果不存在，先生成
        File pubFile = new File(pubKeyPath);
        File priFile = new File(priKeyPath);
        if (!pubFile.exists() || !priFile.exists()){
            RsaUtils.generateKey(pubKeyPath,priKeyPath,secret);
        }
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

}
