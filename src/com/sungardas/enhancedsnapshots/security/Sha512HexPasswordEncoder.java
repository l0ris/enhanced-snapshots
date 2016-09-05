package com.sungardas.enhancedsnapshots.security;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


@Component
public class Sha512HexPasswordEncoder implements PasswordEncoder {

    @Override
    public String encode(CharSequence rawPassword) {
       return DigestUtils.sha512Hex(rawPassword.toString());
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
       return encode(rawPassword).equals(encodedPassword);
    }
}