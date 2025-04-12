package com.clinica.availability.controller;

import com.clinica.availability.dto.AvailabilityCheckRequestDTO;
import com.clinica.availability.dto.AvailabilityResponseDTO;
import com.clinica.availability.service.AvailabilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/availability")
public class AvailabilityController {

    @Autowired
    private AvailabilityService availabilityService;

    @PostMapping("/check")
    public ResponseEntity<AvailabilityResponseDTO> checkAvailability(@RequestBody AvailabilityCheckRequestDTO request) {
        AvailabilityResponseDTO response = availabilityService.checkAvailability(request);
        return ResponseEntity.ok(response);
    }
}