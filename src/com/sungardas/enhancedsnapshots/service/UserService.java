package com.sungardas.enhancedsnapshots.service;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.User;
import com.sungardas.enhancedsnapshots.dto.UserDto;


import javax.annotation.security.RolesAllowed;
import java.util.List;

public interface UserService {
    List<UserDto> getAllUsers();

    @RolesAllowed("ROLE_ADMIN")
    void createUser(UserDto newUser, String password);

    void updateUser(UserDto newUser, String newPassword, String currentUserEmail);

    @RolesAllowed("ROLE_ADMIN")
    void removeUser(String userEmail);

    User getUser(String user);

    boolean isAdmin(String userEmail);
}
