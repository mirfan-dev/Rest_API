package com.rest.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtToken {
    @Value("${jwt.secret}")
    private String SECRET;

    @Value("${jwt.expiration.access}")
    private long EXPIRATION_TIME;

    @Value("${jwt.expiration.refresh}")
    private long EXPIRATION_ACCESS_TIME;

    private static final String REFRESH_TOKEN_TYPE = "refresh_token";
    private static final String ACCESS_TOKEN_TYPE = "access_token";

    // Generate Token
    public String generateToken(String email, boolean isAccessToken) {

        long expTime=isAccessToken ? EXPIRATION_TIME : EXPIRATION_ACCESS_TIME;

        String tokenType=isAccessToken ? ACCESS_TOKEN_TYPE : REFRESH_TOKEN_TYPE;
        Map<String ,Object> claims=new HashMap<>();
        claims.put("typ",tokenType);
        String token = Jwts.builder()
                .setClaims(claims)
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expTime))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()), SignatureAlgorithm.HS256)
                .compact();
        return token;
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    // Get username from token

    public String extractEmail(String token) {
        return getAllClaims(token).getSubject();
    }

    private Claims getAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // validate Token

    public boolean validateToken(String token){

        if(this.isTokenExpired(token)){
            return false;
        }

        try {
            Jwts.parser().setSigningKey(SECRET.getBytes()).build().parseSignedClaims(token);
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    public boolean isTokenExpired(String token) {
        Date expiration = Jwts.parser()
                .setSigningKey(SECRET.getBytes()).build()
                .parseClaimsJws(token).getBody().getExpiration();
        return expiration.before(new Date());

    }

    public boolean isRefreshToken(String token){
        Claims claims = Jwts.parser()
                .setSigningKey(SECRET.getBytes()).build()
                .parseClaimsJws(token)
                .getBody();

        String tokenType = (String) claims.get("typ");
        if(tokenType==null) return false;
        return tokenType.equals(REFRESH_TOKEN_TYPE);
    }

    public boolean isAccessToken(String token){
        Claims claims = Jwts.parser()
                .setSigningKey(SECRET.getBytes()).build()
                .parseClaimsJws(token)
                .getBody();

        String tokenType = (String) claims.get("typ");

        if(tokenType==null) return false;
        return tokenType.equals(ACCESS_TOKEN_TYPE);
    }
}