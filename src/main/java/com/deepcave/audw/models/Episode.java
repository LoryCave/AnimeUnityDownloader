package com.deepcave.audw.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Episode {
    private Integer id;
    private Integer anime_id;
    private String number;
}
