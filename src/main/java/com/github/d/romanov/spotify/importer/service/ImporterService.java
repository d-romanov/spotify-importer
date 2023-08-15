package com.github.d.romanov.spotify.importer.service;

import com.github.d.romanov.spotify.importer.client.SpotifyClient;
import com.github.d.romanov.spotify.importer.model.ImportResult;
import com.github.d.romanov.spotify.importer.model.Track;
import com.github.d.romanov.spotify.importer.model.dto.AddTracksToPlaylistRequest;
import com.github.d.romanov.spotify.importer.model.dto.CreatePlaylistRequest;
import com.github.d.romanov.spotify.importer.model.dto.Playlist;
import com.github.d.romanov.spotify.importer.model.dto.UploadRequest;
import com.github.d.romanov.spotify.importer.service.parser.ParserService;
import com.github.d.romanov.spotify.importer.utils.ListUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.util.StringUtils.hasText;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImporterService {

    private final SpotifyClient spotifyClient;
    private final ParserService parserService;


    /**
     * @param upload Uploaded form data with file and import parameters
     * @return {@link ImportResult} of import status (true if all import requests succeeded), and list of tracks not found in Spotify
     */
    public Mono<ImportResult> importTracks(UploadRequest upload) {
        List<Track> notFoundTracks = new ArrayList<>();
        return parserService.parseTracks(upload.file())
                .publishOn(Schedulers.parallel())
                .flatMap(track -> spotifyClient.searchTrackId(track)
                                .onErrorResume(throwable -> {
                                    log.debug("Caught error: {}", throwable.toString());
                                    return Mono.just(track);
                                }),
                        //process tracks one by one in DEFAULT_POOL_SIZE parallel threads
                        //to limit Spotify API calls to avoid 429 Too Many Requests
                        Schedulers.DEFAULT_POOL_SIZE, 1)
                .filter(track -> {
                    if (track.getId() == null) {
                        log.debug("{} not found", track);
                        notFoundTracks.add(track);
                        return false;
                    }
                    return true;
                })
                .map(Track::getId)
                .collectList()
                .flatMap(trackIds -> {
                    Mono<Boolean> isImportSuccessful = switch (upload.importType()) {
                        case LIKED_SONGS -> importToLikedTracks(trackIds);
                        case PLAYLIST -> addTracksToPlaylist(trackIds, upload);
                    };
                    return isImportSuccessful
                            .map(isImpSuccess -> new ImportResult(isImpSuccess, notFoundTracks));
                });
    }

    private Mono<Boolean> importToLikedTracks(List<String> trackIds) {
        return Flux.fromIterable(ListUtils.splitList(trackIds, 50))
                .flatMap(spotifyClient::addToLikedTracks)
                .reduce((b1, b2) -> b1 && b2);
    }

    private Mono<Boolean> addTracksToPlaylist(List<String> trackIds, UploadRequest upload) {
        return Mono.just(upload.playlistId())
                .filter(playlistId -> !"newPlaylist".equals(playlistId))
                .switchIfEmpty(createPlaylist(upload))
                .flatMapMany(playlistId -> {
                    List<String> uris = trackIds.stream()
                            .map(id -> "spotify:track:" + id)
                            .collect(Collectors.toList());
                    return Flux.fromIterable(ListUtils.splitList(uris, 100))
                            .map(AddTracksToPlaylistRequest::new)
                            .flatMap(request -> spotifyClient.addTracksToPlaylist(request, playlistId));
                })
                .reduce((b1, b2) -> b1 && b2);
    }

    private Mono<String> createPlaylist(UploadRequest upload) {
        String name = hasText(upload.newPlaylistName()) ? upload.newPlaylistName()
                : FilenameUtils.getBaseName(upload.file().filename());

        return spotifyClient.createPlaylist(new CreatePlaylistRequest(name, false))
                .map(Playlist::id);
    }

}
