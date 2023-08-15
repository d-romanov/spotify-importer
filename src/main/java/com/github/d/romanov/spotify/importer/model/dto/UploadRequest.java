package com.github.d.romanov.spotify.importer.model.dto;

import com.github.d.romanov.spotify.importer.model.ImportType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.util.MultiValueMap;

import java.util.Optional;

public record UploadRequest(
        FilePart file,
        ImportType importType,
        String playlistId,
        String newPlaylistName
) {
    public static UploadRequest fromMultiValueMap(MultiValueMap<String, Part> map) {
        return new UploadRequest(
                (FilePart) map.getFirst("file"),
                Optional.ofNullable(map.getFirst("importType"))
                        .map(FormFieldPart.class::cast)
                        .map(FormFieldPart::value)
                        .map(ImportType::valueOf)
                        .orElseThrow(() -> new IllegalStateException("Import type is not specified")),
                Optional.ofNullable(map.getFirst("playlistId"))
                        .map(FormFieldPart.class::cast)
                        .map(FormFieldPart::value)
                        .orElse(null),
                Optional.ofNullable(map.getFirst("newPlaylistName"))
                        .map(FormFieldPart.class::cast)
                        .map(FormFieldPart::value)
                        .orElse(null)
        );
    }
}
