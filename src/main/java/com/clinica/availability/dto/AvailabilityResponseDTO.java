package com.clinica.availability.dto;

public class AvailabilityResponseDTO {

    private boolean available;
    private String message;

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}