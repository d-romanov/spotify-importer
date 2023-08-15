package com.github.d.romanov.spotify.importer.client;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Spotify API has (<a href="https://developer.spotify.com/documentation/web-api/concepts/rate-limits">rate limit</a>),
 * but they don't specify it explicitly. So if we receive TooManyRequests response, we should limit our request rate.
 * It is implemented via {@link RateLimiter}, which allows dynamically adjust rate (decrease in our case).
 * <p>
 * poolCountdown is used to prevent dramatic rate decrease, because requests are made in parallel,
 * and all requests can receive TooManyRequests response.
 * <p>
 * {@link CircuitBreaker} is used to prevent any requests towards Spotify API during "Retry-After" wait period.
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class SpotifyClientFilter implements ExchangeFilterFunction {

    private final RateLimiter spotifyRateLimiter;
    private final CircuitBreaker spotifyCircuitBreaker;
    private final AtomicInteger poolCountdown = new AtomicInteger(Schedulers.DEFAULT_POOL_SIZE);

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        return next.exchange(request)
                .flatMap(clientResponse -> Mono.just(clientResponse)
                        .filter(response -> response.statusCode().isError())
                        .flatMap(ClientResponse::createException)
                        .flatMap(Mono::error)
                        .thenReturn(clientResponse))
                .transformDeferred(RateLimiterOperator.of(spotifyRateLimiter))
                .transformDeferred(CircuitBreakerOperator.of(spotifyCircuitBreaker))
                .retryWhen(Retry.indefinitely()
                        .filter(throwable -> throwable instanceof WebClientResponseException.TooManyRequests)
                        .doBeforeRetryAsync(retrySignal -> {
                            spotifyCircuitBreaker.transitionToForcedOpenState();
                            log.debug("Circuit breaker is {}", spotifyCircuitBreaker.getState());
                            var clientException = (WebClientResponseException.TooManyRequests) retrySignal.failure();
                            long timeout = Optional.of(clientException.getHeaders())
                                    .map(httpHeaders -> httpHeaders.get("Retry-After"))
                                    .orElse(List.of())
                                    .stream()
                                    .findFirst()
                                    .map(retryAfterSec -> {
                                        log.debug("429 Too Many Requests encountered, retrying {} after {} seconds",
                                                Optional.ofNullable(clientException.getRequest())
                                                        .map(HttpRequest::getURI)
                                                        .map(URI::toString)
                                                        .orElse("unknown request"),
                                                retryAfterSec);
                                        return Long.parseLong(retryAfterSec);
                                    })
                                    .orElse(2L);

                            if (poolCountdown.decrementAndGet() <= 0) {
                                spotifyRateLimiter.changeLimitForPeriod(
                                        Math.max(1, spotifyRateLimiter.getRateLimiterConfig().getLimitForPeriod() * 9 / 10));
                                spotifyRateLimiter.changeTimeoutDuration(Duration.ofSeconds(timeout));
                                poolCountdown.set(Schedulers.DEFAULT_POOL_SIZE);
                            }

                            log.debug("Current rate limit: {} requests per {}",
                                    spotifyRateLimiter.getRateLimiterConfig().getLimitForPeriod(),
                                    spotifyRateLimiter.getRateLimiterConfig().getLimitRefreshPeriod());

                            return Mono.delay(Duration.ofSeconds(timeout))
                                    .doOnNext(aLong -> {
                                        spotifyCircuitBreaker.transitionToClosedState();
                                        log.debug("Circuit breaker is {}", spotifyCircuitBreaker.getState());
                                    })
                                    .then();
                        }))
                // Retry indefinitely all errors, except 4xx client errors other than 429.
                // This includes circuit breaker's CallNotPermittedException
                // and rate limiter's RequestNotPermitted exception
                .retryWhen(Retry.indefinitely()
                        .filter(throwable -> throwable instanceof WebClientResponseException.TooManyRequests
                                || !(throwable instanceof WebClientResponseException exception
                                && exception.getStatusCode().is4xxClientError()))
                        .doBeforeRetry(retrySignal -> {
                            if (retrySignal.failure() instanceof WebClientResponseException exception
                                    && exception.getStatusCode().is5xxServerError()) {
                                log.error(exception.toString());
                            }
                        }));
    }
}
