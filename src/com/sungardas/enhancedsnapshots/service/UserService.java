package com.sungardas.enhancedsnapshots.service;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.User;
import com.sungardas.enhancedsnapshots.dto.UserDto;


import javax.annotation.security.RolesAllowed;
import java.util.List;

public interface UserService {
    List<UserDto> getAllUsers();

    void createUser(UserDto newUser, String password);

    void createSamlUser(UserDto newUser);

    void updateUser(UserDto newUser, String newPassword, String currentUserEmail);

    void removeUser(String userEmail);

    User getUser(String user);

    boolean isAdmin(String userEmail);
}
