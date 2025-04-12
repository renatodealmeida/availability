package com.clinica.availability.controller;

import com.clinica.availability.model.AvailabilityPattern;
import com.clinica.availability.service.PatternService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/patterns")
public class PatternController {

    @Autowired
    private PatternService patternService;

    @GetMapping
    public ResponseEntity<List<AvailabilityPattern>> getAllPatterns() {
        return ResponseEntity.ok(patternService.getAllPatterns());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AvailabilityPattern> getPatternById(@PathVariable Long id) {
        return patternService.getPatternById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AvailabilityPattern> createPattern(@RequestBody AvailabilityPattern pattern) {
        AvailabilityPattern createdPattern = patternService.createPattern(pattern);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPattern);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AvailabilityPattern> updatePattern(@PathVariable Long id, @RequestBody AvailabilityPattern pattern) {
        return patternService.updatePattern(id, pattern)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePattern(@PathVariable Long id) {
        patternService.deletePattern(id);
        return ResponseEntity.noContent().build();
    }
}