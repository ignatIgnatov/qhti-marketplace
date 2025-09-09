package com.platform.ads.controller;

import com.platform.ads.dto.BoatAdRequest;
import com.platform.ads.dto.BoatAdResponse;
import com.platform.ads.dto.BoatMarketplaceStatsResponse;
import com.platform.ads.dto.BoatSearchRequest;
import com.platform.ads.dto.enums.MainBoatCategory;
import com.platform.ads.exception.AdNotFoundException;
import com.platform.ads.exception.AuthServiceException;
import com.platform.ads.exception.CategoryMismatchException;
import com.platform.ads.exception.ErrorResponse;
import com.platform.ads.exception.InvalidFieldValueException;
import com.platform.ads.exception.MandatoryFieldMissingException;
import com.platform.ads.exception.UserNotFoundException;
import com.platform.ads.service.BoatMarketplaceService;
import com.platform.ads.service.BoatSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/ads")
@RequiredArgsConstructor
@Validated
@Tag(name = "Boat Marketplace", description = "QHTI.BG Boat Marketplace API for managing and searching boat advertisements across all categories")
public class BoatMarketplaceController {

    private final BoatMarketplaceService marketplaceService;
    private final BoatSearchService searchService;

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Create advertisement with images",
            description = "Creates a new boat advertisement with mandatory images. At least 1 image is required, maximum 14 images allowed."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Advertisement created successfully",
                    content = @Content(schema = @Schema(implementation = BoatAdResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data or missing images"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
            @ApiResponse(responseCode = "413", description = "Image file too large"),
            @ApiResponse(responseCode = "422", description = "Validation failed - invalid image format or brand"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Mono<ResponseEntity<BoatAdResponse>> createAdWithImages(
            @Parameter(description = "Advertisement data", required = true)
            @RequestPart("adData") @Valid BoatAdRequest adRequest,

            @Parameter(description = "Image files (1-14 images, max 10MB each, JPEG/PNG/WEBP/HEIC)", required = true)
            @RequestPart("images") Flux<FilePart> images,

            @Parameter(description = "JWT token", required = true)
            @RequestHeader("Authorization") String authHeader) {

        long startTime = System.currentTimeMillis();
        String token = extractTokenFromHeader(authHeader);

        log.info("=== CREATE AD WITH IMAGES REQUEST === User: {}, Category: {} ===",
                adRequest.getUserEmail(), adRequest.getCategory());

        return marketplaceService.createBoatAdWithImages(adRequest, images, token)
                .map(response -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("=== CREATE AD WITH IMAGES RESPONSE === ID: {}, Duration: {}ms ===",
                            response.getId(), duration);
                    return ResponseEntity.status(HttpStatus.CREATED).body(response);
                })
                .doOnError(error -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("=== CREATE AD WITH IMAGES ERROR === User: {}, Duration: {}ms, Error: {} ===",
                            adRequest.getUserEmail(), duration, error.getMessage());
                });
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get advertisement by ID",
            description = "Retrieves a specific advertisement by its ID. Automatically increments view count."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Advertisement found and returned successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BoatAdResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Advertisement not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public Mono<ResponseEntity<BoatAdResponse>> getAdById(
            @Parameter(description = "Advertisement ID", required = true, example = "123")
            @PathVariable @NotNull @Min(1) Long id) {

        long startTime = System.currentTimeMillis();
        log.info("=== GET AD BY ID REQUEST === ID: {} ===", id);

        return marketplaceService.getAdById(id)
                .map(response -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("=== GET AD SUCCESS === ID: {}, Category: {}, Views: {}, Duration: {}ms ===",
                            id, response.getCategory(), response.getViewsCount(), duration);
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(AdNotFoundException.class, e -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.warn("=== AD NOT FOUND === ID: {}, Duration: {}ms ===", id, duration);
                    return Mono.just(ResponseEntity.notFound().build());
                });
//                .onErrorResume(error -> {
//                    long duration = System.currentTimeMillis() - startTime;
//                    log.error("=== GET AD ERROR === ID: {}, Duration: {}ms, Error: {} ===",
//                            id, duration, error.getMessage());
//                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
//                });
    }

    @Operation(
            summary = "Update boat advertisement with images",
            description = "Updates an existing boat advertisement including specification details, " +
                    "adding new images, and/or removing existing images. All operations are performed " +
                    "in a single transaction to ensure data consistency."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Advertisement updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BoatAdResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or validation errors",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = "{\n  \"error\": \"VALIDATION_ERROR\",\n  \"message\": \"Invalid field value\",\n  \"timestamp\": \"2025-01-15T10:30:00Z\"\n}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid token or user not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - user does not own this advertisement",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Advertisement not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "413",
                    description = "Payload too large - file size exceeds 5MB limit",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PutMapping(value = "/{adId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<BoatAdResponse>> updateBoatAdWithImages(
            @PathVariable Long adId,
            @RequestPart("adData") BoatAdRequest request,
            @RequestPart(value = "newImages", required = false) Flux<FilePart> newImages,
            @RequestHeader("Authorization") String authHeader) {

        log.info("=== UPDATE AD REQUEST === AdID: {}, User: {}, Category: {}, ImagesToDelete: {} ===",
                adId, request.getUserEmail(), request.getCategory(), request.getImagesToDelete());

        String token = authHeader.replace("Bearer ", "");

        return marketplaceService.updateBoatAdWithImages(adId, request, newImages, request.getImagesToDelete(), token)
                .map(response -> ResponseEntity.ok(response))
                .onErrorResume(AdNotFoundException.class, e -> {
                    log.warn("=== AD NOT FOUND === AdID: {} ===", adId);
                    return Mono.just(ResponseEntity.notFound().build());
                })
                .onErrorResume(UserNotFoundException.class, e -> {
                    log.warn("=== USER NOT FOUND === User: {} ===", request.getUserEmail());
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                })
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("=== VALIDATION ERROR === AdID: {}, Error: {} ===", adId, e.getMessage());
                    return Mono.just(ResponseEntity.badRequest().build());
                })
                .onErrorResume(InvalidFieldValueException.class, e -> {
                    log.warn("=== INVALID FIELD VALUE === AdID: {}, Field: {}, Error: {} ===",
                            adId, e.getFieldName(), e.getMessage());
                    return Mono.just(ResponseEntity.badRequest().build());
                })
                .onErrorResume(MandatoryFieldMissingException.class, e -> {
                    log.warn("=== MISSING MANDATORY FIELD === AdID: {}, Field: {} ===",
                            adId, e.getMessage());
                    return Mono.just(ResponseEntity.badRequest().build());
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("=== UNEXPECTED ERROR === AdID: {}, Error: {} ===", adId, e.getMessage(), e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

//    @Operation(
//            summary = "Update boat advertisement details only",
//            description = "Updates an existing boat advertisement details and specifications without " +
//                    "modifying images. Use this endpoint when you only need to update text fields, " +
//                    "prices, specifications, etc."
//    )
//    @ApiResponses(value = {
//            @ApiResponse(
//                    responseCode = "200",
//                    description = "Advertisement details updated successfully",
//                    content = @Content(
//                            mediaType = "application/json",
//                            schema = @Schema(implementation = BoatAdResponse.class)
//                    )
//            ),
//            @ApiResponse(
//                    responseCode = "400",
//                    description = "Invalid request data or validation errors",
//                    content = @Content(
//                            mediaType = "application/json",
//                            schema = @Schema(implementation = ErrorResponse.class)
//                    )
//            ),
//            @ApiResponse(
//                    responseCode = "401",
//                    description = "Unauthorized - invalid token or user not found",
//                    content = @Content(
//                            mediaType = "application/json",
//                            schema = @Schema(implementation = ErrorResponse.class)
//                    )
//            ),
//            @ApiResponse(
//                    responseCode = "403",
//                    description = "Forbidden - user does not own this advertisement",
//                    content = @Content(
//                            mediaType = "application/json",
//                            schema = @Schema(implementation = ErrorResponse.class)
//                    )
//            ),
//            @ApiResponse(
//                    responseCode = "404",
//                    description = "Advertisement not found",
//                    content = @Content(
//                            mediaType = "application/json",
//                            schema = @Schema(implementation = ErrorResponse.class)
//                    )
//            ),
//            @ApiResponse(
//                    responseCode = "500",
//                    description = "Internal server error",
//                    content = @Content(
//                            mediaType = "application/json",
//                            schema = @Schema(implementation = ErrorResponse.class)
//                    )
//            )
//    })
//    @PutMapping(value = "/{adId}/details", consumes = MediaType.APPLICATION_JSON_VALUE)
//    public Mono<ResponseEntity<BoatAdResponse>> updateBoatAdDetails(
//            @Parameter(
//                    description = "Advertisement ID to update",
//                    required = true,
//                    example = "123"
//            )
//            @PathVariable Long adId,
//
//            @Parameter(
//                    description = "Advertisement data containing all details and specifications to update",
//                    required = true
//            )
//            @RequestBody @Valid BoatAdRequest request,
//
//            @Parameter(
//                    description = "Bearer token for authentication",
//                    required = true,
//                    example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
//            )
//            @RequestHeader("Authorization") String authHeader) {
//
//        log.info("=== UPDATE AD DETAILS === AdID: {}, User: {} ===", adId, request.getUserEmail());
//
//        String token = authHeader.replace("Bearer ", "");
//
//        return marketplaceService.updateBoatAd(adId, request, token)
//                .map(ResponseEntity::ok)
//                .onErrorResume(AdNotFoundException.class, e -> {
//                    log.warn("=== AD NOT FOUND === AdID: {} ===", adId);
//                    return Mono.just(ResponseEntity.notFound().build());
//                })
//                .onErrorResume(UserNotFoundException.class, e -> {
//                    log.warn("=== USER NOT FOUND === User: {} ===", request.getUserEmail());
//                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
//                })
//                .onErrorResume(IllegalArgumentException.class, e -> {
//                    log.warn("=== VALIDATION ERROR === AdID: {}, Error: {} ===", adId, e.getMessage());
//                    return Mono.just(ResponseEntity.badRequest().build());
//                })
//                .onErrorResume(InvalidFieldValueException.class, e -> {
//                    log.warn("=== INVALID FIELD VALUE === AdID: {}, Field: {}, Error: {} ===",
//                            adId, e.getFieldName(), e.getMessage());
//                    return Mono.just(ResponseEntity.badRequest().build());
//                })
//                .onErrorResume(MandatoryFieldMissingException.class, e -> {
//                    log.warn("=== MISSING MANDATORY FIELD === AdID: {}, Field: {} ===",
//                            adId, e.getMessage());
//                    return Mono.just(ResponseEntity.badRequest().build());
//                })
//                .onErrorResume(Exception.class, e -> {
//                    log.error("=== UNEXPECTED ERROR === AdID: {}, Error: {} ===", adId, e.getMessage(), e);
//                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
//                });
//    }

    // ===========================
    // SEARCH ENDPOINTS
    // ===========================

    @PostMapping("/search")
    @Operation(
            summary = "Advanced search for advertisements",
            description = "Performs advanced search across all advertisement categories with filtering, sorting, and pagination support"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Search completed successfully (may return empty results)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BoatAdResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid search criteria",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public Flux<BoatAdResponse> searchAds(
            @Parameter(description = "Search criteria with filters and sorting options", required = true)
            @Valid @RequestBody BoatSearchRequest searchRequest) {

        long startTime = System.currentTimeMillis();
        log.info("=== SEARCH ADS REQUEST === Category: {}, Location: {}, PriceRange: {}-{}, Brand: {} ===",
                searchRequest.getCategory(), searchRequest.getLocation(),
                searchRequest.getMinPrice(), searchRequest.getMaxPrice(), searchRequest.getBrand());

        return searchService.searchAds(searchRequest)
                .doOnComplete(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("=== SEARCH COMPLETED === Category: {}, Duration: {}ms ===",
                            searchRequest.getCategory(), duration);
                })
                .doOnError(error -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("=== SEARCH ERROR === Category: {}, Duration: {}ms, Error: {} ===",
                            searchRequest.getCategory(), duration, error.getMessage());
                });
//                .onErrorResume(InvalidSearchCriteriaException.class, e -> {
//                    log.error("=== INVALID SEARCH CRITERIA === {}", e.getMessage());
//                    return Flux.empty();
//                });
    }

    @GetMapping("/category/{category}")
    @Operation(
            summary = "Get advertisements by category",
            description = "Retrieves all active advertisements for a specific category with optional limit"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Advertisements retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BoatAdResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid category or limit parameter"
            )
    })
    public Flux<BoatAdResponse> getAdsByCategory(
            @Parameter(description = "Advertisement category", required = true, example = "BOATS_AND_YACHTS")
            @PathVariable @NotNull MainBoatCategory category,
            @Parameter(description = "Maximum number of results to return", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {

        long startTime = System.currentTimeMillis();
        log.info("=== GET ADS BY CATEGORY === Category: {}, Limit: {} ===", category, limit);

        return searchService.searchByCategory(category)
                .take(limit)
                .doOnComplete(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("=== CATEGORY SEARCH COMPLETED === Category: {}, Limit: {}, Duration: {}ms ===",
                            category, limit, duration);
                });
    }

    @GetMapping("/location")
    @Operation(
            summary = "Search advertisements by location",
            description = "Retrieves advertisements filtered by location with fuzzy matching"
    )
    public Flux<BoatAdResponse> getAdsByLocation(
            @Parameter(description = "Location search term", required = true, example = "София")
            @RequestParam @NotNull String location,
            @Parameter(description = "Maximum number of results", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {

        long startTime = System.currentTimeMillis();
        log.info("=== GET ADS BY LOCATION === Location: '{}', Limit: {} ===", location, limit);

        return searchService.searchByLocation(location)
                .take(limit)
                .doOnComplete(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("=== LOCATION SEARCH COMPLETED === Location: '{}', Duration: {}ms ===",
                            location, duration);
                });
    }

    // ===========================
    // FEATURED & TRENDING ENDPOINTS
    // ===========================

    @GetMapping("/featured")
    @Operation(
            summary = "Get featured advertisements",
            description = "Retrieves all currently featured advertisements across all categories"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Featured advertisements retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BoatAdResponse.class))
    )
    public Flux<BoatAdResponse> getFeaturedAds() {
        long startTime = System.currentTimeMillis();
        log.info("=== GET FEATURED ADS REQUEST ===");

        return searchService.getFeaturedAds()
                .doOnComplete(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("=== FEATURED ADS COMPLETED === Duration: {}ms ===", duration);
                });
    }

    @GetMapping("/recent")
    @Operation(
            summary = "Get most recent advertisements",
            description = "Retrieves the most recently created advertisements"
    )
    public Flux<BoatAdResponse> getRecentAds(
            @Parameter(description = "Number of recent ads to return", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {

        long startTime = System.currentTimeMillis();
        log.info("=== GET RECENT ADS === Limit: {} ===", limit);

        return searchService.getRecentAds(limit)
                .doOnComplete(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("=== RECENT ADS COMPLETED === Limit: {}, Duration: {}ms ===", limit, duration);
                });
    }

    @GetMapping("/popular")
    @Operation(
            summary = "Get most viewed advertisements",
            description = "Retrieves advertisements sorted by view count in descending order"
    )
    public Flux<BoatAdResponse> getMostViewedAds(
            @Parameter(description = "Number of popular ads to return", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {

        long startTime = System.currentTimeMillis();
        log.info("=== GET POPULAR ADS === Limit: {} ===", limit);

        return searchService.getMostViewedAds(limit)
                .doOnComplete(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("=== POPULAR ADS COMPLETED === Limit: {}, Duration: {}ms ===", limit, duration);
                });
    }

    // ===========================
    // STATISTICS ENDPOINT
    // ===========================

    @GetMapping("/stats")
    @Operation(
            summary = "Get marketplace statistics",
            description = "Retrieves comprehensive statistics about the marketplace including ad counts, price information, etc."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Statistics retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BoatMarketplaceStatsResponse.class))
    )
    public Mono<ResponseEntity<BoatMarketplaceStatsResponse>> getMarketplaceStats() {
        long startTime = System.currentTimeMillis();
        log.info("=== GET MARKETPLACE STATS REQUEST ===");

        return marketplaceService.getMarketplaceStats()
                .map(stats -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("=== MARKETPLACE STATS SUCCESS === TotalAds: {}, ActiveAds: {}, AvgPrice: {}, Duration: {}ms ===",
                            stats.getTotalAds(), stats.getActiveAds(), stats.getAveragePrice(), duration);
                    return ResponseEntity.ok(stats);
                });
//                .onErrorResume(error -> {
//                    long duration = System.currentTimeMillis() - startTime;
//                    log.error("=== MARKETPLACE STATS ERROR === Duration: {}ms, Error: {} ===",
//                            duration, error.getMessage());
//                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
//                });
    }

