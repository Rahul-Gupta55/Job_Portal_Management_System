package com.jobportal.searchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SearchCriteria {

    private String keyword;
    private String location;
    private String type;
    private Integer expMin;
    private Integer expMax;
    private String company;
    private int page;
    private int size;
}
