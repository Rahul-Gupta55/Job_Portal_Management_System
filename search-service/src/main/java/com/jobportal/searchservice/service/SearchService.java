package com.jobportal.searchservice.service;

import com.jobportal.searchservice.dto.PagedResponse;
import com.jobportal.searchservice.dto.SearchCriteria;
import com.jobportal.searchservice.dto.SearchJobResponse;

import java.util.List;

public interface SearchService {
    PagedResponse<SearchJobResponse> searchJobs(SearchCriteria criteria);

    List<String> suggestions(String q);
}
