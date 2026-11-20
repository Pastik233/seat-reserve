package com.seat.reserve.config;

import com.seat.reserve.security.JwtAuthFilter;
import com.seat.reserve.security.JwtUtil;
import com.seat.reserve.security.RestAuthEntryPoint;
import com.seat.reserve.security.UserSnapshotStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** 白名单：登录本身、健康检查、错误页 */
    /** 白名单：登录本身、健康检查、错误页，以及打包后的前端静态资源 */
    private static final String[] WHITE_LIST = {
            "/api/auth/login",
            "/api/ping", "/api/ping/**",
            "/error",
            // 前端静态资源（jar 内置）
            "/", "/index.html", "/assets/**", "/favicon.ico", "/vite.svg",
            // 前端路由本身也要放行，否则刷新 /seats 会被拦成 401（鉴权由前端的路由守卫做）
            "/login", "/seats", "/my", "/admin", "/admin/seats","/admin/bookings"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtUtil jwtUtil,
                                                   UserSnapshotStore snapshotStore,
                                                   RestAuthEntryPoint entryPoint) throws Exception {
        JwtAuthFilter jwtAuthFilter = new JwtAuthFilter(jwtUtil, snapshotStore);
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(WHITE_LIST).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 委托式密码编码器：认 {noop}、{bcrypt} 这类前缀。
     * 种子里是 {noop}123456（方便调试），以后注册接口用 passwordEncoder.encode() 存 {bcrypt}...
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