    // ===========================
    // CATEGORY-SPECIFIC SEARCH ENDPOINTS
    // ===========================

//    @PostMapping("/boats/search")
//    @Operation(
//            summary = "Search boats and yachts",
//            description = "Search specifically in the boats and yachts category with boat-specific filters"
//    )
//    @Tag(name = "Category-Specific Search")
//    public Flux<BoatAdResponse> searchBoats(
//            @Parameter(description = "Boat-specific search criteria")
//            @Valid @RequestBody BoatSearchRequest searchRequest) {
//
//        searchRequest.setCategory(BoatCategory.BOATS_AND_YACHTS);
//        log.info("=== SEARCH BOATS === Brand: {}, Model: {}, YearRange: {}-{} ===",
//                searchRequest.getBrand(), searchRequest.getModel(),
//                searchRequest.getMinYear(), searchRequest.getMaxYear());
//
//        return searchService.searchAds(searchRequest);
//    }
//
//    @PostMapping("/jetskis/search")
//    @Operation(
//            summary = "Search jet skis",
//            description = "Search specifically in the jet skis category"
//    )
//    @Tag(name = "Category-Specific Search")
//    public Flux<BoatAdResponse> searchJetSkis(@Valid @RequestBody BoatSearchRequest searchRequest) {
//        searchRequest.setCategory(BoatCategory.JET_SKIS);
//        log.info("=== SEARCH JET SKIS === Brand: {}, Model: {} ===",
//                searchRequest.getBrand(), searchRequest.getModel());
//        return searchService.searchAds(searchRequest);
//    }
//
//    @PostMapping("/trailers/search")
//    @Operation(
//            summary = "Search trailers",
//            description = "Search specifically in the trailers category"
//    )
//    @Tag(name = "Category-Specific Search")
//    public Flux<BoatAdResponse> searchTrailers(@Valid @RequestBody BoatSearchRequest searchRequest) {
//        searchRequest.setCategory(BoatCategory.TRAILERS);
//        log.info("=== SEARCH TRAILERS === Brand: {} ===", searchRequest.getBrand());
//        return searchService.searchAds(searchRequest);
//    }
//
//    @PostMapping("/engines/search")
//    @Operation(
//            summary = "Search engines",
//            description = "Search specifically in the engines category"
//    )
//    @Tag(name = "Category-Specific Search")
//    public Flux<BoatAdResponse> searchEngines(@Valid @RequestBody BoatSearchRequest searchRequest) {
//        searchRequest.setCategory(BoatCategory.ENGINES);
//        log.info("=== SEARCH ENGINES === Brand: {}, YearRange: {}-{} ===",
//                searchRequest.getBrand(), searchRequest.getMinYear(), searchRequest.getMaxYear());
//        return searchService.searchAds(searchRequest);
//    }
//
//    @PostMapping("/electronics/search")
//    @Operation(
//            summary = "Search marine electronics",
//            description = "Search marine electronics including sonars, probes, and trolling motors"
//    )
//    @Tag(name = "Category-Specific Search")
//    public Flux<BoatAdResponse> searchMarineElectronics(@Valid @RequestBody BoatSearchRequest searchRequest) {
//        searchRequest.setCategory(BoatCategory.MARINE_ELECTRONICS);
//        log.info("=== SEARCH MARINE ELECTRONICS === Type: {}, Brand: {}, ScreenSize: {} ===",
//                searchRequest.getElectronicsType(), searchRequest.getBrand(), searchRequest.getScreenSize());
//        return searchService.searchAds(searchRequest);
//    }
//
//    @PostMapping("/fishing/search")
//    @Operation(
//            summary = "Search fishing equipment",
//            description = "Search fishing rods, reels, lures, and other fishing equipment"
//    )
//    @Tag(name = "Category-Specific Search")
//    public Flux<BoatAdResponse> searchFishing(@Valid @RequestBody BoatSearchRequest searchRequest) {
//        searchRequest.setCategory(BoatCategory.FISHING);
//        log.info("=== SEARCH FISHING === Type: {}, Technique: {}, TargetFish: {} ===",
//                searchRequest.getFishingType(), searchRequest.getFishingTechnique(), searchRequest.getTargetFish());
//        return searchService.searchAds(searchRequest);
//    }
//
//    @PostMapping("/parts/search")
//    @Operation(
//            summary = "Search boat parts",
//            description = "Search for boat parts, propellers, impellers, and accessories"
//    )
//    @Tag(name = "Category-Specific Search")
//    public Flux<BoatAdResponse> searchParts(@Valid @RequestBody BoatSearchRequest searchRequest) {
//        searchRequest.setCategory(BoatCategory.PARTS);
//        log.info("=== SEARCH PARTS === Type: {} ===", searchRequest.getPartType());
//        return searchService.searchAds(searchRequest);
//    }
//
//    @PostMapping("/services/search")
//    @Operation(
//            summary = "Search boat services",
//            description = "Search for boat repair services, engine services, and marine service providers"
//    )
//    @Tag(name = "Category-Specific Search")
//    public Flux<BoatAdResponse> searchServices(@Valid @RequestBody BoatSearchRequest searchRequest) {
//        searchRequest.setCategory(BoatCategory.SERVICES);
//        log.info("=== SEARCH SERVICES === Type: {}, AuthorizedService: {}, SupportedBrand: {} ===",
//                searchRequest.getServiceType(), searchRequest.getAuthorizedService(), searchRequest.getSupportedBrand());
//        return searchService.searchAds(searchRequest);
//    }

