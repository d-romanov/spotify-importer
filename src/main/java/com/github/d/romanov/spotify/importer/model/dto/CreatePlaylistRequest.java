package com.github.d.romanov.spotify.importer.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreatePlaylistRequest(
        String name,
        @JsonProperty("public")
        boolean isPublic
) {}
