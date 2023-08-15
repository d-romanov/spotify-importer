package com.github.d.romanov.spotify.importer.controller;

import com.github.d.romanov.spotify.importer.model.ImportResult;
import com.github.d.romanov.spotify.importer.model.dto.UploadRequest;
import com.github.d.romanov.spotify.importer.service.ImporterService;
import com.github.d.romanov.spotify.importer.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class AppController {

    public static final String INDEX = "index";
    public static final String INDEX_PATH = "/";
    public static final String UPLOAD_PATH = "/upload";

    private final ImporterService importerService;
    private final UserService userService;

    @GetMapping(INDEX_PATH)
    public Mono<Rendering> homepage(@AuthenticationPrincipal OAuth2User principal) {

        String userName = Optional.ofNullable(principal.getAttribute("display_name"))
                .map(String.class::cast)
                .orElse("undefined");

        return userService.getUserPlaylists(principal)
                .map(playlists -> Rendering.view("index")
                        .modelAttribute("userPlaylists", playlists)
                        .modelAttribute("principal", userName)
                        .build());
    }

    /**
     * Have to use ServerWebExchange.getMultipartData() because of CsrfWebFilter.
     * It invokes getMultipartData() to get csrf token from multipart/form-data. According to
     * {@link <a href="https://docs.spring.io/spring-framework/reference/6.0.11/web/webflux/reactive-spring.html#webflux-codecs-multipart">Multipart reference</a>}
     * "Once getMultipartData() is used, the original raw content can no longer be read from the request body.
     * For this reason applications have to consistently use getMultipartData() for repeated, map-like access to parts"
     **/

    @PostMapping(value = UPLOAD_PATH, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public Mono<Rendering> uploadFile(ServerWebExchange exchange) {
        return exchange.getMultipartData().map(UploadRequest::fromMultiValueMap)
                .flatMap(importerService::importTracks)
                .filter(ImportResult::isImportSuccessful)
                .map(importResult -> {
                    ModelMap modelMap = new ModelMap();
                    if (!importResult.notFoundTracks().isEmpty()) {
                        modelMap.put("notFoundTracks", importResult.notFoundTracks());
                    }
                    return modelMap;
                })
                .doOnNext(modelMap -> modelMap.put("message", "Import completed successfully"))
                .defaultIfEmpty(new ModelMap("error", "Import failed, check logs"))
                .map(modelMap -> Rendering.view(INDEX)
                        .model(modelMap)
                        .build())
                .onErrorResume(throwable -> Mono.just(Rendering.view(INDEX)
                        .modelAttribute("error", throwable.toString())
                        .build()));
    }

}
