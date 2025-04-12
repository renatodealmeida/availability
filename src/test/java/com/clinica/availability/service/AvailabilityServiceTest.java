package com.clinica.availability.service;

import com.clinica.availability.dto.AvailabilityCheckRequestDTO;
import com.clinica.availability.dto.AvailabilityResponseDTO;
import com.clinica.availability.model.AvailabilityException;
import com.clinica.availability.model.AvailabilityRule;
import com.clinica.availability.model.ResourceAvailability;
import com.clinica.availability.repository.ExceptionRepository;
import com.clinica.availability.repository.RuleRepository;
import com.clinica.availability.repository.ResourceAvailabilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class AvailabilityServiceTest {

    @Mock
    private ResourceAvailabilityRepository availabilityRepository;

    @Mock
    private ExceptionRepository exceptionRepository;

    @Mock
    private RuleRepository ruleRepository;

    @InjectMocks
    private AvailabilityService availabilityService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCheckAvailability_exception() {
        // Arrange
        AvailabilityCheckRequestDTO request = new AvailabilityCheckRequestDTO();
        request.setResourceId(1L);
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(2));

        AvailabilityException exception = new AvailabilityException();
        exception.setResourceId(1L);
        exception.setStartTime(LocalDateTime.now());
        exception.setEndTime(LocalDateTime.now().plusDays(1));

        when(exceptionRepository.findAll()).thenReturn(List.of(exception));

        // Act
        AvailabilityResponseDTO response = availabilityService.checkAvailability(request);

        // Assert
        assertFalse(response.isAvailable());
        assertEquals("Indisponível devido a uma exceção.", response.getMessage());
    }

    @Test
    void testCheckAvailability_rule() {
        // Arrange
        AvailabilityCheckRequestDTO request = new AvailabilityCheckRequestDTO();
        request.setResourceId(1L);
        request.setStartTime(LocalDateTime.parse("2024-01-02T10:00:00")); // Tuesday
        request.setEndTime(LocalDateTime.parse("2024-01-02T11:00:00"));

        AvailabilityRule rule = new AvailabilityRule();
        rule.setResourceId(1L);
        rule.setDayOfWeek(DayOfWeek.TUESDAY.toString());
        rule.setStartTime("08:00");
        rule.setEndTime("09:00");

        when(ruleRepository.findAll()).thenReturn(List.of(rule));

        // Act
        AvailabilityResponseDTO response = availabilityService.checkAvailability(request);

        // Assert
        assertFalse(response.isAvailable());
        assertEquals("Indisponível conforme as regras de disponibilidade.", response.getMessage());
    }

    @Test
    void testCheckAvailability_specificAvailability() {
        // Arrange
        AvailabilityCheckRequestDTO request = new AvailabilityCheckRequestDTO();
        request.setResourceId(1L);
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(2));

        ResourceAvailability specificAvailability = new ResourceAvailability();
        specificAvailability.setResourceId(1L);
        specificAvailability.setStartTime(LocalDateTime.now());
        specificAvailability.setEndTime(LocalDateTime.now().plusDays(1));
        specificAvailability.setAvailable(false);

        when(availabilityRepository.findAll()).thenReturn(List.of(specificAvailability));

        // Act
        AvailabilityResponseDTO response = availabilityService.checkAvailability(request);

        // Assert
        assertFalse(response.isAvailable());
        assertEquals("Indisponível conforme agendamento específico.", response.getMessage());
    }

    @Test
    void testCheckAvailability_available() {
        // Arrange
        AvailabilityCheckRequestDTO request = new AvailabilityCheckRequestDTO();
        request.setResourceId(1L);
        request.setStartTime(LocalDateTime.parse("2024-01-02T10:00:00")); // Tuesday
        request.setEndTime(LocalDateTime.parse("2024-01-02T11:00:00"));

        AvailabilityRule rule = new AvailabilityRule();
        rule.setResourceId(1L);
        rule.setDayOfWeek(DayOfWeek.TUESDAY.toString());
        rule.setStartTime("09:00");
        rule.setEndTime("12:00");

        when(ruleRepository.findAll()).thenReturn(List.of(rule));

        // Act
        AvailabilityResponseDTO response = availabilityService.checkAvailability(request);

        // Assert
        assertTrue(response.isAvailable());
        assertEquals("Disponível.", response.getMessage());
    }
}