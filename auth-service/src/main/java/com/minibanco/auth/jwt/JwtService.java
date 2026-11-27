package com.minibanco.auth.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;

@Service
public class JwtService {

    private final PrivateKey privateKey;
    private final long expiration;

    public JwtService(@Value("${jwt.private-key}") String privateKey,
                      @Value("${jwt.expiration}") long expiration) throws Exception {
        byte[] keyBytes = Decoders.BASE64.decode(privateKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        this.privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        this.expiration = expiration;
    }

    public String generateToken(String username){
        Date now = new Date();

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(privateKey)
                .compact();
    }
}
