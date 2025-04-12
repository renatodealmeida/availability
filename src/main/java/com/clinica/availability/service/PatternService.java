package com.clinica.availability.service;

import com.clinica.availability.model.AvailabilityPattern;
import com.clinica.availability.repository.PatternRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PatternService {

    @Autowired
    private PatternRepository patternRepository;

    public List<AvailabilityPattern> getAllPatterns() {
        return patternRepository.findAll();
    }

    public Optional<AvailabilityPattern> getPatternById(Long id) {
        return patternRepository.findById(id);
    }

    public AvailabilityPattern createPattern(AvailabilityPattern pattern) {
        return patternRepository.save(pattern);
    }

    public Optional<AvailabilityPattern> updatePattern(Long id, AvailabilityPattern pattern) {
        return patternRepository.findById(id)
                .map(existingPattern -> {
                    pattern.setId(id); // Garante que estamos atualizando o padrão correto
                    return patternRepository.save(pattern);
                });
    }

    public void deletePattern(Long id) {
        patternRepository.deleteById(id);
    }
}