package com.caseclosed.backend.dto.cases;

import com.caseclosed.backend.entity.Suspect;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class SuspectResponse {

    private UUID id;
    private String name;
    private int age;
    private String title;
    private String company;
    private String relationshipToFounder;
    private String avatarSeed;
    private int interrogationCount;
    @JsonProperty("isHighProfile")
    private boolean isHighProfile;
    private String backgroundCheck;

    public static SuspectResponse from(Suspect suspect) {
        return SuspectResponse.builder()
                .id(suspect.getId())
                .name(suspect.getName())
                .age(suspect.getAge())
                .title(suspect.getTitle())
                .company(suspect.getCompany())
                .relationshipToFounder(suspect.getRelationshipToFounder())
                .avatarSeed(suspect.getAvatarSeed())
                .interrogationCount(suspect.getInterrogationCount())
                .isHighProfile(suspect.isHighProfile())
                .backgroundCheck(suspect.getBackgroundCheck())
                .build();
    }
}
