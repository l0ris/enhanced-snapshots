package com.sungardas.enhancedsnapshots.security;


public class InitUserDetailsServiceDev  extends InitUserDetailsService {

    private static final String DEFAULT_PSW = "admin";
    protected String getPsw(){
        return DEFAULT_PSW;
    }
}
