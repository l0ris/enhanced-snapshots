package com.sungardas.enhancedsnapshots.rest;

import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.User;
import com.sungardas.enhancedsnapshots.dto.UserDto;
import com.sungardas.enhancedsnapshots.rest.utils.Constants;
import com.sungardas.enhancedsnapshots.service.UserService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.support.BindingAwareModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/session")
public class AuthenticationController {

    private static final Logger LOG = LogManager.getLogger(AuthenticationController.class);

    @Autowired
    private ServletContext context;

    @Autowired
    private HttpServletRequest servletRequest;

    @Autowired
    private UserService userService;

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<UserDto> login(@RequestBody User user) {
        // check whether there is already user logged in with the same session id
        Map<String, String> allowedSessions = getAllowedSessions();
        String sessionId = servletRequest.getSession().getId();
        // JIRA: SNAP-102
        // Temp solution. Replace email which is mapped to session in case when another user logged in from the same browser
        if (!allowedSessions.get(sessionId).equals(user.getEmail().toLowerCase())) {
            allowedSessions.put(sessionId, user.getEmail().toLowerCase());
        }
        UserDto result = userService.getUser(user.getEmail().toLowerCase(), user.getPassword());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<UserDto> login(Model model) {
        User user = new User();
        user.setEmail((String) ((BindingAwareModelMap) model).get(Constants.JSON_AUTHENTIFICATION_EMAIL));
        user.setPassword((String) ((BindingAwareModelMap) model).get(Constants.JSON_AUTHENTIFICATION_PASSWORD));
        return login(user);
    }

    @RequestMapping(method = RequestMethod.DELETE)
    public ResponseEntity<Void> logout() {
        String sessionId = servletRequest.getSession().getId();
        Map<String, String> allowedSessions = getAllowedSessions();
        allowedSessions.remove(sessionId);
        LOG.debug("Logout for session: [{}].", sessionId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private Map<String, String> getAllowedSessions() {
        return (Map<String, String>) context.getAttribute(Constants.CONTEXT_ALLOWED_SESSIONS_ATR_NAME);
    }

}
