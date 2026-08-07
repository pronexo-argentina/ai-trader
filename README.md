# AI Trader

![Java](https://img.shields.io/badge/Java-21-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-23-blue)
![Python](https://img.shields.io/badge/Python-3.11%2B-blue)
![FastAPI](https://img.shields.io/badge/FastAPI-Backend-009688)
![License](https://img.shields.io/badge/License-GPLv3-blue)
![Status](https://img.shields.io/badge/status-early%20development-yellow)

AI Trader is an open-source platform for market analysis, strategy backtesting,
risk evaluation and future machine-learning experimentation.

The project combines a modern JavaFX desktop interface with a Python analytics
engine connected to real public market data.

> AI Trader is currently an educational and research project.  
> It does not provide financial advice or guarantee future results.

---

## Dashboard

![AI Trader Dashboard](docs/images/dashboard.png)

---

## Current Features

- Real cryptocurrency market data
- BTC/USDT and ETH/USDT support
- Binance and Kraken public market data
- EMA 12 / EMA 26 analysis
- RSI 14
- ATR 14
- Rule-based technical signals
- Historical backtesting
- Transaction fees
- Slippage simulation
- Basic position sizing
- Stop-loss
- Take-profit
- Maximum drawdown
- Win rate
- Profit factor
- Buy & Hold benchmark
- JavaFX desktop dashboard
- Python analytics backend
- FastAPI REST API
- CCXT market integration

---

## Important: AI Status

The current version does **not yet use Machine Learning to generate trading
signals**.

Current signals are based on explicit technical-analysis rules.

Machine Learning will be introduced only after the data pipeline, backtesting,
risk management and validation infrastructure are sufficiently mature.

This distinction is intentional: AI Trader should never present a simple rule
as artificial intelligence.

---

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
│ Backtesting                  │
│ Indicators                   │
│ Risk Management              │
└──────────────┬───────────────┘
               │
               ▼
┌──────────────────────────────┐
│        Python Engine         │
│ pandas / NumPy / CCXT        │
└──────────────┬───────────────┘
               │
               ▼
        Public Market Data
```

---

## Project Structure

```text
ai_trader/
├── backend/
│   ├── src/
│   ├── requirements.txt
│   └── README.md
│
├── desktop/
│   ├── src/
│   ├── pom.xml
│   └── README.md
│
├── docs/
│   ├── images/
│   ├── MANUAL.md
│   └── CHANGELOG.md
│
├── .gitignore
├── LICENSE
└── README.md
```

---

## Requirements

### Backend

- Python 3.11+
- pip

### Desktop

- Java 21+
- Maven
- JavaFX

---

## Running the Backend

```bash
cd backend
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn src.api:app --reload --port 7000
```

Check the backend:

```bash
curl http://127.0.0.1:7000/health
```

Expected response:

```json
{"status":"ok"}
```

FastAPI documentation is available at:

```text
http://127.0.0.1:7000/docs
```

---

## Running the Desktop Application

Open another terminal:

```bash
cd desktop
mvn clean javafx:run
```

The desktop application connects to:

```text
http://127.0.0.1:7000
```

---

## Current Analysis Flow

```text
Market Data
    ↓
Technical Indicators
    ↓
Rule-Based Signal
    ↓
Backtesting
    ↓
Risk Metrics
    ↓
Buy & Hold Comparison
    ↓
JavaFX Dashboard
```

---

## Roadmap

Planned development includes:

- Historical date-range selection
- Longer backtesting periods
- Candlestick charts
- Multiple trading strategies
- Momentum strategies
- Mean reversion
- Breakout strategies
- Strategy comparison
- Sharpe ratio and additional risk metrics
- Walk-forward validation
- Out-of-sample testing
- Paper trading
- Market-regime detection
- Machine Learning models
- Gradient boosting
- LSTM / Transformer experimentation
- News analysis
- Sentiment analysis
- Explainable signals
- Portfolio tracking

---

## Documentation

The project includes beginner-oriented documentation because no previous
trading knowledge should be required to understand the application.

See:

- [User Manual](docs/MANUAL.md)
- [Changelog](docs/CHANGELOG.md)

Concepts such as candles, timeframe, RSI, ATR, drawdown, stop-loss, slippage
and Buy & Hold are documented as they are introduced into the project.

---

## Development Principles

AI Trader is being built around several principles:

1. Real market data before predictive AI.
2. Risk management before automation.
3. Avoid look-ahead bias.
4. Include fees and slippage in backtests.
5. Compare strategies against simple benchmarks.
6. Prefer explainable signals.
7. Validate strategies out of sample.
8. Use paper trading before considering real capital.
9. Never present backtest performance as guaranteed future performance.

---

## Disclaimer

AI Trader is provided for educational, research and software-development
purposes.

Nothing in this project constitutes financial, investment or trading advice.

Financial markets involve risk and historical performance does not guarantee
future results.

---

## License

AI Trader is licensed under the
**GNU General Public License v3.0 (GPL-3.0)**.

See the [LICENSE](LICENSE) file for details.
