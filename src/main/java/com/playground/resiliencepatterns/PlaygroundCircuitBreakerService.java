package com.playground.resiliencepatterns;

import com.playground.resiliencepatterns.PlaygroundResource.BusinessException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Call;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class PlaygroundCircuitBreakerService {

    public Mono<String> getWelcomeTextReactive(Boolean fail) {
        final CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("backendA");

        return Mono.just("Welcome my hobbits")
                .map(value -> {
                    if (fail) {
                        throw new BusinessException();
                    }
                    return value;
                })
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(this::getSequenceReactiveFallback);
    }

    private Mono<String> getSequenceReactiveFallback(Throwable throwable) {
        if (throwable instanceof CallNotPermittedException) {
            return Mono.just("Unfortunately you cannot be welcomed caused by circuit breaker.\n");
        }
        return Mono.error(throwable);
    }

    @Retry
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "backendA", fallbackMethod = "getWelcomeTextFallback") - OPEN
    public Mono<String> getWelcomeText(Boolean fail) {
        return Mono.just("Welcome my hobbits")
                .map(value -> {
                    if (fail) {
                        throw new RuntimeException();
                    }
                    return value;
                });
    }

    private Mono<String> getWelcomeTextFallback(Boolean fail, BusinessException businessException) {
        log.info("Business Exception was thrown : {}", businessException.toString());
        throw new PlaygroundResource.TryAgainException();
    }

    private Mono<String> getWelcomeTextFallback(Boolean fail, RuntimeException businessException) {
        log.info("Runtime Exception was thrown : {}", businessException.toString());
        return Mono.just("Unfortunately you cannot be welcomed\n");
    }

    private Mono<String> getWelcomeTextFallback(Boolean fail, CallNotPermittedException businessException) {
        log.info("Runtime Exception was thrown : {}", businessException.toString());
        return Mono.just("Unfortunately you cannot be welcomed caused bt call not permitted\n");
    }

}
