package com.sungardas.enhancedsnapshots.rest;

import com.sungardas.enhancedsnapshots.dto.RetentionDto;
import com.sungardas.enhancedsnapshots.exception.DataAccessException;
import com.sungardas.enhancedsnapshots.exception.EnhancedSnapshotsException;
import com.sungardas.enhancedsnapshots.service.RetentionService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@RequestMapping("/retention")
public class RetentionController {
    private static final Logger LOG = LogManager.getLogger(RetentionController.class);

    @Autowired
    private RetentionService retentionService;

    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Internal enhancedsnapshots error, see logs")
    @ExceptionHandler(EnhancedSnapshotsException.class)
    public void exceptionHandler(EnhancedSnapshotsException e){
        LOG.error(e);
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Volume not found")
    @ExceptionHandler(DataAccessException.class)
    public void dataAccessExceptionHandler(DataAccessException e){
        LOG.error(e);
    }

    @RequestMapping(method = POST)
    public ResponseEntity<Void> modifyRetentionPolicy(@RequestBody RetentionDto retentionDto){
        retentionService.putRetention(retentionDto);
        return new ResponseEntity(HttpStatus.OK);
    }


    @RequestMapping(value = "/{volumeId}", method = GET)
    public ResponseEntity<RetentionDto> getRetention(@PathVariable String volumeId){
        return new ResponseEntity<>(retentionService.getRetentionDto(volumeId), HttpStatus.OK);
    }

}
