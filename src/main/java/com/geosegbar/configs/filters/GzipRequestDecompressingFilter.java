package com.geosegbar.configs.filters;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

public class GzipRequestDecompressingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String contentEncoding = request.getHeader("Content-Encoding");

        if (contentEncoding != null && contentEncoding.toLowerCase().contains("gzip")) {
            filterChain.doFilter(new GzipRequestWrapper(request), response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private static class GzipRequestWrapper extends HttpServletRequestWrapper {

        private final byte[] decompressedBody;

        GzipRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            try (InputStream gzipStream = new GZIPInputStream(request.getInputStream())) {
                this.decompressedBody = gzipStream.readAllBytes();
            }
        }

        @Override
        public ServletInputStream getInputStream() {
            java.io.ByteArrayInputStream byteStream = new java.io.ByteArrayInputStream(decompressedBody);
            return new ServletInputStream() {
                @Override public int read() throws IOException { return byteStream.read(); }
                @Override public int read(byte[] b, int off, int len) throws IOException { return byteStream.read(b, off, len); }
                @Override public boolean isFinished() { return byteStream.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener readListener) {}
            };
        }

        @Override
        public java.io.BufferedReader getReader() throws IOException {
            return new java.io.BufferedReader(new java.io.InputStreamReader(getInputStream(), getCharacterEncoding()));
        }

        @Override
        public int getContentLength() { return decompressedBody.length; }

        @Override
        public long getContentLengthLong() { return decompressedBody.length; }

        @Override
        public String getHeader(String name) {
            if ("Content-Encoding".equalsIgnoreCase(name)) return null;
            if ("Content-Length".equalsIgnoreCase(name)) return String.valueOf(decompressedBody.length);
            return super.getHeader(name);
        }
    }
}
