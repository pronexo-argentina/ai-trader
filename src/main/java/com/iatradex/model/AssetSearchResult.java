package com.iatradex.model;

public record AssetSearchResult(
        String symbol,
        String name,
        String exchange,
        String type,
        String logoUrl
) {}
