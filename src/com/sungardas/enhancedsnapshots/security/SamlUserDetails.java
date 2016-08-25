package com.sungardas.enhancedsnapshots.security;

import com.sungardas.enhancedsnapshots.aws.dynamodb.Roles;
import com.sungardas.enhancedsnapshots.dto.UserDto;
import com.sungardas.enhancedsnapshots.service.UserService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;

import java.util.Arrays;


public class SamlUserDetails implements SAMLUserDetailsService {

    private UserService userService;

    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public Object loadUserBySAML(SAMLCredential credential) throws UsernameNotFoundException {
        com.sungardas.enhancedsnapshots.aws.dynamodb.model.User user = userService.getUser(credential.getNameID().getValue().toLowerCase());
        if (user == null) {
            String email = credential.getNameID().getValue().toLowerCase();
            UserDto userDto = new UserDto();
            userDto.setEmail(email);
            userDto.setRole(Roles.USER.getName());
            userService.createUser(userDto, "");
            user = userService.getUser(email);
        }
        return new User(credential.getNameID().getValue(), "",
                Arrays.asList(new SimpleGrantedAuthority(ROLE_PREFIX + user.getRole().toUpperCase())));
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }
}
