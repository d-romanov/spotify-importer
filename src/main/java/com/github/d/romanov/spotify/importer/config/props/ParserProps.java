package com.github.d.romanov.spotify.importer.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "parser")
public class ParserProps {

    private Txt txt;

    @Data
    public static class Txt {
        private String delimiter;
    }
}
