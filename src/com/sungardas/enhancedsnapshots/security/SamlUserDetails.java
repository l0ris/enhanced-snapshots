package com.sungardas.enhancedsnapshots.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;

import java.util.Arrays;


public class SamlUserDetails implements SAMLUserDetailsService {

    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public Object loadUserBySAML(SAMLCredential credential) throws UsernameNotFoundException {
        return new User(credential.getNameID().getValue(), "",
                Arrays.asList(new SimpleGrantedAuthority(ROLE_PREFIX + "ADMIN")));
    }
}
