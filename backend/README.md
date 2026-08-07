# AI Trader Backend

## Ejecutar

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn src.api:app --reload --port 7000
```

## Mercados

### Criptomonedas
Datos públicos mediante CCXT:
- Binance
- Kraken

### Acciones / ETF
Datos históricos mediante Yahoo Finance a través de `yfinance`.

Símbolos iniciales:
- AAPL
- ASML
- YPF
- SPY
- QQQ

La fuente bursátil se usa para investigación/backtesting, no como feed oficial
de ejecución.
