package com.github.d.romanov.spotify.importer.utils;

import com.github.d.romanov.spotify.importer.model.Track;
import lombok.experimental.UtilityClass;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class TrackUtils {

    private static final Pattern TRACK_NAME_PATTERN = Pattern.compile("(.+) \\(feat\\.");

    //Empirically, spotify search produces more accurate results when excluding featuring artists from track name
    public static void fixFeatTags(Track track) {
        String trackName = track.getTrackName();
        if (trackName != null && trackName.toLowerCase().contains("feat.")) {
            Matcher trackMatcher = TRACK_NAME_PATTERN.matcher(trackName);
            if (trackMatcher.find()) {
                track.setTrackName(trackMatcher.group(1));
            }
        }
    }
}
