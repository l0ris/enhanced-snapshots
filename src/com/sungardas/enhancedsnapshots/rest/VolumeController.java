package com.sungardas.enhancedsnapshots.rest;

import java.util.Set;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.sungardas.enhancedsnapshots.dto.VolumeDto;
import com.sungardas.enhancedsnapshots.service.VolumeService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.RolesAllowed;


@RestController
@RequestMapping("/volume")
public class VolumeController {

    @Autowired
    private VolumeService volumeService;

    @RolesAllowed({"ROLE_ADMIN", "ROLE_USER"})
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity getAllVolumes() {
        try {
            Set<VolumeDto> volumes = volumeService.getVolumes();
            return new ResponseEntity(volumes, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity("Failed to get volumes.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RolesAllowed({"ROLE_ADMIN", "ROLE_USER"})
    @RequestMapping(value = "/{regionId}", method = RequestMethod.GET)
    public ResponseEntity getVolumesByRegion(@PathVariable("regionId") String region) {
        try {
            Set<VolumeDto> volumes = volumeService.getVolumesByRegion(Region.getRegion(Regions.fromName(region)));
            return new ResponseEntity(volumes, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity("Failed to get volumes for region: " + region, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
