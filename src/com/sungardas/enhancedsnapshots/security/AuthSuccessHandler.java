package com.sungardas.enhancedsnapshots.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthSuccessHandler implements AuthenticationSuccessHandler {

    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException, ServletException {
        httpServletResponse.setStatus(200);
        httpServletResponse.getWriter().write("{ \"role\":\"" + authentication.getAuthorities().iterator().next().getAuthority().replace(ROLE_PREFIX, "").toLowerCase()
                + "\", \"email\":\"" + authentication.getName() + "\" }");
    }

}
