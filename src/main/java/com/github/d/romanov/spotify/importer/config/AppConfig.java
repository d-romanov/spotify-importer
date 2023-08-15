package com.github.d.romanov.spotify.importer.config;

import com.github.d.romanov.spotify.importer.client.SpotifyClientFilter;
import com.github.d.romanov.spotify.importer.config.props.ParserProps;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.csrf.XorServerCsrfTokenRequestAttributeHandler;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties({ParserProps.class})
@EnableWebFluxSecurity
public class AppConfig {

    @Bean
    public WebClient spotifyWebClient(ReactiveOAuth2AuthorizedClientManager authorizedClientManager,
            SpotifyClientFilter spotifyFilter) {

        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2Client =
                new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        oauth2Client.setDefaultClientRegistrationId("spotify");
        return WebClient.builder()
                .baseUrl("https://api.spotify.com")
                .filter(spotifyFilter)
                .filter(oauth2Client)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(
            ReactiveClientRegistrationRepository clientRegistrationRepository,
            ServerOAuth2AuthorizedClientRepository authorizedClientRepository) {

        var authorizedClientProvider = ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode()
                .refreshToken()
                .build();

        var authorizedClientManager = new DefaultReactiveOAuth2AuthorizedClientManager(clientRegistrationRepository,
                authorizedClientRepository);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }


    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity serverHttpSecurity) {
        var tokenRequestAttributeHandler = new XorServerCsrfTokenRequestAttributeHandler();
        tokenRequestAttributeHandler.setTokenFromMultipartDataEnabled(true);
        serverHttpSecurity
                .authorizeExchange(authorize -> authorize.anyExchange().authenticated())
                .oauth2Login(Customizer.withDefaults())
                .csrf(csrf -> csrf.csrfTokenRequestHandler(tokenRequestAttributeHandler));
        return serverHttpSecurity.build();
    }

    @Bean
    public RateLimiter spotifyRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMillis(100))
                .limitForPeriod(100)
                .timeoutDuration(Duration.ofSeconds(5))
                .build();

        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.of(config);
        return rateLimiterRegistry.rateLimiter("spotifyRateLimiter");
    }

    @Bean
    public CircuitBreaker circuitBreaker() {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        return circuitBreakerRegistry.circuitBreaker("spotifyCircuitBreaker");
    }

}
