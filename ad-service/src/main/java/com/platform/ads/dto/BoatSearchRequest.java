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

    // Common fields for multiple categories
    private String brand;
    private String model;
    private Integer minYear;
    private Integer maxYear;
    private String condition;

    // BOAT-SPECIFIC FIELDS
    private BoatSpecificationDto.BoatType boatType;
    private BigDecimal minLength;
    private BigDecimal maxLength;
    private BigDecimal minWidth;
    private BigDecimal maxWidth;
    private Integer minHorsepower;
    private Integer maxHorsepower;
    private BoatSpecificationDto.EngineType engineType;
    private Boolean engineIncluded;
    private BoatSpecificationDto.FuelType fuelType;
    private BoatSpecificationDto.MaterialType material;
    private Boolean isRegistered;
    private Boolean inWarranty;

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

    // Water Sports specific
    private String waterSportsType;

    // Marine Accessories specific
    private String accessoryType;

    // Rentals specific
    private String rentalType;
    private Boolean licenseRequired;
    private String managementType;
    private String serviceTypeRentals;
    private String companyName;
    private Integer numberOfPeople;
}