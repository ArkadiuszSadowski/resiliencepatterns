package com.playground.resiliencepatterns;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@RestController
@RequestMapping("/api/v1/playground")
@RequiredArgsConstructor
public class PlaygroundResource {

    private final PlaygroundCircuitBreakerService circuitBreakerService;
    private final PlaygroundBulkHeadService bulkHeadService;

    @GetMapping("/cb-1")
    public Mono<String> circuitBreakerTest1(@RequestParam(defaultValue = "false") Boolean fail) {
        return circuitBreakerService.getWelcomeTextReactive(fail);
    }

    @GetMapping("/cb-2")
    public Mono<String> circuitBreakerTest2(@RequestParam(defaultValue = "false") Boolean fail) {
        return circuitBreakerService.getWelcomeText(fail);
    }

    @Retry
    @CircuitBreaker(name = "backendB") -- OPEN
    @GetMapping("/cb-3")
    public Mono<List<Integer>> circuitBreakerTest3() {
        return Flux.fromStream(IntStream.range(1, 10).boxed())
                .delayElements(Duration.ofMillis(1000))
                .map(value -> {
                    if (value == 5) {
                        //throw new BusinessException();
                    }
                    return value * value;
                })
                .collectList()
                .onErrorContinue(error -> error instanceof BusinessException, (error, value) -> log.error("error occurred for value = {}", value));
    }

    @Bulkhead(name = "default")
    @PostMapping("/bh-1")
    public String bulkheadTest1() throws InterruptedException {
        Thread.sleep(4000L);
        return "OK";
    }

    @PostMapping("/bh-2")
    public Mono<String> bulkheadTest2(@RequestParam String name) {
        return bulkHeadService.welcomeMe(name);
    }

    @Retry(name = "default")
    @PostMapping("/retry")
    public void retryTest() {
        log.info("Another call");
        throw new BusinessException();
    }

    @Retry(name = "default")
    @PostMapping("/rl")
    public void rateLimiter() throws InterruptedException {
        Thread.sleep(4000L);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    static class TryAgainException extends RuntimeException {
        public TryAgainException() {
            super("Try again within 60 seconds");
        }
    }

    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    static class BusinessException extends RuntimeException {
        public BusinessException() {
            super("Business exception");
        }
    }

}