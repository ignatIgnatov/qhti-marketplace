package com.platform.ads.repository;

import com.platform.ads.entity.BoatConsoleType;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface BoatConsoleTypeRepository extends ReactiveCrudRepository<BoatConsoleType, Long> {

    Flux<BoatConsoleType> findByBoatSpecId(Long boatSpecId);

    Mono<Void> deleteByBoatSpecId(Long boatSpecId);
}