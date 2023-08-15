package com.github.d.romanov.spotify.importer.client;

import com.github.d.romanov.spotify.importer.model.Track;
import com.github.d.romanov.spotify.importer.model.dto.AddTracksToPlaylistRequest;
import com.github.d.romanov.spotify.importer.model.dto.CreatePlaylistRequest;
import com.github.d.romanov.spotify.importer.model.dto.Item;
import com.github.d.romanov.spotify.importer.model.dto.ItemsWrapper;
import com.github.d.romanov.spotify.importer.model.dto.Playlist;
import com.github.d.romanov.spotify.importer.model.dto.PlaylistsResponse;
import com.github.d.romanov.spotify.importer.model.dto.SearchResponse;
import com.github.d.romanov.spotify.importer.utils.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpotifyClient {

    @Qualifier("spotifyWebClient")
    private final WebClient webClient;

    public Mono<Track> searchTrackId(Track track) {
        log.debug("Searching for {}", track);
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/search")
                        .queryParam("q", track.getTrackName() + " " + track.getArtistName())
                        .queryParam("type", "track")
                        .queryParam("limit", "1")
                        .build())
                .retrieve()
                .bodyToMono(SearchResponse.class)
                .map(searchResponse -> {
                    Optional.ofNullable(searchResponse)
                            .map(SearchResponse::tracks)
                            .map(ItemsWrapper::items)
                            .filter(items -> !items.isEmpty())
                            .map(items -> items.get(0))
                            .map(Item::id)
                            .ifPresent(track::setId);
                    return track;
                });
    }

    public Mono<Boolean> addToLikedTracks(List<String> trackIds) {
        return webClient.put()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/me/tracks")
                        .queryParam("ids", String.join(",", trackIds))
                        .build())
                .retrieve()
                .toEntity(Void.class)
                .map(ResponseEntity::getStatusCode)
                .map(HttpStatusCode::is2xxSuccessful);
    }

    public Mono<PlaylistsResponse> getUserPlaylists() {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/me/playlists")
                        .queryParam("limit", 50)
                        .build())
                .retrieve()
                .bodyToMono(PlaylistsResponse.class);
    }

    public Mono<Playlist> createPlaylist(CreatePlaylistRequest request) {
        return TokenUtils.getPrincipal()
                .flatMap(oAuth2User -> webClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .path("/v1/users/" + oAuth2User.getAttribute("id") + "/playlists")
                                .build())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(Playlist.class));
    }

    public Mono<Boolean> addTracksToPlaylist(AddTracksToPlaylistRequest request, String playlistId) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/playlists/" + playlistId + "/tracks")
                        .queryParam("playlist_id", playlistId)
                        .queryParam("position", "0")
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .toEntity(Void.class)
                .map(ResponseEntity::getStatusCode)
                .map(HttpStatusCode::is2xxSuccessful);
    }
}
