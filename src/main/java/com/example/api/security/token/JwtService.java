package com.example.api.security.token;

import com.example.api.common.exception.InvalidTokenException;
import com.example.api.security.config.JwtProperties;
import com.example.api.security.model.AppUserDetails;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        byte[] keyBytes = Base64.getDecoder().decode(
                properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.signingKey = new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    public TokenPair generateTokenPair(AppUserDetails user) {
        String access = generateToken(user, TokenType.ACCESS, properties.getAccessTokenExpirationMs());
        String refresh = generateToken(user, TokenType.REFRESH, properties.getRefreshTokenExpirationMs());
        return new TokenPair(access, refresh);
    }

    public String generateToken(AppUserDetails user, TokenType type, long ttlMs) {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer(properties.getIssuer())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusMillis(ttlMs)))
                .jwtID(UUID.randomUUID().toString())
                .claim("type", type.name())
                .claim("uid", user.getUserId())
                .claim("auth", user.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .toList())
                .build();

        SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            signedJwt.sign(new MACSigner(signingKey));
        } catch (JOSEException e) {
            throw new IllegalStateException("No fue posible firmar el JWT", e);
        }
        return signedJwt.serialize();
    }

    public JwtClaims parse(String token, TokenType expectedType) {
        try {
            SignedJWT signedJwt = SignedJWT.parse(token);
            if (!signedJwt.verify(new MACVerifier(signingKey))) {
                throw new InvalidTokenException("Firma del token invalida");
            }
            JWTClaimsSet claims = signedJwt.getJWTClaimsSet();

            Date expiration = claims.getExpirationTime();
            if (expiration == null || expiration.before(new Date())) {
                throw new InvalidTokenException("Token expirado");
            }
            if (!properties.getIssuer().equals(claims.getIssuer())) {
                throw new InvalidTokenException("Emisor del token invalido");
            }

            TokenType type;
            try {
                type = TokenType.valueOf(claims.getStringClaim("type"));
            } catch (Exception e) {
                throw new InvalidTokenException("Tipo de token invalido");
            }
            if (type != expectedType) {
                throw new InvalidTokenException("Tipo de token no permitido para esta operacion");
            }

            Number userId = claims.getLongClaim("uid");
            List<String> authorities = claims.getStringListClaim("auth");

            return new JwtClaims(
                    claims.getSubject(),
                    userId == null ? null : userId.longValue(),
                    type,
                    authorities == null ? List.of() : authorities,
                    claims.getJWTID()
            );
        } catch (ParseException | JOSEException e) {
            throw new InvalidTokenException("Token malformado");
        }
    }
}