package com.clinica.availability.service;

import com.clinica.availability.dto.AvailabilityCheckRequestDTO;
import com.clinica.availability.dto.AvailabilityResponseDTO;
import com.clinica.availability.model.AvailabilityException;
import com.clinica.availability.model.AvailabilityPattern;
import com.clinica.availability.model.AvailabilityRule;
import com.clinica.availability.model.ResourceAvailability;
import com.clinica.availability.repository.ExceptionRepository;
import com.clinica.availability.repository.PatternRepository;
import com.clinica.availability.repository.RuleRepository;
import com.clinica.availability.repository.ResourceAvailabilityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class AvailabilityService {

    @Autowired
    private ResourceAvailabilityRepository availabilityRepository;

    @Autowired
    private PatternRepository patternRepository;

    @Autowired
    private ExceptionRepository exceptionRepository;

    @Autowired
    private RuleRepository ruleRepository;

    public AvailabilityResponseDTO checkAvailability(AvailabilityCheckRequestDTO request) {
        AvailabilityResponseDTO response = new AvailabilityResponseDTO();
        Long resourceId = request.getResourceId();
        LocalDateTime startTime = request.getStartTime();
        LocalDateTime endTime = request.getEndTime();

        // 1. Verificar se há exceções
        Optional<AvailabilityException> exception = exceptionRepository.findAll().stream()
                .filter(e -> e.getResourceId().equals(resourceId)
                        && e.getStartTime().isBefore(startTime)
                        && e.getEndTime().isAfter(endTime))
                .findFirst();

        if (exception.isPresent()) {
            response.setAvailable(false);
            response.setMessage("Indisponível devido a uma exceção.");
            return response;
        }

        // 2. Verificar regras de disponibilidade
        DayOfWeek dayOfWeek = startTime.getDayOfWeek();
        String dayOfWeekStr = dayOfWeek.toString();

        List<AvailabilityRule> rules = ruleRepository.findAll().stream()
                .filter(rule -> rule.getResourceId().equals(resourceId) && rule.getDayOfWeek().equals(dayOfWeekStr))
                .toList();

        boolean availableByRule = rules.stream().anyMatch(rule -> {
            LocalTime ruleStartTime = LocalTime.parse(rule.getStartTime(), DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime ruleEndTime = LocalTime.parse(rule.getEndTime(), DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime checkStartTime = startTime.toLocalTime();
            LocalTime checkEndTime = endTime.toLocalTime();

            return checkStartTime.isAfter(ruleStartTime) && checkEndTime.isBefore(ruleEndTime);
        });

        if (!availableByRule) {
            response.setAvailable(false);
            response.setMessage("Indisponível conforme as regras de disponibilidade.");
            return response;
        }

        // 3. Verificar padrões de recorrência (CRON)
        List<AvailabilityPattern> patterns = patternRepository.findAll().stream()
                .filter(pattern -> {
                    //TODO: Implementar lógica de verificação de CRON
                    return false;
                })
                .toList();

        if (!patterns.isEmpty()) {
            response.setAvailable(false);
            response.setMessage("Indisponível devido a um padrão de recorrência.");
            return response;
        }

        // 4. Verificar disponibilidade específica
        Optional<ResourceAvailability> specificAvailability = availabilityRepository.findAll().stream()
                .filter(a -> a.getResourceId().equals(resourceId)
                        && a.getStartTime().isBefore(startTime)
                        && a.getEndTime().isAfter(endTime))
                .findFirst();

        if (specificAvailability.isPresent() && !specificAvailability.get().isAvailable()) {
            response.setAvailable(false);
            response.setMessage("Indisponível conforme agendamento específico.");
            return response;
        }

        response.setAvailable(true);
        response.setMessage("Disponível.");
        return response;
    }
}