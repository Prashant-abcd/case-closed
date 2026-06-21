package com.caseclosed.backend.dto.folder;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFolderRequest {

    @Size(max = 2000, message = "Contradictions text must be less than 2000 characters")
    private String contradictions;

    @Size(max = 2000, message = "Intent to murder text must be less than 2000 characters")
    private String intentToMurder;
}
