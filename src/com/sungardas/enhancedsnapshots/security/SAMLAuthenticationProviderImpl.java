package com.sungardas.enhancedsnapshots.security;

import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.service.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.schema.impl.XSStringImpl;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml.SAMLAuthenticationProvider;
import org.springframework.security.saml.SAMLCredential;

public class SAMLAuthenticationProviderImpl extends SAMLAuthenticationProvider {

    private static final Logger LOG = LogManager.getLogger(SAMLAuthenticationProviderImpl.class);

    private static final String ALLOWED_LIST_ATTRIBUTE_NAME = "ess_instance_guids";

    private static final String ADMIN_WILDCARD_EXP = "*";

    private ConfigurationMediator configurationMediator;

    private UserService userService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (configurationMediator.isSungardasSSO()) {
            Authentication result = super.authenticate(authentication);

            SAMLCredential credential = (SAMLCredential) result.getCredentials();
            Attribute attribute = credential.getAttribute(ALLOWED_LIST_ATTRIBUTE_NAME);
            if (attribute != null) {
                for (XMLObject object : attribute.getAttributeValues()) {
                    String value = ((XSStringImpl) object).getValue();
                    if (ADMIN_WILDCARD_EXP.equals(value)) {
                        LOG.warn("User ({}) has admin access, instance UUID: {}", credential.getNameID().getValue(), configurationMediator.getUUID());
                        return result;
                    }
                    if (configurationMediator.getUUID().equals(value)) {
                        return result;
                    }
                }
            }

            LOG.error("User ({}) has not allowed to use this instance with UUID: {}", credential.getNameID().getValue(), configurationMediator.getUUID());

            userService.removeUser(result.getName());

            throw new AuthenticationServiceException("Access denied");
        } else {
            return super.authenticate(authentication);
        }
    }

    public void setConfigurationMediator(ConfigurationMediator configurationMediator) {
        this.configurationMediator = configurationMediator;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }
}
