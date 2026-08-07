import ccxt
import pandas as pd

SUPPORTED_EXCHANGES = {
    "binance": ccxt.binance,
    "kraken": ccxt.kraken,
}

def fetch_ohlcv(exchange_id: str, symbol: str, timeframe: str, limit: int) -> pd.DataFrame:
    exchange_id = exchange_id.lower().strip()
    if exchange_id not in SUPPORTED_EXCHANGES:
        raise ValueError(f"Exchange no soportado: {exchange_id}")
    exchange = SUPPORTED_EXCHANGES[exchange_id]({"enableRateLimit": True, "timeout": 15000})
    exchange.load_markets()
    if symbol not in exchange.markets:
        raise ValueError(f"{symbol} no existe en {exchange_id}")
    if not exchange.has.get("fetchOHLCV"):
        raise ValueError(f"{exchange_id} no soporta OHLCV mediante CCXT")
    rows = exchange.fetch_ohlcv(symbol, timeframe=timeframe, limit=limit)
    if not rows:
        raise RuntimeError("El exchange no devolvió velas")
    df = pd.DataFrame(rows, columns=["timestamp","open","high","low","close","volume"])
    df["timestamp"] = df["timestamp"].astype("int64")
    for col in ["open","high","low","close","volume"]:
        df[col] = df[col].astype(float)
    return df.dropna().reset_index(drop=True)
