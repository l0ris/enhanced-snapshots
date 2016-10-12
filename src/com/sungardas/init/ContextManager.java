package com.sungardas.init;

import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.security.SAMLAuthenticationProviderImpl;
import com.sungardas.enhancedsnapshots.security.SamlUserDetails;
import com.sungardas.enhancedsnapshots.service.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.PropertySource;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.stereotype.Service;
import org.springframework.web.context.support.XmlWebApplicationContext;

@Service
public class ContextManager {

    private static final Logger LOG = LogManager.getLogger(ContextManager.class);

    private boolean CONTEXT_REFRESH_IN_PROCESS = false;

    @Autowired
    private XmlWebApplicationContext applicationContext;


    public void refreshInitContext(){
        LOG.info("Init context refresh process started.");
        CONTEXT_REFRESH_IN_PROCESS = true;
        XmlWebApplicationContext rootContext = (XmlWebApplicationContext) applicationContext.getParent();
        rootContext.setConfigLocations("/WEB-INF/spring-security.xml");
        rootContext.refresh();
        applicationContext.setConfigLocations("/WEB-INF/init-spring-web-config.xml");
        applicationContext.refresh();
        LOG.info("Init context refreshed successfully.");
        CONTEXT_REFRESH_IN_PROCESS = false;
    }

    public void refreshContext(boolean ssoMode, String entityId) {
        LOG.info("Context refresh process started. SSO mode: {}", ssoMode);
        CONTEXT_REFRESH_IN_PROCESS = true;
        //for sso
        if(ssoMode) {
            // do not change order of context refreshes
            // we need to refresh root context otherwise springSecurityFilterChain will not be updated
            // https://jira.spring.io/browse/SPR-6228
            XmlWebApplicationContext rootContext = (XmlWebApplicationContext) applicationContext.getParent();
            rootContext.setConfigLocations("/WEB-INF/spring-security-saml.xml");
            // add property entityId to root context, it will be used as psw for jks
            rootContext.getEnvironment().getPropertySources().addLast((new RefreshRootContextPropertySource(entityId)));
            rootContext.refresh();

            // refresh application context
            applicationContext.setConfigLocations("/WEB-INF/spring-web-config.xml");
            applicationContext.refresh();

            // set userService property to userDetails bean, so we could manage users roles within ssoLogin mode
            applicationContext.getBean(SamlUserDetails.class).setUserService(applicationContext.getBean(UserService.class));
            applicationContext.getBean(SAMLAuthenticationProviderImpl.class).setConfigurationMediator(applicationContext.getBean(ConfigurationMediator.class));
        }
        // for local authentication
        else {
            applicationContext.setConfigLocations("/WEB-INF/spring-web-config.xml", "/WEB-INF/spring-security-dynamoDB.xml");
            applicationContext.refresh();
            // clearing init auth providers
            ((ProviderManager)applicationContext.getBean("authenticationManager")).getProviders().clear();

            // adding main auth provider
            ((ProviderManager)applicationContext.getBean("authenticationManager")).getProviders()
                    .add((AuthenticationProvider) applicationContext.getBean("authProvider"));
        }

        LOG.info("Context refreshed successfully.");
        CONTEXT_REFRESH_IN_PROCESS = false;
    }


    // Passing enhancedsnapshots.saml.sp.entityId property to context
    private static class RefreshRootContextPropertySource extends PropertySource<String> {
        private String entityId;

        public RefreshRootContextPropertySource(String entityId) {
            super("custom");
            this.entityId = entityId;
        }

        @Override
        public String getProperty(String name) {
            if (name.equals("enhancedsnapshots.saml.sp.entityId")) {
                return entityId;
            }
            return null;
        }
    }

    public boolean contextRefreshInProcess(){
        return CONTEXT_REFRESH_IN_PROCESS;
    }

}
