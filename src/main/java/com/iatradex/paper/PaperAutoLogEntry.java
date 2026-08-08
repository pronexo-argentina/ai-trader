package com.iatradex.paper;

public record PaperAutoLogEntry(
        String timestamp,
        String level,
        String message
) {}
