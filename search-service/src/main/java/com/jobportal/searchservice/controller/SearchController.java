package com.jobportal.searchservice.controller;

import com.jobportal.searchservice.dto.ApiResponse;
import com.jobportal.searchservice.dto.PagedResponse;
import com.jobportal.searchservice.dto.SearchCriteria;
import com.jobportal.searchservice.dto.SearchJobResponse;
import com.jobportal.searchservice.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Search Service", description = "Simple job search APIs")
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/jobs")
    @Operation(summary = "Search jobs with basic filters")
    public ResponseEntity<ApiResponse<PagedResponse<SearchJobResponse>>> searchJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer expMin,
            @RequestParam(required = false) Integer expMax,
            @RequestParam(required = false) String company,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        SearchCriteria criteria = SearchCriteria.builder()
                .keyword(keyword)
                .location(location)
                .type(type)
                .expMin(expMin)
                .expMax(expMax)
                .company(company)
                .page(page)
                .size(size)
                .build();

        return ResponseEntity.ok(ApiResponse.of(
                "Search results fetched successfully",
                searchService.searchJobs(criteria)
        ));
    }

    @GetMapping("/suggestions")
    @Operation(summary = "Get title suggestions")
    public ResponseEntity<ApiResponse<List<String>>> suggestions(@RequestParam(defaultValue = "") String q) {
        return ResponseEntity.ok(ApiResponse.of("Suggestions fetched successfully", searchService.suggestions(q)));
    }
}
