package com.github.d.romanov.spotify.importer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Track {

    private String id;
    private String trackName;
    private String artistName;

    @Override
    public String toString() {
        return artistName + " - " + trackName;
    }
}
