package com.github.d.romanov.spotify.importer.service.parser;

import com.github.d.romanov.spotify.importer.config.props.ParserProps;
import com.github.d.romanov.spotify.importer.model.Track;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Parser for file txt file, one track per line in format: {@code trackName ${delimiter} artistName}
 */

@Slf4j
@RequiredArgsConstructor
@Service
public class TxtFileParser implements FileParser {

    private final ParserProps parserProps;

    @Override
    public boolean isApplicable(FilePart file) {
        return MediaType.TEXT_PLAIN.equals(file.headers().getContentType());
    }

    @Override
    public Flux<Track> parseTracks(FilePart file) {
        return DataBufferUtils.join(file.content())
                .flatMapMany(dataBuffer -> {
                    try (var inputStream = dataBuffer.asInputStream(true);
                         var streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                         var bufferedReader = new BufferedReader(streamReader)) {

                        return Flux.fromIterable(bufferedReader.lines()
                                .map(line -> line.split(parserProps.getTxt().getDelimiter()))
                                .map(details -> Track.builder()
                                        .trackName(details[0].trim())
                                        .artistName(details.length > 1 ? details[1].trim() : "")
                                        .build())
                                .collect(Collectors.toList()));
                    } catch (IOException e) {
                        return Flux.error(e);
                    }
                });
    }
}
