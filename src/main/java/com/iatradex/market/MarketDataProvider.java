package com.iatradex.market;

import com.iatradex.model.Candle;

import java.util.List;

public interface MarketDataProvider {
    List<Candle> fetch(String symbol, String timeframe, String period) throws Exception;
}
