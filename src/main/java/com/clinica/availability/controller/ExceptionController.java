package com.clinica.availability.controller;

import com.clinica.availability.model.AvailabilityException;
import com.clinica.availability.service.ExceptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/exceptions")
public class ExceptionController {

    @Autowired
    private ExceptionService exceptionService;

    @GetMapping
    public ResponseEntity<List<AvailabilityException>> getAllExceptions() {
        return ResponseEntity.ok(exceptionService.getAllExceptions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AvailabilityException> getExceptionById(@PathVariable Long id) {
        return exceptionService.getExceptionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AvailabilityException> createException(@RequestBody AvailabilityException exception) {
        AvailabilityException createdException = exceptionService.createException(exception);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdException);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AvailabilityException> updateException(@PathVariable Long id, @RequestBody AvailabilityException exception) {
        return exceptionService.updateException(id, exception)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteException(@PathVariable Long id) {
        exceptionService.deleteException(id);
        return ResponseEntity.noContent().build();
    }
}