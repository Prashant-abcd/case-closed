package com.caseclosed.backend.dto.interrogation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InterrogationRequest {

    @NotBlank(message = "Message cannot be empty")
    @Size(max = 2000, message = "Message cannot exceed 2000 characters")
    private String message;
}
