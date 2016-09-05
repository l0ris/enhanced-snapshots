package com.sungardas.snapdirector.dto.converter;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.User;
import com.sungardas.enhancedsnapshots.dto.UserDto;
import com.sungardas.enhancedsnapshots.dto.converter.UserDtoConverter;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class UserDtoConverterTest {

    private static final String FIRST_NAME = "Alex";
    private static final String LAST_NAME = "Smith";
    private static final String EMAIL = "as@snap.com";
    private static final String USER_ROLE = "user";
    private static final String ADMIN_ROLE = "admin";
    private static final String PSW = "admin";


    @Test
    public void convertUserToUserDto() {
        // with user role
        User user = new User(EMAIL, PSW, USER_ROLE, FIRST_NAME, LAST_NAME);
        UserDto userDto = UserDtoConverter.convert(user);
        Assert.assertTrue(userDto.getEmail().equals(EMAIL));
        Assert.assertTrue(userDto.getFirstName().equals(FIRST_NAME));
        Assert.assertTrue(userDto.getLastName().equals(LAST_NAME));
        Assert.assertTrue(userDto.isAdmin() == false);

        // with admin role
        user.setRole(ADMIN_ROLE);
        userDto = UserDtoConverter.convert(user);
        Assert.assertTrue(userDto.isAdmin() == true);

    }

    @Test
    public void convertUserDtoToUser() {
        UserDto userDto = new UserDto();
        userDto.setEmail(EMAIL);
        userDto.setFirstName(FIRST_NAME);
        userDto.setLastName(LAST_NAME);
        User user = UserDtoConverter.convert(userDto);

        Assert.assertTrue(user.getEmail().equals(EMAIL));
        Assert.assertTrue(user.getFirstName().equals(FIRST_NAME));
        Assert.assertTrue(user.getLastName().equals(LAST_NAME));
    }

    @Test
    public void convertUsersToUserDtos() {
        User user_first = new User(EMAIL, PSW, USER_ROLE, FIRST_NAME, LAST_NAME);
        User user_second = new User(EMAIL + "_2", PSW + "_2", ADMIN_ROLE, FIRST_NAME + "_2", LAST_NAME + "_2");
        List<User> users = new ArrayList<>();
        users.add(user_first);
        users.add(user_second);

        List<UserDto> userDtoList = UserDtoConverter.convert(users);
        Assert.assertTrue(userDtoList.size() == 2);
        Assert.assertTrue(userDtoList.get(0).getFirstName().equals(FIRST_NAME));
        Assert.assertTrue(userDtoList.get(0).getLastName().equals(LAST_NAME));
        Assert.assertTrue(userDtoList.get(0).getEmail().equals(EMAIL));
        Assert.assertTrue(userDtoList.get(0).isAdmin() == false);

        Assert.assertTrue(userDtoList.get(1).getFirstName().equals(FIRST_NAME + "_2"));
        Assert.assertTrue(userDtoList.get(1).getLastName().equals(LAST_NAME + "_2"));
        Assert.assertTrue(userDtoList.get(1).getEmail().equals(EMAIL + "_2"));
        Assert.assertTrue(userDtoList.get(1).isAdmin() == true);
    }

}
