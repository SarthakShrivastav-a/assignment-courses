package api.assignment.backend.controller;

import api.assignment.backend.dto.search.SearchResponse;
import api.assignment.backend.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<SearchResponse> search(@RequestParam("q") String query) {
        return ResponseEntity.ok(searchService.search(query));
    }
}
