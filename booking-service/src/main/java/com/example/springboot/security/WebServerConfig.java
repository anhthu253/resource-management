package com.example.springboot.security;


import org.apache.tomcat.util.http.Rfc6265CookieProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.servlet.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebServerConfig {
    @Value("${server.servlet.session.cookie.same-site}")
    private String setSameSiteCookies;
    @Value("${server.servlet.session.cookie.secure}")
    private boolean useHttpOnly;
    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        tomcat.addContextCustomizers(context -> {
            Rfc6265CookieProcessor cookieProcessor = new Rfc6265CookieProcessor();
                cookieProcessor.setSameSiteCookies(setSameSiteCookies); // cross-site for Cloud Run
                context.setUseHttpOnly(useHttpOnly); // optional
            context.setCookieProcessor(cookieProcessor);
        });
        return tomcat;
    }
}