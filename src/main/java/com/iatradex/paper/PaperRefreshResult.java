package com.iatradex.paper;

import java.util.List;

public record PaperRefreshResult(
        int updatedPositions,
        int closedPositions,
        List<String> messages,
        String updatedAt
) {}
