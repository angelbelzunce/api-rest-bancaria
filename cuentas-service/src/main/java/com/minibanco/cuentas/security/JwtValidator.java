package com.minibanco.cuentas.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

@Component
public class JwtValidator {
    private final PublicKey publicKey;

    public JwtValidator(@Value("${jwt.public-key}") String publicKey) throws Exception {
        byte[] keyBytes = Decoders.BASE64.decode(publicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        this.publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }

    public String getUsername (String token){
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
