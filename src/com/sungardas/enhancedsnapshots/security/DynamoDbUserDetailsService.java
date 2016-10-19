package com.sungardas.enhancedsnapshots.security;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.User;
import com.sungardas.enhancedsnapshots.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Arrays;


public class DynamoDbUserDetailsService implements UserDetailsService {

    private static final String ROLE_PREFIX = "ROLE_";
    @Autowired
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userService.getUser(username.toLowerCase());
        if (user != null) {
            return new org.springframework.security.core.userdetails.User(username, user.getPassword(),
                    Arrays.asList(new SimpleGrantedAuthority(ROLE_PREFIX + user.getRole().toUpperCase())));
        }
        throw new UsernameNotFoundException("No User found with login: ");
    }
}
