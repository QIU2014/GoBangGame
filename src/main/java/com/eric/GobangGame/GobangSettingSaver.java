package com.eric.GobangGame;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Logger;

public class GobangSettingSaver {
    private static final Logger gobangSettingSaverLogger = Logger.getLogger(GobangSettingSaver.class);

    public static void saveSettings(Locale locale) {
        AppSettings settings = new AppSettings();
        settings.setLocale(new LocaleSettings(locale.getLanguage(), locale.getCountry()));

        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File("settings.json"), settings);
            gobangSettingSaverLogger.info("Settings saved: " + locale);
        } catch (IOException e) {
            gobangSettingSaverLogger.error("Unexpected exception occurred! ", e);
        }
    }

    @Getter
    @Setter
    static class AppSettings {
        private LocaleSettings locale;

        public AppSettings() {}
    }

    @Getter
    @Setter
    static class LocaleSettings {
        private String language;
        private String country;

        public LocaleSettings() {}

        // Helper constructor
        public LocaleSettings(String language, String country) {
            this.language = language;
            this.country = country;
        }
    }
}

// Separate class for old format
@Getter
@Setter
class OldFormatSettings {
    private String lang;

    public OldFormatSettings() {}
}