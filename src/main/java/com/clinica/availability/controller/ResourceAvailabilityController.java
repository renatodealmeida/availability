package com.clinica.availability.controller;

import com.clinica.availability.model.ResourceAvailability;
import com.clinica.availability.service.ResourceAvailabilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/resource-availability")
public class ResourceAvailabilityController {

    @Autowired
    private ResourceAvailabilityService resourceAvailabilityService;

    @GetMapping
    public ResponseEntity<List<ResourceAvailability>> getAllResourceAvailabilities() {
        return ResponseEntity.ok(resourceAvailabilityService.getAllResourceAvailabilities());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResourceAvailability> getResourceAvailabilityById(@PathVariable Long id) {
        return resourceAvailabilityService.getResourceAvailabilityById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ResourceAvailability> createResourceAvailability(@RequestBody ResourceAvailability resourceAvailability) {
        ResourceAvailability createdResourceAvailability = resourceAvailabilityService.createResourceAvailability(resourceAvailability);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdResourceAvailability);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResourceAvailability> updateResourceAvailability(@PathVariable Long id, @RequestBody ResourceAvailability resourceAvailability) {
        return resourceAvailabilityService.updateResourceAvailability(id, resourceAvailability)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResourceAvailability(@PathVariable Long id) {
        resourceAvailabilityService.deleteResourceAvailability(id);
        return ResponseEntity.noContent().build();
    }
}