    // ===========================
    // UTILITY METHODS
    // ===========================

    private String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            log.debug("Extracted JWT token (length: {})", token.length());
            return token;
        }
        log.error("Invalid authorization header format: {}",
                authHeader != null ? authHeader.substring(0, Math.min(20, authHeader.length())) + "..." : "null");
        throw new IllegalArgumentException("Invalid authorization header format");
    }

    private Mono<ResponseEntity<BoatAdResponse>> handleCreateAdError(Throwable error) {
        log.error("=== AD CREATION ERROR HANDLER === Error Type: {}, Message: {} ===",
                error.getClass().getSimpleName(), error.getMessage());

        if (error instanceof UserNotFoundException) {
            log.warn("User not found during ad creation: {}", error.getMessage());
            return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("Error-Type", "USER_NOT_FOUND")
                    .build());
        }

        if (error instanceof MandatoryFieldMissingException) {
            log.warn("Mandatory field missing: {}", error.getMessage());
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("Error-Type", "MANDATORY_FIELD_MISSING")
                    .header("Error-Message", error.getMessage())
                    .build());
        }

        if (error instanceof CategoryMismatchException) {
            log.warn("Category mismatch: {}", error.getMessage());
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("Error-Type", "CATEGORY_MISMATCH")
                    .header("Error-Message", error.getMessage())
                    .build());
        }

        if (error instanceof AuthServiceException) {
            log.error("Authentication service error: {}", error.getMessage());
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header("Error-Type", "AUTH_SERVICE_ERROR")
                    .header("Error-Message", error.getMessage())
                    .build());
        }

        log.error("Unhandled error during ad creation", error);
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("Error-Type", "INTERNAL_SERVER_ERROR")
                .build());
    }

    // ===========================
    // GLOBAL EXCEPTION HANDLERS
    // ===========================

