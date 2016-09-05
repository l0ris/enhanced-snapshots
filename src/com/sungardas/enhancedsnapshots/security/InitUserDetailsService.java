package com.sungardas.enhancedsnapshots.security;

import com.amazonaws.util.EC2MetadataUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Arrays;


public class InitUserDetailsService implements UserDetailsService {

    private static final String DEFAULT_LOGIN = "admin@enhancedsnapshots";
    private static final String ROLE_CONFIGURATOR = "ROLE_CONFIGURATOR";

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if(username.equals(DEFAULT_LOGIN))
            return new User(username, getPsw(), Arrays.asList(new SimpleGrantedAuthority(ROLE_CONFIGURATOR)));
        throw new UsernameNotFoundException("No User found with login: ");
    }

    protected String getPsw(){
        return EC2MetadataUtils.getInstanceId();
    }
}
