package com.sungardas.enhancedsnapshots.service.impl;

import java.util.ArrayList;
import java.util.List;

import com.sungardas.enhancedsnapshots.aws.dynamodb.Roles;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.User;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.UserRepository;
import com.sungardas.enhancedsnapshots.dto.UserDto;
import com.sungardas.enhancedsnapshots.dto.converter.UserDtoConverter;
import com.sungardas.enhancedsnapshots.exception.DataAccessException;
import com.sungardas.enhancedsnapshots.exception.OperationNotAllowedException;
import com.sungardas.enhancedsnapshots.exception.UniqueConstraintViolationException;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private UserServiceImpl userService;
    private String adminEmail = "admin@super.com";
    private String userEmail = "user@user.com";
    private String psw = "psw";


    @Before
    public void setUp() {
        when(userRepository.findOne(userEmail)).thenReturn(UserDtoConverter.convert(createUserDto(userEmail)));
        when(userRepository.findOne(adminEmail)).thenReturn(UserDtoConverter.convert(createAdminDto(adminEmail)));
    }


    /**
     * Check admin can create new user accounts
     */
    @Test
    public void adminCanCreateNewUsers() {
        User newUser = createUser(userEmail);
        userService.createUser(createUserDto(userEmail), psw, adminEmail);

        // email should be converted to lowercase before user saving in DB
        newUser.setEmail(newUser.getEmail().toLowerCase());
        verify(userRepository, times(1)).save(newUser);
    }

    /**
     * Not admins are not allowed to create new user accounts
     */
    @Test(expected = OperationNotAllowedException.class)
    public void notAdminCanNotCreateNewUsers() {
        userService.createUser(createUserDto(userEmail), psw, userEmail);
    }

    /**
     * Admin can create another admin account
     */
    @Test
    public void adminCanCreateAnotherAdminAccount() {
        User newAdmin = createAdmin("new@admin");
        userService.createUser(createAdminDto("new@admin"), psw, adminEmail);
        verify(userRepository, times(1)).save(newAdmin);
    }


    /**
     * There can not be several users registered with the same account
     */
    @Test(expected = UniqueConstraintViolationException.class)
    public void thereCanNotBeSeveralUsersWithTheSameAccount() {
        when(userRepository.exists(createUserDto(userEmail).getEmail().toLowerCase())).thenReturn(true);
        userService.createUser(createUserDto(userEmail), psw, adminEmail);
    }


    /**
     * Admin can remove user accounts
     */
    @Test
    public void adminCanRemoveUserAccount() {
        when(userRepository.exists(userEmail)).thenReturn(true);
        userService.removeUser(userEmail, adminEmail);
        verify(userRepository, times(1)).delete(userEmail);
    }

    /**
     * In case there ia an attempt to remove non existing account DataAccessException should be thrown
     */
    @Test(expected = DataAccessException.class)
    public void nonExistingAccountCanNotBeRemoved() {
        userService.removeUser("nonExistingUser", adminEmail);
    }

    /**
     * Users are not allowed to remove other user accounts
     */
    @Test(expected = OperationNotAllowedException.class)
    public void userCanNotRemoveUserAccount() {
        when(userRepository.exists("someuser@com")).thenReturn(true);
        userService.removeUser("someUser@com", userEmail);
    }

    /**
     * Admin can remove another admin account
     */
    @Test
    public void adminCanRemoveAnotherAdminAccount() {
        String email = "some@admin";
        when(userRepository.exists(email)).thenReturn(true);
        User admin = createAdmin(email);
        when(userRepository.findOne(email)).thenReturn(admin);

        userService.removeUser(email, adminEmail);
        verify(userRepository, times(1)).delete(email);
    }

    /**
     * Admin can update user account
     */
    @Test
    public void adminCanUpdateUserAccount() {
        when(userRepository.exists(createUserDto(userEmail).getEmail().toLowerCase())).thenReturn(true);

        // updating password
        userService.updateUser(createUserDto(userEmail), "newPassword", adminEmail);

        User user = createUser(userEmail);
        user.setPassword(DigestUtils.sha512Hex("newPassword"));
        verify(userRepository, times(1)).save(user);
    }

    /**
     * User can not update another user account
     */
    @Test(expected = OperationNotAllowedException.class)
    public void userCanNotUpdateAnotherUserAccount() {
        when(userRepository.exists(createUserDto("user2@user2").getEmail().toLowerCase())).thenReturn(true);
        // updating password
        userService.updateUser(createUserDto("user2@user2"), "newPassword", userEmail);
    }

    /**
     * User can update his account
     */
    @Test
    public void userCanUpdateHisAccount() {
        when(userRepository.exists(createUserDto(userEmail).getEmail().toLowerCase())).thenReturn(true);

        // updating password
        userService.updateUser(createUserDto(userEmail), "newPassword", userEmail);

        User user = createUser(userEmail);
        user.setPassword(DigestUtils.sha512Hex("newPassword"));
        verify(userRepository, times(1)).save(user);
    }

    /**
     * Not existing user can not be updated
     */
    @Test(expected = DataAccessException.class)
    public void nonExistingAccountCanNotBeUpdated() {
        userService.updateUser(createUserDto("unreal@user"), "psw", adminEmail);
    }


    /**
     * Admin can update other admin account
     */
    @Test
    public void adminCanUpdateAnotherAdminAccount() {
        User user = createAdmin("another@admin");
        when(userRepository.exists(createUserDto("another@admin").getEmail())).thenReturn(true);
        when(userRepository.findOne(createUserDto("another@admin").getEmail())).thenReturn(user);

        // updating password
        userService.updateUser(createAdminDto("another@admin"), "newPassword", adminEmail);
        user.setPassword(DigestUtils.sha512Hex("newPassword"));
        verify(userRepository, times(1)).save(user);
    }

    /**
     * Admin account can not be removed in case it's last admin in system
     */
    @Test(expected = OperationNotAllowedException.class)
    public void lastAdminCanNotBeRemoved() {
        when(userRepository.exists(adminEmail)).thenReturn(true);
        List<User> admins = new ArrayList<>();
        admins.add(UserDtoConverter.convert(createAdminDto(adminEmail)));
        when(userRepository.findByRole(Roles.ADMIN.getName())).thenReturn(admins);

        userService.removeUser(adminEmail, adminEmail);
    }

    /**
     * Admin role can not be changed in case it's last admin in system
     */
    @Test(expected = OperationNotAllowedException.class)
    public void lastAdminCanNotChangeHisRoleToUser() {
        when(userRepository.exists(adminEmail)).thenReturn(true);
        List<User> admins = new ArrayList<>();
        admins.add(UserDtoConverter.convert(createAdminDto(adminEmail)));
        when(userRepository.findByRole(Roles.ADMIN.getName())).thenReturn(admins);

        UserDto adminDto = createAdminDto(adminEmail);
        adminDto.setAdmin(false);
        userService.updateUser(adminDto, psw, adminEmail);
    }

    /**
     * Users are not allowed to change their roles to admin
     */
    @Test(expected = OperationNotAllowedException.class)
    public void userCanNotChangHisRoleToAdmin() {
        when(userRepository.exists(createUserDto(userEmail).getEmail().toLowerCase())).thenReturn(true);
        UserDto dto = createAdminDto(userEmail);
        dto.setAdmin(true);
        userService.updateUser(dto, "777", userEmail);
    }

    /**
     * In case password was not provided it should not be changed
     */
    @Test
    public void useOldPasswordInCaseItWasNotProvided() {
        when(userRepository.exists(userEmail)).thenReturn(true);

        User user = createUser(userEmail);
        user.setPassword("777");
        when(userRepository.findOne(userEmail)).thenReturn(user);

        userService.updateUser(createUserDto(userEmail), "", adminEmail);
        verify(userRepository, times(1)).save(user);
    }

    /**
     * Email should be case insensitive
     */
    @Test(expected = UniqueConstraintViolationException.class)
    public void emailsShouldBeCaseInsensitive() {
        UserDto userDto = createUserDto(userEmail);
        userDto.setEmail(userDto.getEmail().toUpperCase());
        when(userRepository.exists(userEmail)).thenReturn(true);
        userService.createUser(userDto, psw, adminEmail);
    }


    private UserDto createUserDto(String email) {
        UserDto user = new UserDto();
        user.setAdmin(false);
        user.setFirstName("Michael");
        user.setLastName("Smith");
        user.setEmail(email);
        return user;
    }

    private UserDto createAdminDto(String email) {
        UserDto user = new UserDto();
        user.setAdmin(true);
        user.setFirstName("admin");
        user.setLastName("super");
        user.setEmail(email);
        return user;
    }

    private User createUser(String email) {
        UserDto dto = createUserDto(email);
        User user = UserDtoConverter.convert(dto);
        user.setPassword(DigestUtils.sha512Hex(psw));
        return user;
    }

    private User createAdmin(String email) {
        UserDto dto = createAdminDto(email);
        User user = UserDtoConverter.convert(dto);
        user.setPassword(DigestUtils.sha512Hex(psw));
        return user;
    }


}
