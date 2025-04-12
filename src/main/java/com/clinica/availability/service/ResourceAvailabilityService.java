package com.clinica.availability.service;

import com.clinica.availability.model.ResourceAvailability;
import com.clinica.availability.repository.ResourceAvailabilityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ResourceAvailabilityService {

    @Autowired
    private ResourceAvailabilityRepository resourceAvailabilityRepository;

    public List<ResourceAvailability> getAllResourceAvailabilities() {
        return resourceAvailabilityRepository.findAll();
    }

    public Optional<ResourceAvailability> getResourceAvailabilityById(Long id) {
        return resourceAvailabilityRepository.findById(id);
    }

    public ResourceAvailability createResourceAvailability(ResourceAvailability resourceAvailability) {
        return resourceAvailabilityRepository.save(resourceAvailability);
    }

    public Optional<ResourceAvailability> updateResourceAvailability(Long id, ResourceAvailability resourceAvailability) {
        return resourceAvailabilityRepository.findById(id)
                .map(existingResourceAvailability -> {
                    resourceAvailability.setId(id); // Garante que estamos atualizando a disponibilidade correta
                    return resourceAvailabilityRepository.save(resourceAvailability);
                });
    }

    public void deleteResourceAvailability(Long id) {
        resourceAvailabilityRepository.deleteById(id);
    }
}