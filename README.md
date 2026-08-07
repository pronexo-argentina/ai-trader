# AI Trader

![Java](https://img.shields.io/badge/Java-21-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-23-blue)
![Python](https://img.shields.io/badge/Python-3.11%2B-blue)
![FastAPI](https://img.shields.io/badge/FastAPI-Backend-009688)
![License](https://img.shields.io/badge/License-GPLv3-blue)
![Status](https://img.shields.io/badge/status-early%20development-yellow)

AI Trader is an open-source multi-market platform for market analysis,
strategy backtesting, risk evaluation and future machine-learning experimentation.

The project combines a JavaFX desktop interface with a Python analytics engine
connected to public cryptocurrency and stock-market data.

> AI Trader is currently an educational and research project.  
> It does not provide financial advice or guarantee future results.

## Current Features

### Markets

**Cryptocurrencies**
- BTC/USDT
- ETH/USDT
- Binance
- Kraken
- Public market data through CCXT

**Stocks / ETFs**
- Apple (`AAPL`)
- ASML (`ASML`)
- YPF ADR (`YPF`)
- SPY
- QQQ
- Historical market data through Yahoo Finance / yfinance

### Analysis and Backtesting

- EMA 12 / EMA 26
- RSI 14
- ATR 14
- Rule-based technical signals
- Historical periods: 1 month, 3 months, 6 months and 1 year
- 1h, 4h and 1d timeframes
- Transaction fees
- Slippage simulation
- Position sizing
- Stop-loss
- Take-profit
- Maximum drawdown
- Win rate
- Profit factor
- Sharpe ratio
- Average winning trade
- Average losing trade
- Buy & Hold benchmark
- Real dates in price and equity charts

## Important: AI Status

The current version does **not yet use Machine Learning to generate trading signals**.

Current signals are based on explicit technical-analysis rules.

Machine Learning will be introduced after the market-data pipeline, backtesting,
risk management and validation infrastructure are mature enough.

## Architecture

```text
┌──────────────────────────────┐
│       JavaFX Desktop         │
│                              │
│ Dashboard                    │
│ Charts                       │
│ Metrics                      │
│ Technical Signals            │
└──────────────┬───────────────┘
               │ HTTP / JSON
               ▼
┌──────────────────────────────┐
│       FastAPI Backend        │
│                              │
│ Indicators                   │
│ Backtesting                  │
│ Risk Metrics                 │
└──────────────┬───────────────┘
               │
          ┌────┴─────┐
          ▼          ▼
       CCXT       yfinance
          │          │
          ▼          ▼
      Crypto     Stocks / ETFs
```

## Requirements

### Backend
- Python 3.11+
- pip

### Desktop
- Java 21+
- Maven
- JavaFX

## Running the Backend

```bash
cd backend
python3 -m venv .venv
source .venv/bin/activate
python3 -m pip install -r requirements.txt
uvicorn src.api:app --reload --port 7000
```

Health check:

```bash
curl http://127.0.0.1:7000/health
```

FastAPI docs:

```text
http://127.0.0.1:7000/docs
```

## Running the Desktop

```bash
cd desktop
mvn clean javafx:run
```

## Initial Stock Test

Recommended first test:

```text
Market: Acciones / ETF
Source: yahoo
Asset: AAPL
Timeframe: 1h
Period: 3m
```

## YPF Note

`YPF` currently refers to the YPF ADR traded in the United States.

It is not yet the local BYMA instrument in Argentine pesos.

## Data-source Notes

Cryptocurrency data is obtained through public CCXT endpoints.

Stock/ETF historical data is obtained through Yahoo Finance using `yfinance`.

For intraday stock analysis, AI Trader downloads the requested history in smaller
date chunks and merges them. This avoids relying on a single large Yahoo Finance
intraday request, which can return incomplete data.
This is suitable for research and backtesting, but it should not be treated as
an official execution-grade real-time feed.

## Documentation

- [User Manual](docs/MANUAL.md)
- [Changelog](docs/CHANGELOG.md)

The manual is intentionally written for users with no previous trading experience.

## Roadmap

- Candlestick charts
- Multiple strategies
- Momentum
- Mean reversion
- Breakout
- Strategy comparison
- Out-of-sample validation
- Walk-forward validation
- Paper trading
- Market-regime detection
- Machine Learning
- Gradient boosting
- LSTM / Transformer experimentation
- News analysis
- Sentiment analysis
- Explainable signals
- Portfolio tracking
- Argentine local market data

## Development Principles

1. Real market data before predictive AI.
2. Risk management before automation.
3. Avoid look-ahead bias.
4. Include fees and slippage.
5. Compare against simple benchmarks.
6. Prefer explainable signals.
7. Validate out of sample.
8. Paper trade before considering real capital.
9. Never present backtest performance as guaranteed future performance.

## Disclaimer

AI Trader is provided for educational, research and software-development purposes.

Nothing in this project constitutes financial, investment or trading advice.

Financial markets involve risk and historical performance does not guarantee future results.

## License

AI Trader is licensed under the **GNU General Public License v3.0 (GPL-3.0)**.


## Yahoo intraday diagnostics

During early development, stock intraday downloads log the number of rows
returned for each Yahoo Finance date chunk and the total merged rows.

This diagnostic output is temporary and helps detect incomplete intraday
responses before relying on those results for backtesting.


### Stock data normalization

For Yahoo Finance stock data, missing intraday volume does not invalidate an
otherwise complete OHLC candle. AI Trader keeps candles with valid timestamp,
open, high, low and close values, and treats missing volume as zero.


### Timestamp normalization

Market timestamps are normalized explicitly to Unix milliseconds rather than
assuming pandas uses nanosecond resolution internally. This keeps stock
intraday candles unique across pandas/yfinance versions and prevents historical
bars from collapsing into duplicate timestamps.
