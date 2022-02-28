package org.xapps.services.usermanagementservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.xapps.services.usermanagementservice.entities.User;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class AuthorizationFilter implements Filter {
    private final SecurityConfig securityConfig;
    private final ObjectMapper objectMapper;

    public AuthorizationFilter(SecurityConfig securityConfig, ObjectMapper objectMapper) {
        this.securityConfig = securityConfig;
        this.objectMapper = objectMapper;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String authzHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authzHeader != null && authzHeader.startsWith(securityConfig.getType())) {
            UsernamePasswordAuthenticationToken authentication = getAuthentication(request);
            if (authentication != null) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request) {
        UsernamePasswordAuthenticationToken auth = null;
        String authzHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = authzHeader.replace(securityConfig.getType(), "");
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(securityConfig.getKey())
                    .parseClaimsJws(token)
                    .getBody();
            String subject = claims.getSubject();
            User user = objectMapper.readValue(subject, User.class);
            List<GrantedAuthority> authorities = user.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.getName())).collect(Collectors.toList());
            auth = new UsernamePasswordAuthenticationToken(user, null, authorities);
        } catch (Exception ex) {
            log.error("Exception captured", ex);
        }
        return auth;
    }

}
