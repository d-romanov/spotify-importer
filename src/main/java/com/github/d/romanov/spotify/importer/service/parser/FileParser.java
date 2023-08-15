package com.github.d.romanov.spotify.importer.service.parser;

import com.github.d.romanov.spotify.importer.model.Track;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;

import java.util.Collection;

public interface FileParser {

    boolean isApplicable(FilePart file);

    Flux<Track> parseTracks(FilePart file);
}
