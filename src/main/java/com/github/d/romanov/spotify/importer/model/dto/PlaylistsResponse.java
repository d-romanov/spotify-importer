package com.github.d.romanov.spotify.importer.model.dto;

import java.util.List;

public record PlaylistsResponse(
        List<Playlist> items
) {}
