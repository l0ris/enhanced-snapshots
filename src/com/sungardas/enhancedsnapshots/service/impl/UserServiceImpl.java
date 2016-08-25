package com.sungardas.enhancedsnapshots.service.impl;

import java.util.List;

import com.sungardas.enhancedsnapshots.aws.dynamodb.Roles;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.User;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.UserRepository;
import com.sungardas.enhancedsnapshots.dto.UserDto;
import com.sungardas.enhancedsnapshots.dto.converter.UserDtoConverter;
import com.sungardas.enhancedsnapshots.exception.DataAccessException;
import com.sungardas.enhancedsnapshots.exception.OperationNotAllowedException;
import com.sungardas.enhancedsnapshots.exception.UniqueConstraintViolationException;
import com.sungardas.enhancedsnapshots.service.UserService;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

@Service
@DependsOn("SystemService")
public class UserServiceImpl implements UserService {

    private static final Logger LOG = LogManager.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<UserDto> getAllUsers() {
        try {
            return UserDtoConverter.convert(userRepository.findAll());
        } catch (RuntimeException e) {
            LOG.error(e);
            throw new DataAccessException(e);
        }
    }

    @Override
    public void createUser(UserDto userInfo, String password) throws DataAccessException, UniqueConstraintViolationException {
        // check whether user with the same email already exists
        if (!userRepository.findByEmail(userInfo.getEmail().toLowerCase()).isEmpty()) {
            UniqueConstraintViolationException e = new UniqueConstraintViolationException("User with such email already exists: " + userInfo.getEmail());
            LOG.info("Failed to register user.", e);
            throw e;
        }
        try {
            User newUser = UserDtoConverter.convert(userInfo);
            newUser.setPassword(DigestUtils.sha512Hex(password));

            // convert user email to lowercase
            newUser.setEmail(newUser.getEmail().toLowerCase());
            if (newUser.getRole().isEmpty()) {
                newUser.setRole(Roles.USER.getName());
            }
            userRepository.save(newUser);
        } catch (RuntimeException e) {
            LOG.error("Failed to register user.", e);
            throw e;
        }
    }

    @Override
    public void createSamlUser(UserDto userInfo) throws DataAccessException, UniqueConstraintViolationException {
        createUser(userInfo, "");
    }

    @Override
    public void updateUser(UserDto userInfo, String newPassword, String currentUserEmail) {
        // check user exists
        List<User> users = userRepository.findByEmail(userInfo.getEmail().toLowerCase());
        if (users.isEmpty()) {
            DataAccessException e = new DataAccessException("User with such email does not exist: " + userInfo.getEmail());
            LOG.info("Failed to update user.", e);
            throw e;
        }
        try {
            // currentUser - user who performs update
            User currentUser = userRepository.findByEmail(currentUserEmail.toLowerCase()).get(0);

            // check whether current user has permission to modify existing user
            if (isAdmin(currentUser) || userInfo.getEmail().toLowerCase().equals(currentUserEmail)) {
                User userToBeUpdated = users.get(0);
                User updatedUser = UserDtoConverter.convert(userInfo);
                updatedUser.setId(userToBeUpdated.getId());
                // new password setting
                if (newPassword != null && !newPassword.isEmpty()) {
                    updatedUser.setPassword(DigestUtils.sha512Hex(newPassword));
                } else {
                    // in case password is empty we use old password
                    updatedUser.setPassword(userToBeUpdated.getPassword());
                }
                // in case it's last admin in system, ADMIN role can not be changed
                if (isAdmin(userToBeUpdated) && updatedUser.getRole().equals(Roles.USER.getName()) && isLastAdmin()) {
                    OperationNotAllowedException e = new OperationNotAllowedException("At least one user with admin role must be in system.");
                    LOG.error("Admin role can not be changed in case it's last admin in system.", e);
                    throw e;
                }
                // not admin user can not change its role to admin
                if (!isAdmin(currentUser) && updatedUser.getRole().equals(Roles.ADMIN.getName())) {
                    OperationNotAllowedException e = new OperationNotAllowedException("Only admin users can create other users with admin roles");
                    LOG.error("Only admin users can create other users with admin roles", e);
                    throw e;
                }
                // convert user email to lowercase
                updatedUser.setEmail(updatedUser.getEmail().toLowerCase());
                userRepository.save(updatedUser);
            } else {
                OperationNotAllowedException e = new OperationNotAllowedException("Only users with admin role can update users.");
                LOG.error("Failed to update user.", e);
                throw e;
            }
        } catch (RuntimeException e) {
            LOG.error("Failed to update user.", e);
            throw e;
        }
    }

    @Override
    public void removeUser(String userEmail) {
        // check user exists
        if (userRepository.findByEmail(userEmail.toLowerCase()).isEmpty()) {
            DataAccessException e = new DataAccessException("User with such email does not exist: " + userEmail);
            LOG.info("Failed to remove user.", e);
            throw e;
        }
        try {
            // in case it's last admin in system, it can not be removed
            if (isAdmin(userEmail) && isLastAdmin()) {
                OperationNotAllowedException e = new OperationNotAllowedException("Admin user can not be removed in case it is the last admin in system.");
                LOG.debug("Admin user can not be removed in case it's last admin in system.", e);
                throw e;
            } else {
                userRepository.delete(userRepository.findByEmail(userEmail));
            }
        } catch (RuntimeException e) {
            LOG.error("Failed to remove user.", e);
            throw e;
        }

    }

    @Override
    public User getUser(String user) {
        // there should not be several users with one email
        if(userRepository.findByEmail(user.toLowerCase()).isEmpty()){
            return null;
        }
        return userRepository.findByEmail(user.toLowerCase()).get(0);
    }

    public boolean isAdmin(String userEmail) {
        User user = userRepository.findByEmail(userEmail.toLowerCase()).get(0);
        return user.getRole() != null && user.getRole().equals(Roles.ADMIN.getName());
    }

    private boolean isAdmin(User user) {
        return user.getRole() != null &&
                user.getRole().equals(Roles.ADMIN.getName());
    }

    private boolean isLastAdmin() {
        return userRepository.findByRole(Roles.ADMIN.getName()).size() == 1;
    }
}
