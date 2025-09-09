package com.platform.ads.repository;

import com.platform.ads.entity.FishingSpecification;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface FishingSpecificationRepository extends ReactiveCrudRepository<FishingSpecification, Long> {
    Mono<FishingSpecification> findByAdId(Long adId);

    @Modifying
    Mono<Void> deleteByAdId(Long adId);
}