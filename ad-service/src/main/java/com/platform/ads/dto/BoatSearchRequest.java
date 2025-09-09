package com.platform.ads.dto;

import com.platform.ads.dto.enums.AdType;
import com.platform.ads.dto.enums.MainBoatCategory;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BoatSearchRequest {
    // Common search criteria
    private MainBoatCategory category;
    private String location;
    private PriceInfo.PriceType priceType;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private AdType adType;
    private String sortBy;

    // Category-specific search criteria can be added here
    // For example:
    private String brand;
    private String model;
    private Integer minYear;
    private Integer maxYear;
    private String condition;

    // Marine Electronics specific
    private String electronicsType;
    private String screenSize;
    private Boolean gpsIntegrated;

    // Fishing specific
    private String fishingType;
    private String fishingTechnique;
    private String targetFish;

    // Parts specific
    private String partType;

    // Services specific
    private String serviceType;
    private Boolean authorizedService;
    private String supportedBrand;

    // ADD NEW FIELDS FOR WATER SPORTS
    private String waterSportsType;

    // ADD NEW FIELDS FOR MARINE ACCESSORIES
    private String accessoryType;

    // ADD NEW FIELDS FOR RENTALS
    private String rentalType;
    private Boolean licenseRequired;
    private String managementType;
    private String serviceTypeRentals; // To differentiate from services serviceType
    private String companyName;
    private Integer numberOfPeople;

    // Additional common filters that might be useful
    private Boolean inWarranty;
    private Boolean isRegistered;
    private String engineType;
    private Integer minHorsepower;
    private Integer maxHorsepower;
}