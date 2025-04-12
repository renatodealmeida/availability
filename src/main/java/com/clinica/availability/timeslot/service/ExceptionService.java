package com.clinica.availability.service;

import com.clinica.availability.model.AvailabilityException;
import com.clinica.availability.repository.ExceptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ExceptionService {

    @Autowired
    private ExceptionRepository exceptionRepository;

    public List<AvailabilityException> getAllExceptions() {
        return exceptionRepository.findAll();
    }

    public Optional<AvailabilityException> getExceptionById(Long id) {
        return exceptionRepository.findById(id);
    }

    public AvailabilityException createException(AvailabilityException exception) {
        return exceptionRepository.save(exception);
    }

    public Optional<AvailabilityException> updateException(Long id, AvailabilityException exception) {
        return exceptionRepository.findById(id)
                .map(existingException -> {
                    exception.setId(id); // Garante que estamos atualizando a exceção correta
                    return exceptionRepository.save(exception);
                });
    }

    public void deleteException(Long id) {
        exceptionRepository.deleteById(id);
    }
}