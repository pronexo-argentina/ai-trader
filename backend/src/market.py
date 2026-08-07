import time
from typing import Literal

import ccxt
import pandas as pd
import yfinance as yf


CRYPTO_EXCHANGES = {
    "binance": ccxt.binance,
    "kraken": ccxt.kraken,
}

TIMEFRAME_MS = {
    "1h": 60 * 60 * 1000,
    "4h": 4 * 60 * 60 * 1000,
    "1d": 24 * 60 * 60 * 1000,
}

PERIOD_DAYS = {
    "1m": 30,
    "3m": 90,
    "6m": 180,
    "1y": 365,
}

YF_PERIODS = {
    "1m": "1mo",
    "3m": "3mo",
    "6m": "6mo",
    "1y": "1y",
}


def fetch_market_data(
    market_type: str,
    source: str,
    symbol: str,
    timeframe: str,
    period: str,
) -> pd.DataFrame:
    if market_type == "crypto":
        return fetch_crypto_ohlcv(source, symbol, timeframe, period)

    if market_type == "stocks":
        return fetch_stock_ohlcv(symbol, timeframe, period)

    raise ValueError(f"Mercado no soportado: {market_type}")


def fetch_crypto_ohlcv(
    exchange_id: str,
    symbol: str,
    timeframe: str,
    period: str,
) -> pd.DataFrame:
    exchange_id = exchange_id.lower().strip()

    if exchange_id not in CRYPTO_EXCHANGES:
        raise ValueError(f"Exchange no soportado: {exchange_id}")

    if timeframe not in TIMEFRAME_MS:
        raise ValueError(f"Timeframe no soportado: {timeframe}")

    if period not in PERIOD_DAYS:
        raise ValueError(f"Período no soportado: {period}")

    exchange = CRYPTO_EXCHANGES[exchange_id]({
        "enableRateLimit": True,
        "timeout": 20_000,
    })

    exchange.load_markets()

    if symbol not in exchange.markets:
        raise ValueError(f"{symbol} no existe en {exchange_id}")

    if not exchange.has.get("fetchOHLCV"):
        raise ValueError(f"{exchange_id} no soporta OHLCV")

    now_ms = exchange.milliseconds()
    since = now_ms - PERIOD_DAYS[period] * 24 * 60 * 60 * 1000
    tf_ms = TIMEFRAME_MS[timeframe]

    rows = []
    seen = set()
    cursor = since

    for _ in range(100):
        batch = exchange.fetch_ohlcv(
            symbol,
            timeframe=timeframe,
            since=cursor,
            limit=1000,
        )

        if not batch:
            break

        for row in batch:
            ts = int(row[0])
            if ts not in seen:
                seen.add(ts)
                rows.append(row)

        last_ts = int(batch[-1][0])
        next_cursor = last_ts + tf_ms

        if next_cursor <= cursor:
            break

        cursor = next_cursor

        if cursor >= now_ms:
            break

        time.sleep(max(getattr(exchange, "rateLimit", 0), 0) / 1000.0)

    if not rows:
        raise RuntimeError("El exchange no devolvió velas")

    df = pd.DataFrame(
        rows,
        columns=["timestamp", "open", "high", "low", "close", "volume"],
    )

    df = df[df["timestamp"] >= since].copy()
    df = df.sort_values("timestamp").drop_duplicates("timestamp").reset_index(drop=True)

    return _normalize_dataframe(df)



