package com.sungardas.enhancedsnapshots.rest;

import javax.servlet.Filter;

import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.UserRepository;

public interface RestAuthenticationFilter extends Filter {
    void setUserRepository(UserRepository userRepository);

    void setInstanceId(String instanceId);
}
