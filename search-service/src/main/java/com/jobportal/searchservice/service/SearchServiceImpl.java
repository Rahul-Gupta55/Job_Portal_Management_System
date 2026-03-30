package com.jobportal.searchservice.service;

import com.jobportal.searchservice.dto.PagedResponse;
import com.jobportal.searchservice.dto.SearchCriteria;
import com.jobportal.searchservice.dto.SearchJobResponse;
import com.jobportal.searchservice.entity.SearchJob;
import com.jobportal.searchservice.repository.SearchJobRepository;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {

    private static final String OPEN_STATUS = "OPEN";

    private final SearchJobRepository searchJobRepository;

    @Override
    @RateLimiter(name = "searchEndpoint")
    public PagedResponse<SearchJobResponse> searchJobs(SearchCriteria criteria) {

        if (criteria.getPage() < 0 || criteria.getSize() <= 0) {
            log.warn("Invalid page or size received. page={}, size={}", criteria.getPage(), criteria.getSize());
        }

        int safePage = Math.max(criteria.getPage(), 0);
        int safeSize = criteria.getSize() <= 0 ? 10 : criteria.getSize();
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        var resultPage = searchJobRepository
                .findAll(buildSpecification(criteria), pageable)
                .map(this::mapToResponse);

        log.info("Job search completed. page={}, size={}, results={}, total={}",
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getNumberOfElements(),
                resultPage.getTotalElements());

        return PagedResponse.<SearchJobResponse>builder()
                .content(resultPage.getContent())
                .page(resultPage.getNumber())
                .size(resultPage.getSize())
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .build();
    }

    @Override
    @RateLimiter(name = "searchEndpoint")
    public List<String> suggestions(String q) {
        if (isBlank(q)) {
            log.warn("Blank query received for suggestions.");
        }

        List<String> suggestions = searchJobRepository
                .findAll((root, query, cb) -> cb.equal(cb.upper(root.get("status")), OPEN_STATUS),
                        Sort.by(Sort.Direction.ASC, "title"))
                .stream()
                .map(SearchJob::getTitle)
                .filter(title -> title != null && startsWithIgnoreCase(title, q))
                .distinct()
                .limit(10)
                .toList();

        log.info("Suggestions fetched successfully. count={}", suggestions.size());
        return suggestions;
    }

    private Specification<SearchJob> buildSpecification(SearchCriteria criteria) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<Predicate>();
            predicates.add(cb.equal(cb.upper(root.get("status")), OPEN_STATUS));

            if (!isBlank(criteria.getKeyword())) {
                String value = likeValue(criteria.getKeyword());
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), value),
                        cb.like(cb.lower(root.get("company")), value),
                        cb.like(cb.lower(root.get("location")), value)));
            }

            if (!isBlank(criteria.getLocation())) {
                predicates.add(cb.like(cb.lower(root.get("location")), likeValue(criteria.getLocation())));
            }

            if (!isBlank(criteria.getCompany())) {
                predicates.add(cb.like(cb.lower(root.get("company")), likeValue(criteria.getCompany())));
            }

            if (!isBlank(criteria.getType())) {
                predicates.add(cb.equal(cb.lower(root.get("jobType")), criteria.getType().trim().toLowerCase(Locale.ROOT)));
            }

            if (criteria.getExpMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("experienceMin"), criteria.getExpMin()));
            }

            if (criteria.getExpMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("experienceMax"), criteria.getExpMax()));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private boolean startsWithIgnoreCase(String value, String query) {
        if (isBlank(query)) {
            return true;
        }
        return value.toLowerCase(Locale.ROOT).startsWith(query.trim().toLowerCase(Locale.ROOT));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String likeValue(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private SearchJobResponse mapToResponse(SearchJob job) {
        return SearchJobResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .company(job.getCompany())
                .location(job.getLocation())
                .salaryRange(job.getSalaryRange())
                .jobType(job.getJobType())
                .status(job.getStatus())
                .experienceMin(job.getExperienceMin())
                .experienceMax(job.getExperienceMax())
                .deadline(job.getDeadline())
                .build();
    }
}