package com.sungardas.enhancedsnapshots.rest;

import java.text.ParseException;
import java.util.UUID;

import com.sungardas.enhancedsnapshots.dto.MessageDto;
import com.sungardas.enhancedsnapshots.dto.TaskDto;
import com.sungardas.enhancedsnapshots.exception.EnhancedSnapshotsException;
import com.sungardas.enhancedsnapshots.service.TaskService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.RolesAllowed;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;


@RestController
@RequestMapping("/task")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @ExceptionHandler(EnhancedSnapshotsException.class)
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ResponseBody
    private EnhancedSnapshotsException enhancedSnapshotsException(EnhancedSnapshotsException e) {
        return e;
    }

    @RolesAllowed({"ROLE_ADMIN", "ROLE_USER"})
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity getTasks() throws ParseException {
        try {
            return new ResponseEntity(taskService.getAllTasks(), OK);
        } catch (EnhancedSnapshotsException e) {
            return new ResponseEntity(e.getMessage(), HttpStatus.NO_CONTENT);
        }
    }

    @RolesAllowed({"ROLE_ADMIN", "ROLE_USER"})
    @RequestMapping(method = RequestMethod.GET, value = "/{volumeId}")
    public ResponseEntity getTasks(@PathVariable String volumeId) throws ParseException {
        try {
            return new ResponseEntity(taskService.getAllTasks(volumeId), OK);
        } catch (EnhancedSnapshotsException e) {
            return new ResponseEntity(e.getMessage(), HttpStatus.NO_CONTENT);
        }
    }

    @RolesAllowed({"ROLE_ADMIN", "ROLE_USER"})
    @RequestMapping(method = RequestMethod.GET, value = "/regular/{valueId}")
    public ResponseEntity getRegularTasks(@PathVariable String valueId) throws ParseException {
        try {
            return new ResponseEntity(taskService.getAllRegularTasks(valueId), OK);
        } catch (EnhancedSnapshotsException e) {
            return new ResponseEntity(e.getMessage(), HttpStatus.NO_CONTENT);
        }
    }

    @RolesAllowed({"ROLE_ADMIN", "ROLE_USER"})
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<MessageDto> addTask(@RequestBody TaskDto taskInfo) {
        taskInfo.setId(UUID.randomUUID().toString());

        return new ResponseEntity(new MessageDto(taskService.createTask(taskInfo)), OK);
    }

    @RolesAllowed({"ROLE_ADMIN", "ROLE_USER"})
    @RequestMapping(method = RequestMethod.PUT)
    public ResponseEntity updateTask(@RequestBody TaskDto taskInfo) {
        try {
            taskService.updateTask(taskInfo);
            return new ResponseEntity("", OK);
        } catch (EnhancedSnapshotsException e) {
            return new ResponseEntity(e.getMessage(), HttpStatus.NO_CONTENT);
        }
    }

    @RolesAllowed({"ROLE_ADMIN", "ROLE_USER"})
    @RequestMapping(value = "/{taskId}", method = RequestMethod.DELETE)
    @ResponseStatus(OK)
    public void removeTask(@PathVariable String taskId) {
        taskService.removeTask(taskId);
    }
}
