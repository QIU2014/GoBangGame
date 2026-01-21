package com.eric.GobangGame;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Logger;

public class GobangSettingLoader {
    private static final Logger gobangSettingLoaderLogger = Logger.getLogger(GobangSettingLoader.class);

    public static AppSettings loadSettings() throws IOException {
        File settings = new File("settings.json");
        if (!settings.exists()) {
            return null;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonContent = new String(java.nio.file.Files.readAllBytes(settings.toPath()));

        // Try to parse as new format first
        try {
            return objectMapper.readValue(jsonContent, AppSettings.class);
        } catch (Exception e) {
            // If new format fails, try old format
            gobangSettingLoaderLogger.warn("New format parsing failed, trying old format...");

            // Check if it's the old format with just "lang" field
            if (jsonContent.contains("\"lang\"")) {
                try {
                    // Parse old format
                    OldFormatSettings oldSettings = objectMapper.readValue(jsonContent, OldFormatSettings.class);
                    AppSettings settingsModel = new AppSettings();

                    // Convert old format to new format
                    if (oldSettings.getLang() != null) {
                        String langStr = oldSettings.getLang();
                        // Parse "en_US" format
                        String[] parts = langStr.split("_");
                        if (parts.length >= 1) {
                            LocaleSettings locale = new LocaleSettings();
                            locale.setLanguage(parts[0]);
                            if (parts.length >= 2) {
                                locale.setCountry(parts[1]);
                            }
                            settingsModel.setLocale(locale);
                        }
                    }
                    return settingsModel;
                } catch (Exception ex) {
                    gobangSettingLoaderLogger.error("Failed to parse old format", ex);
                    throw new IOException("Failed to parse settings file in any format");
                }
            }
            throw e; // Re-throw original exception
        }
    }

    public static Locale getLocaleFromSettings(AppSettings settings) {
        if (settings != null && settings.getLocale() != null) {
            LocaleSettings localeSettings = settings.getLocale();
            if (localeSettings.getLanguage() != null && !localeSettings.getLanguage().isEmpty()) {
                String country = localeSettings.getCountry();
                if (country != null && !country.isEmpty()) {
                    return Locale.of(localeSettings.getLanguage(), country);
                } else {
                    return Locale.of(localeSettings.getLanguage());
                }
            }
        }
        return null;
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
    }
}