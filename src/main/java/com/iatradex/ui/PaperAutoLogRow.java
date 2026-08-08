package com.iatradex.ui;

import com.iatradex.paper.PaperAutoLogEntry;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class PaperAutoLogRow {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("dd/MM HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    private final StringProperty time;
    private final StringProperty level;
    private final StringProperty message;

    public PaperAutoLogRow(PaperAutoLogEntry entry) {
        String formatted;

        try {
            formatted = FORMAT.format(
                    Instant.parse(entry.timestamp())
            );
        } catch (Exception ignored) {
            formatted = entry.timestamp();
        }

        time = new SimpleStringProperty(formatted);
        level = new SimpleStringProperty(entry.level());
        message = new SimpleStringProperty(entry.message());
    }

    public StringProperty timeProperty() { return time; }
    public StringProperty levelProperty() { return level; }
    public StringProperty messageProperty() { return message; }
}
