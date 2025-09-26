package com.platform.ads.repository;

import com.platform.ads.entity.BoatPurpose;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface BoatPurposeRepository extends ReactiveCrudRepository<BoatPurpose, Long> {

    Flux<BoatPurpose> findByBoatSpecId(Long boatSpecId);

    Mono<Void> deleteByBoatSpecId(Long boatSpecId);
}