package no.dnb.loan.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Configuration
public class LoggingConfiguration extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    )
            throws ServletException, IOException
    {
        var start  = System.currentTimeMillis();
        log.info("INCOMING REQUEST: method={} uri={} remoteAddr={} userAgent={}", request.getMethod(), request.getRequestURI(), request.getRemoteAddr(), request.getHeader("User-Agent"));
        filterChain.doFilter(request, response);
        log.info("OUTGOING RESPONSE: status={} duration={}ms", response.getStatus(), System.currentTimeMillis() - start);
    }
}
