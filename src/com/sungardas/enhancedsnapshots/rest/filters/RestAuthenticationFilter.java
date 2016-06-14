package com.sungardas.enhancedsnapshots.rest.filters;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.UserRepository;
import com.sungardas.enhancedsnapshots.rest.utils.JsonFromStream;
import com.sungardas.enhancedsnapshots.rest.utils.MultiReadHttpServletRequest;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import static com.sungardas.enhancedsnapshots.rest.utils.Constants.CONTEXT_ALLOWED_SESSIONS_ATR_NAME;
import static com.sungardas.enhancedsnapshots.rest.utils.Constants.JSON_AUTHENTIFICATION_EMAIL;
import static com.sungardas.enhancedsnapshots.rest.utils.Constants.JSON_AUTHENTIFICATION_PASSWORD;

@Service
public class RestAuthenticationFilter implements com.sungardas.enhancedsnapshots.rest.RestAuthenticationFilter {

    private static final Logger LOG = LogManager.getLogger(RestAuthenticationFilter.class);

    private UserRepository userRepository;

    private String instanceId;

    public void destroy() {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException,
            IOException {
        Map<String, String> allowedSessions = (Map<String, String>) request.getServletContext().getAttribute(CONTEXT_ALLOWED_SESSIONS_ATR_NAME);
        if (allowedSessions == null) {
            request.getServletContext().setAttribute(CONTEXT_ALLOWED_SESSIONS_ATR_NAME, new HashMap<String, String>());
            allowedSessions = (Map<String, String>) request.getServletContext().getAttribute(CONTEXT_ALLOWED_SESSIONS_ATR_NAME);
        }
        if (request instanceof HttpServletRequest) {
            MultiReadHttpServletRequest multiReadRequest = new MultiReadHttpServletRequest((HttpServletRequest) request);
            HttpSession session = ((HttpServletRequest) request).getSession();

            boolean allowed = allowedSessions.containsKey(session.getId());
            if (!allowed && multiReadRequest.getPathInfo().endsWith("/session")) {
                if (!"DELETE".equals(((HttpServletRequest) request).getMethod())) {
                    InputStream requestStream = multiReadRequest.getInputStream();
                    try {
                        JSONObject authCredentials = JsonFromStream.newJSONObject(requestStream);
                        String email = authCredentials.getString(JSON_AUTHENTIFICATION_EMAIL).toLowerCase();
                        String password = authCredentials.getString(JSON_AUTHENTIFICATION_PASSWORD);
                        allowed = !userRepository.findByEmailAndPassword(email, DigestUtils.sha512Hex(password)).isEmpty();
                        if (allowed) {
                            allowedSessions.put(session.getId(), email);
                            LOG.info("Add session to allowed list: [{}] [{}]", session.getId(), email);
                        }
                    } catch (JSONException e) {
                        LOG.debug(e);
                    }
                } else {
                    allowed = true;
                }
            }
            if (allowed) {
                try {
                    chain.doFilter(multiReadRequest, response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                LOG.info("Authentication failed. Session [{}]", session.getId());
                if (response instanceof HttpServletResponse) {
                    HttpServletResponse httpServletResponse = (HttpServletResponse) response;
                    httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                }
            }
        }

    }

    public void init(FilterConfig fConfig) throws ServletException {
    }

    @Override
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
}
