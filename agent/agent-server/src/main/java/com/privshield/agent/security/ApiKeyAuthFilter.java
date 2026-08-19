package com.privshield.agent.security;

import com.privshield.agent.config.SecurityProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * API Key 认证过滤器 — 对应 Python ApiKeyAuthAsgiMiddleware。
 *
 * <p>当 security.auth-enabled=true 时，校验 Authorization: Bearer 头中的 API Key。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class ApiKeyAuthFilter implements Filter {

    private final SecurityProperties securityProps;

    public ApiKeyAuthFilter(SecurityProperties securityProps) {
        this.securityProps = securityProps;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!securityProps.isAuthEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        if (request instanceof HttpServletRequest httpRequest) {
            String path = httpRequest.getRequestURI();
            // 健康检查端点免认证
            if (path.equals("/health") || path.equals("/v1/health") || path.equals("/livez") || path.equals("/v1/readyz")) {
                chain.doFilter(request, response);
                return;
            }

            String authHeader = httpRequest.getHeader("Authorization");
            String expectedKey = securityProps.getApiKey();
            if (expectedKey != null && !expectedKey.isEmpty()) {
                String bearerToken = null;
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    bearerToken = authHeader.substring(7);
                }
                if (!expectedKey.equals(bearerToken)) {
                    ((HttpServletResponse) response).sendError(401, "Unauthorized");
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }
}