//    @ExceptionHandler(InvalidSearchCriteriaException.class)
//    public ResponseEntity<ErrorResponse> handleInvalidSearchCriteria(InvalidSearchCriteriaException e) {
//        log.error("=== INVALID SEARCH CRITERIA EXCEPTION === Message: {} ===", e.getMessage());
//
//        ErrorResponse errorResponse = ErrorResponse.builder()
//                .error("INVALID_SEARCH_CRITERIA")
//                .message(e.getMessage())
//                .timestamp(java.time.LocalDateTime.now())
//                .build();
//
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
//    }
//
//    @ExceptionHandler(IllegalArgumentException.class)
//    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
//        log.error("=== ILLEGAL ARGUMENT EXCEPTION === Message: {} ===", e.getMessage());
//
//        ErrorResponse errorResponse = ErrorResponse.builder()
//                .error("ILLEGAL_ARGUMENT")
//                .message(e.getMessage())
//                .timestamp(java.time.LocalDateTime.now())
//                .build();
//
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
//    }
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ErrorResponse> handleGenericError(Exception e) {
//        log.error("=== UNHANDLED EXCEPTION === Type: {}, Message: {} ===",
//                e.getClass().getSimpleName(), e.getMessage(), e);
//
//        ErrorResponse errorResponse = ErrorResponse.builder()
//                .error("INTERNAL_SERVER_ERROR")
//                .message("An unexpected error occurred")
//                .timestamp(java.time.LocalDateTime.now())
//                .build();
//
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
//    }
}