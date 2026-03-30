package com.jobportal.searchservice.service;

import com.jobportal.searchservice.dto.PagedResponse;
import com.jobportal.searchservice.dto.SearchCriteria;
import com.jobportal.searchservice.dto.SearchJobResponse;
import com.jobportal.searchservice.entity.SearchJob;
import com.jobportal.searchservice.repository.SearchJobRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceImplTest {

    @Mock
    private SearchJobRepository searchJobRepository;

    @InjectMocks
    private SearchServiceImpl searchService;

    @Test
    void searchJobsReturnsMappedPagedResponse() {
        SearchJob job = SearchJob.builder()
                .id(1L)
                .title("Java Developer")
                .company("Acme")
                .location("Hyderabad")
                .salaryRange("12 LPA")
                .jobType("FULL_TIME")
                .status("OPEN")
                .experienceMin(1)
                .experienceMax(3)
                .deadline(LocalDate.now().plusDays(20))
                .createdAt(LocalDateTime.now())
                .build();

        Page<SearchJob> page = new PageImpl<>(List.of(job), PageRequest.of(0, 10), 1);

        when(searchJobRepository.findAll(
                ArgumentMatchers.<Specification<SearchJob>>any(),
                any(Pageable.class)
        )).thenReturn(page);

        PagedResponse<SearchJobResponse> result =
                searchService.searchJobs(SearchCriteria.builder()
                        .keyword("java")
                        .location("hyderabad")
                        .type("FULL_TIME")
                        .expMin(1)
                        .expMax(3)
                        .company("Acme")
                        .page(0)
                        .size(10)
                        .build());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Java Developer");
        assertThat(result.getContent().get(0).getStatus()).isEqualTo("OPEN");
    }

    @Test
    void searchJobsNormalizesNegativePageAndInvalidSize() {
        Page<SearchJob> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(searchJobRepository.findAll(
                ArgumentMatchers.<Specification<SearchJob>>any(),
                any(Pageable.class)
        )).thenReturn(emptyPage);

        searchService.searchJobs(SearchCriteria.builder().page(-5).size(0).build());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(searchJobRepository).findAll(
                ArgumentMatchers.<Specification<SearchJob>>any(),
                pageableCaptor.capture()
        );

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable)
                .extracting(Pageable::getPageSize, Pageable::getSort)
                .containsExactly(10, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Test
    void suggestionsReturnsDistinctOpenTitlesOnly() {
        List<SearchJob> jobs = List.of(
                SearchJob.builder().id(1L).title("Java Developer").status("OPEN").createdAt(LocalDateTime.now()).build(),
                SearchJob.builder().id(2L).title("Java Developer").status("OPEN").createdAt(LocalDateTime.now()).build(),
                SearchJob.builder().id(3L).title("Java Architect").status("OPEN").createdAt(LocalDateTime.now()).build()
        );

        when(searchJobRepository.findAll(
                ArgumentMatchers.<Specification<SearchJob>>any(),
                eq(Sort.by(Sort.Direction.ASC, "title"))
        )).thenReturn(jobs);

        List<String> result = searchService.suggestions("Ja");

        assertThat(result).containsExactly("Java Developer", "Java Architect");
    }

    @Test
    void suggestionsWithBlankQueryReturnsFirstTenTitles() {
        List<SearchJob> jobs = List.of(
                SearchJob.builder().id(1L).title("A").status("OPEN").createdAt(LocalDateTime.now()).build(),
                SearchJob.builder().id(2L).title("B").status("OPEN").createdAt(LocalDateTime.now()).build(),
                SearchJob.builder().id(3L).title("C").status("OPEN").createdAt(LocalDateTime.now()).build(),
                SearchJob.builder().id(4L).title("D").status("OPEN").createdAt(LocalDateTime.now()).build(),
                SearchJob.builder().id(5L).title("E").status("OPEN").createdAt(LocalDateTime.now()).build(),
                SearchJob.builder().id(6L).title("F").status("OPEN").createdAt(LocalDateTime.now()).build(),
                SearchJob.builder().id(7L).title("G").status("OPEN").createdAt(LocalDateTime.now()).build(),
                SearchJob.builder().id(8L).title("H").status("OPEN").createdAt(LocalDateTime.now()).build(),
                SearchJob.builder().id(9L).title("I").status("OPEN").createdAt(LocalDateTime.now()).build(),
                SearchJob.builder().id(10L).title("J").status("OPEN").createdAt(LocalDateTime.now()).build(),
                SearchJob.builder().id(11L).title("K").status("OPEN").createdAt(LocalDateTime.now()).build()
        );

        when(searchJobRepository.findAll(
                ArgumentMatchers.<Specification<SearchJob>>any(),
                eq(Sort.by(Sort.Direction.ASC, "title"))
        )).thenReturn(jobs);

        List<String> result = searchService.suggestions(" ");

        assertThat(result).hasSize(10);
        assertThat(result).containsExactly("A", "B", "C", "D", "E", "F", "G", "H", "I", "J");
    }


    @Test
    void suggestionsSkipsNullTitlesAndNonMatchingValues() {
        List<SearchJob> jobs = List.of(
                SearchJob.builder().id(1L).title(null).status("OPEN").createdAt(LocalDateTime.now()).build(),
                SearchJob.builder().id(2L).title("Python Developer").status("OPEN").createdAt(LocalDateTime.now()).build(),
                SearchJob.builder().id(3L).title("Java Engineer").status("OPEN").createdAt(LocalDateTime.now()).build()
        );

        when(searchJobRepository.findAll(
                ArgumentMatchers.<Specification<SearchJob>>any(),
                eq(Sort.by(Sort.Direction.ASC, "title"))
        )).thenReturn(jobs);

        List<String> result = searchService.suggestions("Ja");

        assertThat(result).containsExactly("Java Engineer");
    }


    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void searchJobsBuildsPredicatesForProvidedFilters() {
        when(searchJobRepository.findAll(
                ArgumentMatchers.<Specification<SearchJob>>any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        SearchCriteria criteria = SearchCriteria.builder()
                .keyword("Java")
                .location("Hyderabad")
                .type("FULL_TIME")
                .expMin(1)
                .expMax(3)
                .company("Acme")
                .page(0)
                .size(10)
                .build();

        searchService.searchJobs(criteria);

        ArgumentCaptor<Specification<SearchJob>> specificationCaptor = ArgumentCaptor.forClass((Class) Specification.class);
        verify(searchJobRepository).findAll(specificationCaptor.capture(), any(Pageable.class));

        Root<SearchJob> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Path<String> statusPath = mock(Path.class);
        Path<String> titlePath = mock(Path.class);
        Path<String> companyPath = mock(Path.class);
        Path<String> locationPath = mock(Path.class);
        Path<String> jobTypePath = mock(Path.class);
        Path<Integer> expMinPath = mock(Path.class);
        Path<Integer> expMaxPath = mock(Path.class);
        Expression<String> upperStatus = mock(Expression.class);
        Expression<String> lowerTitle = mock(Expression.class);
        Expression<String> lowerCompany = mock(Expression.class);
        Expression<String> lowerLocation = mock(Expression.class);
        Expression<String> lowerJobType = mock(Expression.class);
        Predicate predicate = mock(Predicate.class);

        when(root.get("status")).thenReturn((Path) statusPath);
        when(root.get("title")).thenReturn((Path) titlePath);
        when(root.get("company")).thenReturn((Path) companyPath);
        when(root.get("location")).thenReturn((Path) locationPath);
        when(root.get("jobType")).thenReturn((Path) jobTypePath);
        when(root.get("experienceMin")).thenReturn((Path) expMinPath);
        when(root.get("experienceMax")).thenReturn((Path) expMaxPath);
        when(criteriaBuilder.upper(statusPath)).thenReturn(upperStatus);
        when(criteriaBuilder.equal(upperStatus, "OPEN")).thenReturn(predicate);
        when(criteriaBuilder.lower(titlePath)).thenReturn(lowerTitle);
        when(criteriaBuilder.lower(companyPath)).thenReturn(lowerCompany);
        when(criteriaBuilder.lower(locationPath)).thenReturn(lowerLocation);
        when(criteriaBuilder.lower(jobTypePath)).thenReturn(lowerJobType);
        when(criteriaBuilder.like(lowerTitle, "%java%")).thenReturn(predicate);
        when(criteriaBuilder.like(lowerCompany, "%java%")).thenReturn(predicate);
        when(criteriaBuilder.like(lowerLocation, "%java%")).thenReturn(predicate);
        when(criteriaBuilder.like(lowerLocation, "%hyderabad%")).thenReturn(predicate);
        when(criteriaBuilder.like(lowerCompany, "%acme%")).thenReturn(predicate);
        when(criteriaBuilder.equal(lowerJobType, "full_time")).thenReturn(predicate);
        when(criteriaBuilder.greaterThanOrEqualTo(expMinPath, 1)).thenReturn(predicate);
        when(criteriaBuilder.lessThanOrEqualTo(expMaxPath, 3)).thenReturn(predicate);
        when(criteriaBuilder.or(any(Predicate.class), any(Predicate.class), any(Predicate.class))).thenReturn(predicate);
        when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(predicate);

        specificationCaptor.getValue().toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).like(lowerTitle, "%java%");
        verify(criteriaBuilder).like(lowerCompany, "%acme%");
        verify(criteriaBuilder).like(lowerLocation, "%hyderabad%");
        verify(criteriaBuilder).equal(lowerJobType, "full_time");
        verify(criteriaBuilder).greaterThanOrEqualTo(expMinPath, 1);
        verify(criteriaBuilder).lessThanOrEqualTo(expMaxPath, 3);
    }
}