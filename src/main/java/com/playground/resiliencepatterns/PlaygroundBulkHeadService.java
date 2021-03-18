package com.playground.resiliencepatterns;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class PlaygroundBulkHeadService {

    @RateLimiter(name = "default", fallbackMethod = "bulkheadFallback")
    public Mono<String> welcomeMe(String name) {

        return Mono.just("Welcome " +  name + "\n ");
    }

    private Mono<String> bulkheadFallback(String name, Exception bulkheadFullException) {
        //log.error("BulkheadException : {}", bulkheadFullException.toString());
        return Mono.just("You are not welcomed " + name + "\n");
    }

}