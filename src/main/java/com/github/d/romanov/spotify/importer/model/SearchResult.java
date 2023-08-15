package com.github.d.romanov.spotify.importer.model;

import java.util.List;

public record SearchResult(
        List<String> trackIds,
        List<Track> notFoundTracks
) {}
