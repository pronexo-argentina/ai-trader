from dataclasses import dataclass
import math

import pandas as pd


@dataclass
class Trade:
    entry_time: int
    exit_time: int
    entry_price: float
    exit_price: float
    quantity: float
    pnl: float
    reason: str


def _position_size(cash, entry_price, stop_loss_pct, risk_per_trade):
    risk_budget = cash * risk_per_trade
    risk_per_unit = entry_price * stop_loss_pct

    if risk_per_unit <= 0:
        return 0.0

    return max(0.0, min(risk_budget / risk_per_unit, cash / entry_price))


def _periods_per_year(timeframe: str, market_type: str) -> float:
    if market_type == "crypto":
        return {"1h": 24 * 365, "4h": 6 * 365, "1d": 365}[timeframe]

    # Aproximación para acciones: 252 ruedas al año.
    return {"1h": 6.5 * 252, "4h": 2 * 252, "1d": 252}[timeframe]


def _sharpe_ratio(equity_curve, timeframe, market_type):
    if len(equity_curve) < 3:
        return None

    values = pd.Series([p["equity"] for p in equity_curve], dtype=float)
    returns = values.pct_change().dropna()

    if len(returns) < 2:
        return None

    std = returns.std(ddof=1)
    if std == 0 or pd.isna(std):
        return None

    return float(
        (returns.mean() / std)
        * math.sqrt(_periods_per_year(timeframe, market_type))
    )


def run_backtest(
    df,
    market_type,
    timeframe,
    initial_cash,
    fee_pct,
    slippage_pct,
    risk_per_trade,
    stop_loss_pct,
    take_profit_pct,
):
    cash = initial_cash
    qty = 0.0
    entry_price = None
    entry_time = None

    trades = []
    equity_curve = []

    for i in range(1, len(df)):
        prev = df.iloc[i - 1]
        row = df.iloc[i]

        ts = int(row["timestamp"])
        open_price = float(row["open"])
        high = float(row["high"])
        low = float(row["low"])
        close = float(row["close"])

        if qty == 0 and float(prev["cross"]) > 0:
            entry = open_price * (1 + slippage_pct)
            new_qty = _position_size(
                cash,
                entry,
                stop_loss_pct,
                risk_per_trade,
            )

            cost = new_qty * entry
            entry_fee = cost * fee_pct

            if new_qty > 0 and cost + entry_fee <= cash:
                cash -= cost + entry_fee
                qty = new_qty
                entry_price = entry
                entry_time = ts

        if qty > 0:
            stop = entry_price * (1 - stop_loss_pct)
            target = entry_price * (1 + take_profit_pct)

            reason = None
            exit_price = None

            # Si stop y objetivo caen dentro de la misma vela, asumimos
            # stop primero como hipótesis conservadora.
            if low <= stop:
                exit_price = stop * (1 - slippage_pct)
                reason = "stop_loss"
            elif high >= target:
                exit_price = target * (1 - slippage_pct)
                reason = "take_profit"
            elif float(prev["cross"]) < 0:
                exit_price = open_price * (1 - slippage_pct)
                reason = "signal_exit"

            if reason:
                proceeds = qty * exit_price
                exit_fee = proceeds * fee_pct
                cash += proceeds - exit_fee

                entry_fee = entry_price * qty * fee_pct
                pnl = ((exit_price - entry_price) * qty) - entry_fee - exit_fee

                trades.append(
                    Trade(
                        entry_time=entry_time,
                        exit_time=ts,
                        entry_price=entry_price,
                        exit_price=exit_price,
                        quantity=qty,
                        pnl=pnl,
                        reason=reason,
                    )
                )

                qty = 0.0
                entry_price = None
                entry_time = None

        equity_curve.append({
            "timestamp": ts,
            "equity": float(cash + (qty * close if qty > 0 else 0.0)),
        })

    if qty > 0:
        row = df.iloc[-1]
        ts = int(row["timestamp"])
        exit_price = float(row["close"]) * (1 - slippage_pct)

        proceeds = qty * exit_price
        exit_fee = proceeds * fee_pct
        cash += proceeds - exit_fee

        entry_fee = entry_price * qty * fee_pct
        pnl = ((exit_price - entry_price) * qty) - entry_fee - exit_fee

        trades.append(
            Trade(
                entry_time=entry_time,
                exit_time=ts,
                entry_price=entry_price,
                exit_price=exit_price,
                quantity=qty,
                pnl=pnl,
                reason="end_of_data",
            )
        )

        if equity_curve:
            equity_curve[-1]["equity"] = float(cash)

    running_max = initial_cash
    max_dd = 0.0

    for point in equity_curve:
        value = point["equity"]
        running_max = max(running_max, value)
        max_dd = min(max_dd, value / running_max - 1)

    winners = [t for t in trades if t.pnl > 0]
    losers = [t for t in trades if t.pnl < 0]

    gross_profit = sum(t.pnl for t in winners)
    gross_loss = abs(sum(t.pnl for t in losers))

    profit_factor = None if gross_loss == 0 else gross_profit / gross_loss
    avg_win = None if not winners else gross_profit / len(winners)
    avg_loss = None if not losers else sum(t.pnl for t in losers) / len(losers)

    first = float(df.iloc[0]["open"]) * (1 + slippage_pct)
    last = float(df.iloc[-1]["close"]) * (1 - slippage_pct)

    buy_hold = (
        (last * (1 - fee_pct))
        / (first * (1 + fee_pct))
        - 1
    ) * 100

    metrics = {
        "capital_initial": initial_cash,
        "capital_final": cash,
        "return_pct": (cash / initial_cash - 1) * 100,
        "max_drawdown_pct": max_dd * 100,
        "trades": len(trades),
        "winners": len(winners),
        "win_rate_pct": (len(winners) / len(trades) * 100) if trades else 0.0,
        "profit_factor": profit_factor,
        "avg_win": avg_win,
        "avg_loss": avg_loss,
        "sharpe_ratio": _sharpe_ratio(
            equity_curve,
            timeframe,
            market_type,
        ),
        "buy_hold_return_pct": buy_hold,
    }

    return metrics, equity_curve, [t.__dict__ for t in trades]
