package com.github.d.romanov.spotify.importer.model;

import java.util.List;

public record ImportResult(
        boolean isImportSuccessful,
        List<Track> notFoundTracks
) {}
