from typing import Literal

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from .backtest import run_backtest
from .indicators import add_indicators, technical_snapshot
from .market import fetch_market_data


class AnalysisRequest(BaseModel):
    market_type: Literal["crypto", "stocks"] = "crypto"
    source: str = "binance"
    symbol: str = "BTC/USDT"
    timeframe: Literal["1h", "4h", "1d"] = "1h"
    period: Literal["1m", "3m", "6m", "1y"] = "3m"

    initial_cash: float = Field(default=10_000.0, gt=0)
    fee_pct: float = Field(default=0.001, ge=0, le=0.02)
    slippage_pct: float = Field(default=0.0005, ge=0, le=0.02)
    risk_per_trade: float = Field(default=0.01, gt=0, le=0.10)
    stop_loss_pct: float = Field(default=0.02, gt=0, le=0.20)
    take_profit_pct: float = Field(default=0.04, gt=0, le=0.50)


app = FastAPI(
    title="AI Trader Backend",
    version="0.4.0",
    description=(
        "Motor educativo multi-mercado para análisis y backtesting. "
        "No ejecuta órdenes reales."
    ),
)


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/analysis")
def analysis(request: AnalysisRequest):
    try:
        df = fetch_market_data(
            market_type=request.market_type,
            source=request.source,
            symbol=request.symbol,
            timeframe=request.timeframe,
            period=request.period,
        )

        df = add_indicators(df)

        metrics, equity, trades = run_backtest(
            df=df,
            market_type=request.market_type,
            timeframe=request.timeframe,
            initial_cash=request.initial_cash,
            fee_pct=request.fee_pct,
            slippage_pct=request.slippage_pct,
            risk_per_trade=request.risk_per_trade,
            stop_loss_pct=request.stop_loss_pct,
            take_profit_pct=request.take_profit_pct,
        )

        prices = [
            {
                "timestamp": int(row.timestamp),
                "close": float(row.close),
            }
            for row in df.itertuples(index=False)
        ]

        return {
            "source_type": request.source,
            "market_type": request.market_type,
            "symbol": request.symbol,
            "timeframe": request.timeframe,
            "period": request.period,
            "candle_count": len(df),
            "from_timestamp": int(df.iloc[0]["timestamp"]),
            "to_timestamp": int(df.iloc[-1]["timestamp"]),
            "technical": technical_snapshot(df),
            "metrics": metrics,
            "prices": prices,
            "equity": equity,
            "trades": trades,
        }

    except Exception as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
