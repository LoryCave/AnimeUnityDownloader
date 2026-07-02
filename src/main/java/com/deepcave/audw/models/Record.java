package com.deepcave.audw.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Record {
    private Integer id;
    private String title;
    private String imageurl;
    private String plot;
    private String date;
    private Integer episodes_count;
    private Integer episodes_length;
    private String status;
    private String type;
    private String title_eng;
    private String cover;
    private String title_it;
    private String slug;
}
