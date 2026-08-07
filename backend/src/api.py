from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from .market import fetch_ohlcv
from .indicators import add_indicators, technical_snapshot
from .backtest import run_backtest

class AnalysisRequest(BaseModel):
    exchange:str="binance"; symbol:str="BTC/USDT"; timeframe:str="1h"; limit:int=Field(500,ge=100,le=1000)
    initial_cash:float=10000.0; fee_pct:float=0.001; slippage_pct:float=0.0005; risk_per_trade:float=0.01; stop_loss_pct:float=0.02; take_profit_pct:float=0.04

app=FastAPI(title="AI Trader Backend",version="0.2.0",description="Análisis educativo y backtesting. No ejecuta órdenes reales.")

@app.get("/health")
def health(): return {"status":"ok"}

@app.post("/analysis")
def analysis(r:AnalysisRequest):
    try:
        df=add_indicators(fetch_ohlcv(r.exchange,r.symbol,r.timeframe,r.limit))
        metrics,equity,trades=run_backtest(df,r.initial_cash,r.fee_pct,r.slippage_pct,r.risk_per_trade,r.stop_loss_pct,r.take_profit_pct)
        prices=[{"timestamp":int(x.timestamp),"close":float(x.close)} for x in df.tail(250).itertuples(index=False)]
        return {"source":"real_market_data","exchange":r.exchange,"symbol":r.symbol,"timeframe":r.timeframe,"technical":technical_snapshot(df),"metrics":metrics,"prices":prices,"equity":equity[-250:],"trades":trades[-50:]}
    except Exception as exc:
        raise HTTPException(status_code=400,detail=str(exc)) from exc
