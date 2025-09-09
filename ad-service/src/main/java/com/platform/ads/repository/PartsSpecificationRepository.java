package com.platform.ads.repository;

import com.platform.ads.entity.PartsSpecification;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface PartsSpecificationRepository extends ReactiveCrudRepository<PartsSpecification, Long> {
    Mono<PartsSpecification> findByAdId(Long adId);

    @Modifying
    Mono<Void> deleteByAdId(Long adId);
}