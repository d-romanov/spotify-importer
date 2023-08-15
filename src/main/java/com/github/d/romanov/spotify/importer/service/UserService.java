package com.github.d.romanov.spotify.importer.service;

import com.github.d.romanov.spotify.importer.client.SpotifyClient;
import com.github.d.romanov.spotify.importer.model.dto.Playlist;
import com.github.d.romanov.spotify.importer.model.dto.PlaylistsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final SpotifyClient spotifyClient;

    public Mono<List<Playlist>> getUserPlaylists(OAuth2User principal) {
        return spotifyClient.getUserPlaylists()
                .map(PlaylistsResponse::items)
                .map(playlists -> playlists.stream()
                        .filter(playlist -> playlist.owner().id().equals(principal.getAttribute("id")))
                        .collect(Collectors.toList()));
    }
}
