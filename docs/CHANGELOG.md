# Changelog

## Fix: Yahoo intraday timestamp resolution

- Converts Yahoo/pandas datetimes explicitly to Unix milliseconds.
- Does not assume pandas internally stores datetimes in nanoseconds.
- Prevents hundreds of hourly candles from collapsing into a few duplicate timestamps.
- Added temporary before/after normalization diagnostics.
- Applied the same robust timestamp conversion to 4h resampling.



## Fix: Yahoo candles with missing volume

- Stock candles are no longer discarded when only `volume` is missing.
- OHLC and timestamp remain mandatory.
- Missing stock volume is normalized to zero.
- Fixes cases where hundreds of Yahoo candles were reduced to only a few rows.



## Diagnostic: Yahoo intraday chunks

- Logs row count for every Yahoo Finance intraday date chunk.
- Logs merged and de-duplicated row counts.
- Helps diagnose incomplete AAPL/ASML/YPF intraday history.
- README principal updated.


## Fix: stock intraday history

- Stock 1h/4h history is now downloaded in date chunks and merged.
- Avoids incomplete Yahoo Finance responses such as only a handful of AAPL candles.
- Keeps the selected timeframe instead of silently falling back to daily data.
- README principal updated.

## Fix: Yahoo Finance periods

- Corregido mapeo de períodos para Yahoo Finance:
  - 1m → 1mo
  - 3m → 3mo
  - 6m → 6mo
  - 1y → 1y
- Corrige el error de AAPL/ASML/YPF sin datos al usar períodos mensuales.
- README principal actualizado.
- Eliminado README_UPDATE.md.

## UI update

- Selectores superiores más anchos.
- Mejor contraste en los ComboBox.
- Popup de opciones con tema oscuro.
- Hover y selección más visibles.
- Mejor alineación vertical de etiquetas y selectores.

## 0.4.0

- AI Trader pasa a ser multi-mercado.
- Selector Mercado: Criptomonedas / Acciones-ETF.
- Acciones iniciales: AAPL, ASML, YPF, SPY y QQQ.
- Yahoo Finance mediante yfinance para datos bursátiles.
- CCXT se mantiene para cripto.
- Sharpe anualizado según tipo de mercado.
- Timeframe 4h bursátil generado a partir de velas horarias.
- Manual ampliado con diferencias entre acciones y criptomonedas.

## 0.3.0

- Períodos 1m, 3m, 6m, 1y.
- Profit Factor.
- Sharpe.
- Ganancia/pérdida media.
- Fechas reales.