def fetch_stock_ohlcv(
    symbol: str,
    timeframe: str,
    period: str,
) -> pd.DataFrame:
    from datetime import datetime, timedelta, timezone

    if timeframe not in ("1h", "4h", "1d"):
        raise ValueError(f"Timeframe no soportado: {timeframe}")

    if period not in PERIOD_DAYS:
        raise ValueError(f"Período no soportado: {period}")

    days = PERIOD_DAYS[period]
    now = datetime.now(timezone.utc)
    start = now - timedelta(days=days)

    # Para diario Yahoo suele funcionar bien en una sola consulta.
    if timeframe == "1d":
        df = yf.Ticker(symbol).history(
            start=start,
            end=now + timedelta(days=1),
            interval="1d",
            auto_adjust=True,
            prepost=False,
            actions=False,
        )
    else:
        # Yahoo puede ser inconsistente al pedir varios meses intradía de una vez.
        # Descargamos por bloques de hasta 30 días y concatenamos.
        chunks = []
        cursor = start

        while cursor < now:
            chunk_end = min(cursor + timedelta(days=30), now)

            chunk = yf.Ticker(symbol).history(
                start=cursor,
                end=chunk_end + timedelta(days=1),
                interval="1h",
                auto_adjust=True,
                prepost=False,
                actions=False,
            )

            print(
                "YAHOO CHUNK",
                symbol,
                cursor.isoformat(),
                "->",
                chunk_end.isoformat(),
                "filas:",
                0 if chunk is None else len(chunk),
            )

            if chunk is not None and not chunk.empty:
                chunks.append(chunk)

            cursor = chunk_end

        if not chunks:
            raise RuntimeError(
                f"No se obtuvieron datos bursátiles intradía para {symbol}"
            )

        df = pd.concat(chunks)
        print("YAHOO TOTAL CONCATENADO", symbol, "filas:", len(df))
        df = df[~df.index.duplicated(keep="first")].sort_index()
        print("YAHOO TOTAL SIN DUPLICADOS", symbol, "filas:", len(df))

    if df is None or df.empty:
        raise RuntimeError(f"No se obtuvieron datos bursátiles para {symbol}")

    df = df.reset_index()

    date_col = None
    for candidate in ("Datetime", "Date"):
        if candidate in df.columns:
            date_col = candidate
            break

    if date_col is None:
        # Algunas versiones pueden dejar el nombre de índice como index.
        if "index" in df.columns:
            date_col = "index"
        else:
            raise RuntimeError("No se encontró la columna de fecha de Yahoo Finance")

    # Convertimos explícitamente a milisegundos Unix.
    #
    # No usamos astype("int64") // 10**6 porque pandas puede conservar
    # distintas resoluciones internas (ns/us/ms/s) según la fuente/versión.
    # Eso puede colapsar cientos de velas en unos pocos timestamps repetidos.
    parsed_dates = pd.to_datetime(df[date_col], utc=True, errors="coerce")
    df["timestamp"] = parsed_dates.map(
        lambda value: int(value.timestamp() * 1000)
        if pd.notna(value)
        else np.nan
    )

    renamed = {
        "Open": "open",
        "High": "high",
        "Low": "low",
        "Close": "close",
        "Volume": "volume",
    }
    df = df.rename(columns=renamed)

    df = df[["timestamp", "open", "high", "low", "close", "volume"]].copy()
    print("ANTES NORMALIZE", symbol, "filas:", len(df), "timestamps únicos:", df["timestamp"].nunique())
    df = _normalize_dataframe(df)
    print("DESPUES NORMALIZE", symbol, "filas:", len(df), "timestamps únicos:", df["timestamp"].nunique())

    if timeframe == "4h":
        dt = pd.to_datetime(df["timestamp"], unit="ms", utc=True)
        tmp = df.copy()
        tmp.index = dt

        df = (
            tmp.resample("4h", origin="start_day")
            .agg({
                "open": "first",
                "high": "max",
                "low": "min",
                "close": "last",
                "volume": "sum",
            })
            .dropna()
            .reset_index()
        )

        df["timestamp"] = pd.to_datetime(
            df["index"], utc=True, errors="coerce"
        ).map(
            lambda value: int(value.timestamp() * 1000)
            if pd.notna(value)
            else np.nan
        )
        df = df[["timestamp", "open", "high", "low", "close", "volume"]]
        df = _normalize_dataframe(df)

    min_required = 50
    if len(df) < min_required:
        raise RuntimeError(
            f"Solo se obtuvieron {len(df)} velas para {symbol}. "
            f"Se requieren al menos {min_required}. "
            "Probá otro período o revisá la disponibilidad de Yahoo."
        )

    return df.reset_index(drop=True)

def _normalize_dataframe(df: pd.DataFrame) -> pd.DataFrame:
    out = df.copy()

    for col in ["timestamp", "open", "high", "low", "close", "volume"]:
        if col not in out.columns:
            raise RuntimeError(f"Falta columna de mercado: {col}")

    out["timestamp"] = pd.to_numeric(out["timestamp"], errors="coerce")

    for col in ["open", "high", "low", "close", "volume"]:
        out[col] = pd.to_numeric(out[col], errors="coerce")

    out = out.dropna(
        subset=["timestamp", "open", "high", "low", "close"]
    ).sort_values("timestamp").drop_duplicates("timestamp")

    # En Yahoo Finance el volumen intradía puede venir vacío en algunas velas.
    # No descartamos una vela válida solo por faltar volumen.
    out["volume"] = out["volume"].fillna(0)
    out["timestamp"] = out["timestamp"].astype("int64")

    return out.reset_index(drop=True)
