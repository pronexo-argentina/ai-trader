package com.iatradex.market;

import com.iatradex.model.AssetSearchResult;
import com.iatradex.model.Candle;
import com.iatradex.model.MarketQuote;
import com.iatradex.util.HttpJsonClient;

import java.util.List;

public final class MarketService {

    private final BinanceMarketDataProvider binance;
    private final KrakenMarketDataProvider kraken;
    private final YahooMarketDataProvider yahoo;
    private final YahooSearchService yahooSearch;
    private final OpenBymaDataProvider argentina;

    public MarketService() {
        HttpJsonClient http = new HttpJsonClient();

        this.binance = new BinanceMarketDataProvider(http);
        this.kraken = new KrakenMarketDataProvider(http);
        this.yahoo = new YahooMarketDataProvider(http);
        this.yahooSearch = new YahooSearchService(http);
        this.argentina = new OpenBymaDataProvider(http);
    }

    public List<Candle> fetch(
            String marketType,
            String source,
            String symbol,
            String timeframe,
            String period
    ) throws Exception {

        if ("crypto".equals(marketType)) {
            return switch (source.toLowerCase()) {
                case "binance" -> binance.fetch(symbol, timeframe, period);
                case "kraken" -> kraken.fetch(symbol, timeframe, period);
                default -> throw new IllegalArgumentException(
                        "Exchange no soportado: " + source
                );
            };
        }

        if ("argentina".equals(marketType)) {
            return argentina.fetch(symbol, timeframe, period);
        }

        if ("stocks".equals(marketType)) {
            return yahoo.fetch(symbol, timeframe, period);
        }

        throw new IllegalArgumentException(
                "Mercado no soportado: " + marketType
        );
    }

    public List<AssetSearchResult> searchStocks(
            String query,
            boolean argentinaMarket
    ) throws Exception {

        if (argentinaMarket) {
            return argentina.search(query, 8);
        }

        return yahooSearch.search(query, 8, false);
    }

    public MarketQuote quote(
            String marketType,
            String symbol
    ) throws Exception {

        if ("argentina".equals(marketType)) {
            return argentina.quote(symbol);
        }

        return null;
    }

    public double livePrice(
            String marketType,
            String source,
            String symbol
    ) throws Exception {

        if ("crypto".equals(marketType)) {
            return switch (
                    source == null
                            ? "binance"
                            : source.toLowerCase()
            ) {
                case "kraken" -> kraken.livePrice(symbol);
                default -> binance.livePrice(symbol);
            };
        }

        if ("argentina".equals(marketType)) {
            MarketQuote quote = argentina.quote(symbol);

            if (quote == null
                    || quote.lastPrice() == null
                    || !Double.isFinite(quote.lastPrice())
                    || quote.lastPrice() <= 0.0) {
                throw new IllegalStateException(
                        "Open BYMADATA no devolvió precio para " + symbol
                );
            }

            return quote.lastPrice();
        }

        if ("stocks".equals(marketType)) {
            return yahoo.livePrice(symbol);
        }

        throw new IllegalArgumentException(
                "Mercado no soportado: " + marketType
        );
    }

}
