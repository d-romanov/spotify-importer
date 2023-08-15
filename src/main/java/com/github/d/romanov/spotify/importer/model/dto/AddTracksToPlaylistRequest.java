package com.github.d.romanov.spotify.importer.model.dto;

import java.util.List;

public record AddTracksToPlaylistRequest(
        List<String> uris
) {}
