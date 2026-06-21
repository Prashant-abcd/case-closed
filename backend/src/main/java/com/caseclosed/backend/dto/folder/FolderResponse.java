package com.caseclosed.backend.dto.folder;

import com.caseclosed.backend.entity.Folder;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class FolderResponse {
    private UUID id;
    private UUID suspectId;
    private String suspectName;
    private String contradictions;
    private String intentToMurder;

    public static FolderResponse from(Folder folder) {
        return FolderResponse.builder()
                .id(folder.getId())
                .suspectId(folder.getSuspect().getId())
                .suspectName(folder.getSuspect().getName())
                .contradictions(folder.getContradictions())
                .intentToMurder(folder.getIntentToMurder())
                .build();
    }
}
