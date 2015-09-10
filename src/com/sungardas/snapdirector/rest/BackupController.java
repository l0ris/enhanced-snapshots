package com.sungardas.snapdirector.rest;

import com.sungardas.snapdirector.aws.dynamodb.model.BackupEntry;
import com.sungardas.snapdirector.exception.DataAccessException;
import com.sungardas.snapdirector.exception.DataException;
import com.sungardas.snapdirector.rest.utils.Constants;
import com.sungardas.snapdirector.service.BackupService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/backup")
public class BackupController {

    private static final Logger LOG = LogManager.getLogger(BackupController.class);

    @Autowired
    private ServletContext context;
    @Autowired
    private HttpServletRequest servletRequest;

    @Autowired
    private BackupService backupService;


    @ExceptionHandler(DataException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    private DataException dataException(DataException e) {
        return e;
    }

    @RequestMapping(value = "/{volumeId}", method = RequestMethod.GET)
    public ResponseEntity<String> get(@PathVariable(value = "volumeId") String volumeId) {
        List<BackupEntry> items = backupService.getBackupList(volumeId);
        LOG.debug("Available backups for volume {}: [{}] .", volumeId, jsonArrayRepresentation(items).toString());
        return new ResponseEntity<>(jsonArrayRepresentation(items).toString(), HttpStatus.OK);
    }

    private JSONArray jsonArrayRepresentation(List<BackupEntry> backupEntries) {
        JSONArray backupsJSONArray = new JSONArray();
        for (BackupEntry entry : backupEntries) {
            JSONObject backupItem = new JSONObject();
            backupItem.put("fileName", entry.getFileName());
            backupItem.put("volumeId", entry.getVolumeId());
            backupItem.put("timeCreated", entry.getTimeCreated());
            backupItem.put("size", entry.getSize());
            backupsJSONArray.put(backupItem);
        }
        return backupsJSONArray;
    }


    @RequestMapping(value = "/{backupName}", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteBackup(@PathVariable String backupName) {
        LOG.debug("Removing backup [{}]", backupName);
        try {
            backupService.deleteBackup(backupName, getCurrentUserEmail());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (DataAccessException e) {
            return new ResponseEntity<>("Failed to remove backup.", HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @RequestMapping(value = "/system", method = RequestMethod.GET)
    public ResponseEntity<SystemBackupDto> getSystem() {
        //TODO add impl
        return new ResponseEntity<>(new SystemBackupDto(DateTime.now().getMillis()), HttpStatus.OK);
    }

    private String getCurrentUserEmail() {
        String session = servletRequest.getSession().getId();
        return ((Map<String, String>) context.getAttribute(Constants.CONTEXT_ALLOWED_SESSIONS_ATR_NAME)).get(session);
    }

    private class SystemBackupDto {
        private long lastBackup;

        public SystemBackupDto(long lastBackup) {
            this.lastBackup = lastBackup;
        }

        public long getLastBackup() {
            return lastBackup;
        }

        public void setLastBackup(long lastBackup) {
            this.lastBackup = lastBackup;
        }
    }
}
