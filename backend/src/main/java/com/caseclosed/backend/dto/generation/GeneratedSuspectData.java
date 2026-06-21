package com.caseclosed.backend.dto.generation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneratedSuspectData {
    private String name;
    private int age;
    private String title;
    private String company;
    private String relationshipToFounder;
    private String avatarSeed;
    @JsonProperty("isKiller")
    private boolean isKiller;
    @JsonProperty("isHighProfile")
    private boolean isHighProfile;
    private String trueAlibi;
    private String lieTheyTell;
    private String contradiction;
    private String motiveToKill;
    private String personality;
    private String backgroundCheck;
}
