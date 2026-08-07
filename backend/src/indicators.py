import numpy as np
import pandas as pd

def add_indicators(df: pd.DataFrame) -> pd.DataFrame:
    out = df.copy()
    out["ema_fast"] = out["close"].ewm(span=12, adjust=False).mean()
    out["ema_slow"] = out["close"].ewm(span=26, adjust=False).mean()
    delta = out["close"].diff()
    gain = delta.clip(lower=0)
    loss = -delta.clip(upper=0)
    avg_gain = gain.ewm(alpha=1/14, adjust=False, min_periods=14).mean()
    avg_loss = loss.ewm(alpha=1/14, adjust=False, min_periods=14).mean()
    rs = avg_gain / avg_loss.replace(0, np.nan)
    out["rsi14"] = 100 - (100 / (1 + rs))
    prev_close = out["close"].shift(1)
    tr = pd.concat([out["high"]-out["low"], (out["high"]-prev_close).abs(), (out["low"]-prev_close).abs()], axis=1).max(axis=1)
    out["atr14"] = tr.ewm(alpha=1/14, adjust=False, min_periods=14).mean()
    out["regime"] = np.where(out["ema_fast"] > out["ema_slow"], 1, -1)
    out["cross"] = pd.Series(out["regime"], index=out.index).diff().fillna(0)
    return out

def technical_snapshot(df: pd.DataFrame) -> dict:
    row = df.iloc[-1]
    trend = "ALCISTA" if row["ema_fast"] > row["ema_slow"] else "BAJISTA"
    rsi = None if pd.isna(row["rsi14"]) else float(row["rsi14"])
    atr = None if pd.isna(row["atr14"]) else float(row["atr14"])
    if rsi is None: momentum = "SIN DATOS"
    elif rsi >= 70: momentum = "SOBRECOMPRADO"
    elif rsi <= 30: momentum = "SOBREVENDIDO"
    elif rsi >= 50: momentum = "POSITIVO"
    else: momentum = "NEGATIVO"
    if trend == "ALCISTA" and rsi is not None and 50 <= rsi < 70:
        signal = "OBSERVAR POSIBLE COMPRA"
        explanation = "EMA rápida sobre EMA lenta y RSI positivo sin sobrecompra. Es una regla técnica, no IA."
    elif trend == "BAJISTA":
        signal = "ESPERAR"
        explanation = "EMA rápida por debajo de EMA lenta. La estrategia base evita nuevas posiciones largas."
    else:
        signal = "ESPERAR"
        explanation = "Las condiciones técnicas no cumplen simultáneamente las reglas mínimas."
    return {"last_price":float(row["close"]),"ema_fast":float(row["ema_fast"]),"ema_slow":float(row["ema_slow"]),"rsi14":rsi,"atr14":atr,"trend":trend,"momentum":momentum,"signal":signal,"explanation":explanation}
