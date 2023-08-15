package com.github.d.romanov.spotify.importer.service.parser;

import com.github.d.romanov.spotify.importer.model.SearchResult;
import com.github.d.romanov.spotify.importer.model.Track;
import com.github.d.romanov.spotify.importer.utils.TrackUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParserService {

    private final List<FileParser> parsers;

    /**
     * Parses the playlist file with corresponding parser.
     * Throws {@link IllegalStateException}, if there are no applicable parsers for supplied file.
     *
     * @param file Playlist file
     * @return {@link SearchResult} containing list of track ids found in Spotify, and list of not found tracks
     */
    public Flux<Track> parseTracks(FilePart file) {
        log.debug("Tracks parsing started");
        FileParser parser = parsers.stream()
                .filter(p -> p.isApplicable(file))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("No parsers found for media type " + file.headers().getContentType()));

        return parser.parseTracks(file)
                .transformDeferred(trackFlux -> {
                    log.debug("Tracks parsing complete");
                    return trackFlux;
                })
                .switchIfEmpty(Mono.error(new IllegalStateException("No tracks were found in supplied playlist")))
                .doOnNext(TrackUtils::fixFeatTags);
    }
}
