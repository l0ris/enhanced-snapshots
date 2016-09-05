package com.sungardas.enhancedsnapshots.rest;

import com.sungardas.enhancedsnapshots.dto.UserDto;
import com.sungardas.enhancedsnapshots.exception.DataAccessException;
import com.sungardas.enhancedsnapshots.exception.EnhancedSnapshotsException;
import com.sungardas.enhancedsnapshots.service.UserService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;
import java.io.IOException;
import java.security.Principal;
import java.util.Collections;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;


@RestController
@RequestMapping("/user")
public class UserController {


	private static final Log LOG = LogFactory.getLog(UserController.class);

	@Autowired
	private UserService userService;
	private ObjectMapper mapper;

	@ExceptionHandler(EnhancedSnapshotsException.class)
	@ResponseBody
	@ResponseStatus(INTERNAL_SERVER_ERROR)
	private Exception internalServerError(EnhancedSnapshotsException exception){
		LOG.error(exception);
		return exception;
	}

	@ExceptionHandler(IOException.class)
	@ResponseBody
	@ResponseStatus(INTERNAL_SERVER_ERROR)
	private Exception invalidInput(IOException exception){
		LOG.error(exception);
		return exception;
	}

	@RequestMapping(value = "/currentUser", method = RequestMethod.GET)
	public ResponseEntity getCurrentUser(Principal principal) {
		try {
			String role = userService.getUser(principal.getName()).getRole();
			return new ResponseEntity<>("{ \"role\":\"" + role
					+ "\", \"email\":\"" + principal.getName() + "\" }", HttpStatus.OK);
		} catch (DataAccessException e) {
			return new ResponseEntity<>(Collections.emptyList(), HttpStatus.NOT_ACCEPTABLE);
		}
	}

	@RolesAllowed("ROLE_ADMIN")
	@RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<String> createUser(@RequestBody String userInfo) throws IOException {
		// getting userDto from json
		UserDto user = getUserDtoFromJson(userInfo);
		// getting password
		String password = mapper.readValue(userInfo, ObjectNode.class).get("password").asText();
		userService.createUser(user, password);
		return new ResponseEntity<>("", HttpStatus.OK);
	}

	@RolesAllowed({"ROLE_ADMIN", "ROLE_USER"})
	@RequestMapping(method = RequestMethod.PUT)
	public ResponseEntity updateUser(Principal principal, @RequestBody String userInfo) throws IOException {
		// getting userDto from json
		UserDto user = getUserDtoFromJson(userInfo);

		// getting password
		String password = mapper.readValue(userInfo, ObjectNode.class).get("password").asText();

		userService.updateUser(user, password, principal.getName());
		return new ResponseEntity<>("", HttpStatus.OK);
	}

	@RolesAllowed({"ROLE_ADMIN", "ROLE_USER"})
	@RequestMapping(method = RequestMethod.GET)
	public ResponseEntity getAllUsers() {
		try {
			return new ResponseEntity<>(userService.getAllUsers(), HttpStatus.OK);
		} catch (DataAccessException e) {
			return new ResponseEntity<>(Collections.emptyList(), HttpStatus.NOT_ACCEPTABLE);
		}
	}

	@RolesAllowed("ROLE_ADMIN")
	@RequestMapping(value = "/{userEmail:.+}", method = RequestMethod.DELETE)
	public ResponseEntity removeUser(@PathVariable("userEmail") String userEmail) {
		userService.removeUser(userEmail);
		return new ResponseEntity<>("", HttpStatus.OK);
	}

	private UserDto getUserDtoFromJson(String json) throws IOException {
		if (mapper == null) {
			mapper = new ObjectMapper();
			mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		}
		return mapper.readValue(json, UserDto.class);
	}
}
