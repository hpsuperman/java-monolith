package com.hpsuperman.monolith.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpsuperman.monolith.common.result.Result;
import com.hpsuperman.monolith.common.result.ResultCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider tokenProvider;
    private final TokenStore tokenStore;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String token = BearerTokenResolver.resolve(request);
        if (!StringUtils.hasText(token) || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = tokenProvider.parse(token).getPayload();

            if (isUsableAccessToken(claims)) {
                LoginUser loginUser = LoginUser.fromClaims(claims);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                loginUser, null, loginUser.getAuthorities());
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            log.debug("令牌解析失败 [{}] {}", request.getRequestURI(), e.getMessage());
        } catch (DataAccessException e) {
            log.error("令牌状态存储不可用，拒绝本次请求 | uri={}", request.getRequestURI(), e);
            writeServiceUnavailable(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isUsableAccessToken(Claims claims) {
        boolean isAccessToken = JwtTokenProvider.TYPE_ACCESS
                .equals(claims.get(JwtTokenProvider.CLAIM_TYPE, String.class));
        if (!isAccessToken) {
            return false;
        }
        return !tokenStore.isRevoked(
                tokenProvider.getUserId(claims), claims.getId(), claims.getIssuedAt());
    }

    private void writeServiceUnavailable(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), Result.fail(ResultCode.SERVICE_UNAVAILABLE));
    }
}
