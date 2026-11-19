package com.squire.salon.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Logs the method, path, status, and duration of each request. */
@Component
public class RequestLogFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger("HTTP");

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    long startedAt = System.nanoTime();
    try {
      chain.doFilter(request, response);
    } finally {
      long ms = (System.nanoTime() - startedAt) / 1_000_000;
      String query = request.getQueryString() == null ? "" : "?" + request.getQueryString();
      log.info("{} {}{} {} {}ms", request.getMethod(), request.getRequestURI(), query, response.getStatus(), ms);
    }
  }
}
