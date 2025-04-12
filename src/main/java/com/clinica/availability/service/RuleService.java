package com.clinica.availability.service;

import com.clinica.availability.model.AvailabilityRule;
import com.clinica.availability.repository.RuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RuleService {

    @Autowired
    private RuleRepository ruleRepository;

    public List<AvailabilityRule> getAllRules() {
        return ruleRepository.findAll();
    }

    public Optional<AvailabilityRule> getRuleById(Long id) {
        return ruleRepository.findById(id);
    }

    public AvailabilityRule createRule(AvailabilityRule rule) {
        return ruleRepository.save(rule);
    }

    public Optional<AvailabilityRule> updateRule(Long id, AvailabilityRule rule) {
        return ruleRepository.findById(id)
                .map(existingRule -> {
                    rule.setId(id); // Garante que estamos atualizando a regra correta
                    return ruleRepository.save(rule);
                });
    }

    public void deleteRule(Long id) {
        ruleRepository.deleteById(id);
    }
}