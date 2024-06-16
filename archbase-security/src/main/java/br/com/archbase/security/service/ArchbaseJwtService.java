package br.com.archbase.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@Slf4j
public class ArchbaseJwtService {

    @Value("${archbase.security.jwt.secret-key}")
    private String secretKey;

    @Value("${archbase.security.jwt.token-expiration}")
    private long jwtExpiration;

    @Value("${archbase.security.jwt.refresh-expiration}")
    private long refreshExpiration;

    @PostConstruct
    public void initialize() {
        if (StringUtils.isEmpty(secretKey)) {
            log.warn("Define uma chave secreta para a autenticação. Defina a propriedade 'archbase.security.jwt.secret-key'.");
            return;
        }
        if (jwtExpiration==0) {
            log.warn("Defina o tempo de expiração dos tokens de acesso. Defina a propriedade 'archbase.security.jwt.token-expiration'.");
            return;
        }
        if (refreshExpiration==0) {
            log.warn("Defina o tempo de expiração para refresh dos tokens de acesso. Defina a propriedade 'archbase.security.jwt.refresh-expiration'.");
            return;
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public TokenResult generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public TokenResult generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails
    ) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    public TokenResult generateRefreshToken(
            UserDetails userDetails
    ) {
        return buildToken(new HashMap<>(), userDetails, refreshExpiration);
    }

    private TokenResult buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        var token = Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
        return new TokenResult(token, expiration);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException expiredJwtException) {
            return true;
        }
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public record TokenResult(String token, Long expiresIn) {
    }
}


