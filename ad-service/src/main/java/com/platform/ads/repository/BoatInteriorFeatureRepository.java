package com.platform.ads.repository;

import com.platform.ads.entity.BoatInteriorFeature;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface BoatInteriorFeatureRepository extends ReactiveCrudRepository<BoatInteriorFeature, Long> {
    Flux<BoatInteriorFeature> findByBoatSpecId(Long boatSpecId);

    @Modifying
    Mono<Void> deleteByBoatSpecId(Long boatSpecId);
}
