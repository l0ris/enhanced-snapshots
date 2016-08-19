package com.sungardas.enhancedsnapshots.rest;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.AvailabilityZone;
import com.sungardas.enhancedsnapshots.exception.DataAccessException;
import com.sungardas.enhancedsnapshots.service.AWSCommunicationService;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.RolesAllowed;


@RestController
public class RegionsController {

    @Autowired
    AWSCommunicationService communicationService;

    @RolesAllowed({"ROLE_ADMIN", "ROLE_USER"})
    @RequestMapping(value = "/regions", method = RequestMethod.GET)
    public String getRegions() {
        JSONObject record;
        JSONArray regionsJson = new JSONArray();
        System.out.println("Regions load");
        for (Regions nextRegion : Regions.values()) {    
            record = new JSONObject();
            record.put("id", nextRegion.getName());
            record.put("name", nextRegion.getName());
            regionsJson.put(record);
         }
        return regionsJson.toString();
    }

    @RolesAllowed({"ROLE_ADMIN", "ROLE_USER"})
    @RequestMapping( value = "/zones", method = RequestMethod.GET)
    public ResponseEntity getAvailabilityZones() {
        try {
            HashSet<String> availabilityZones = new HashSet<>();
            for (AvailabilityZone zone : communicationService.describeAvailabilityZonesForCurrentRegion()) {
                availabilityZones.add(zone.getZoneName());
            }
            return new ResponseEntity<>(availabilityZones, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to get list of availability zones.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RolesAllowed({"ROLE_ADMIN", "ROLE_USER"})
    @RequestMapping( value = "/zones/current", method = RequestMethod.GET)
    public ResponseEntity getCurrentAvailabilityZone() {
        try {
            JSONObject record = new JSONObject();;
            record.put("zone-name", communicationService.getCurrentAvailabilityZone());

            return new ResponseEntity<String>(record.toString(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to get current availability zone.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
