package com.jobportal.searchservice.controller;

import com.jobportal.searchservice.dto.ApiResponse;
import com.jobportal.searchservice.dto.PagedResponse;
import com.jobportal.searchservice.dto.SearchCriteria;
import com.jobportal.searchservice.dto.SearchJobResponse;
import com.jobportal.searchservice.service.SearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

    @Mock
    private SearchService searchService;

    @InjectMocks
    private SearchController searchController;

    @Test
    void searchJobsDelegatesAllFiltersToService() {
        PagedResponse<SearchJobResponse> pagedResponse = PagedResponse.<SearchJobResponse>builder()
                .content(List.of(SearchJobResponse.builder().id(1L).title("Java Developer").build()))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .build();
        when(searchService.searchJobs(any(SearchCriteria.class))).thenReturn(pagedResponse);

        ResponseEntity<ApiResponse<PagedResponse<SearchJobResponse>>> response = searchController.searchJobs("java", "hyderabad", "FULL_TIME", 1, 3, "Acme", 0, 10);

        ArgumentCaptor<SearchCriteria> criteriaCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        verify(searchService).searchJobs(criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue())
                .extracting(SearchCriteria::getKeyword, SearchCriteria::getLocation, SearchCriteria::getType,
                        SearchCriteria::getExpMin, SearchCriteria::getExpMax, SearchCriteria::getCompany,
                        SearchCriteria::getPage, SearchCriteria::getSize)
                .containsExactly("java", "hyderabad", "FULL_TIME", 1, 3, "Acme", 0, 10);
        assertThat(response.getBody().getMessage()).isEqualTo("Search results fetched successfully");
        assertThat(response.getBody().getData()).isEqualTo(pagedResponse);
    }

    @Test
    void suggestionsDelegatesQueryToService() {
        when(searchService.suggestions("ja")).thenReturn(List.of("Java Developer", "Java Architect"));

        ResponseEntity<ApiResponse<List<String>>> response = searchController.suggestions("ja");

        assertThat(response.getBody().getMessage()).isEqualTo("Suggestions fetched successfully");
        assertThat(response.getBody().getData()).containsExactly("Java Developer", "Java Architect");
    }
}
