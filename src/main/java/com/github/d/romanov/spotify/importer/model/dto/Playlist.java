package com.github.d.romanov.spotify.importer.model.dto;

public record Playlist(
        String id,
        String name,
        Owner owner
) {

    public record Owner(
            String id
    ) {}
}
