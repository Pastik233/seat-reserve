package com.seat.reserve.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 每个请求来一次：解析 Bearer token → 读 Redis 快照 → 塞进 SecurityContext。
 * 注意：这里不做拦截判断（不 return 401），交给 SecurityConfig 的授权规则和 EntryPoint。
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserSnapshotStore snapshotStore;

    public JwtAuthFilter(JwtUtil jwtUtil, UserSnapshotStore snapshotStore) {
        this.jwtUtil = jwtUtil;
        this.snapshotStore = snapshotStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtUtil.parse(token);
                Long userId = Long.valueOf(claims.getSubject());
                snapshotStore.get(userId).ifPresent(snapshot -> {
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            snapshot, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + snapshot.roleCode())));
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
            } catch (JwtException | IllegalArgumentException e) {
                // 签名不对/过期/不是数字 → 当匿名处理，后面授权规则自然会 401
                SecurityContextHolder.clearContext();
                logger.debug("jwt 校验失败: " + e.getMessage(), e);
            }
        }
        chain.doFilter(request, response);
    }
}