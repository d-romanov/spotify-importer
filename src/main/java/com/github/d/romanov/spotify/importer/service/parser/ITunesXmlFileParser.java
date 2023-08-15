package com.github.d.romanov.spotify.importer.service.parser;

import com.github.d.romanov.spotify.importer.model.Track;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

@Slf4j
@RequiredArgsConstructor
@Service
public class ITunesXmlFileParser implements FileParser {

    public static final String VALUE_PLACEHOLDER = "dummy";

    @Override
    public boolean isApplicable(FilePart file) {
        if (MediaType.TEXT_XML.equals(file.headers().getContentType())) {
            return file.content()
                    .map(dataBuffer -> {
                        try (var inputStream = dataBuffer.asInputStream();
                             var streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                             var bufferedReader = new BufferedReader(streamReader)) {

                            return bufferedReader.lines()
                                    .anyMatch(line -> line.contains("www.apple.com"));
                        } catch (IOException e) {
                            log.error("Failed to read from file " + FilenameUtils.getBaseName(file.filename()), e);
                            return false;
                        } finally {
                            DataBufferUtils.release(dataBuffer);
                        }
                    })
                    .reduce((b1, b2) -> b1 || b2)
                    .blockOptional()
                    .orElse(false);
        }
        return false;
    }

    @Override
    public Flux<Track> parseTracks(FilePart file) {
        Deque<Track> tracks = new ArrayDeque<>();

        return DataBufferUtils.join(file.content())
                .flatMapMany(dataBuffer -> {
                    try (var inputStream = dataBuffer.asInputStream()) {
                        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
                        XMLEventReader reader = xmlInputFactory.createXMLEventReader(inputStream);

                        boolean startImport = false;
                        while (reader.hasNext()) {
                            XMLEvent nextEvent = reader.nextEvent();

                            if (nextEvent.isStartElement()) {
                                StartElement startElement = nextEvent.asStartElement();
                                switch (startElement.getName().getLocalPart()) {
                                    case "dict" -> {
                                        if (startImport) {
                                            tracks.add(new Track());
                                        }
                                    }
                                    case "key" -> {
                                        nextEvent = reader.nextEvent();
                                        switch (nextEvent.asCharacters().getData()) {
                                            case "Tracks" -> startImport = true;
                                            case "Playlists" -> startImport = false;
                                            case "Name" -> {
                                                if (startImport) {
                                                    tracks.getLast().setTrackName(VALUE_PLACEHOLDER);
                                                }
                                            }
                                            case "Artist" -> {
                                                if (startImport) {
                                                    tracks.getLast().setArtistName(VALUE_PLACEHOLDER);
                                                }
                                            }
                                        }
                                    }
                                    case "string" -> {
                                        nextEvent = reader.nextEvent();
                                        if (startImport) {
                                            if (VALUE_PLACEHOLDER.equals(tracks.getLast().getTrackName())) {
                                                tracks.getLast().setTrackName(nextEvent.asCharacters().getData());
                                            } else if (VALUE_PLACEHOLDER.equals(tracks.getLast().getArtistName())) {
                                                tracks.getLast().setArtistName(nextEvent.asCharacters().getData());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (IOException | XMLStreamException e) {
                        return Flux.error(e);
                    } finally {
                        DataBufferUtils.release(dataBuffer);
                    }

                    if (tracks.getFirst().getTrackName() == null) {
                        tracks.removeFirst();
                    }

                    return Flux.fromIterable(tracks);
                });
    }
}
