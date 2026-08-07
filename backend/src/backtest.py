from dataclasses import dataclass
import pandas as pd

@dataclass
class Trade:
    entry_time:int; exit_time:int; entry_price:float; exit_price:float; quantity:float; pnl:float; reason:str

def _position_size(cash, entry_price, stop_loss_pct, risk_per_trade):
    risk_budget = cash * risk_per_trade
    risk_per_unit = entry_price * stop_loss_pct
    if risk_per_unit <= 0: return 0.0
    return max(0.0, min(risk_budget / risk_per_unit, cash / entry_price))

def run_backtest(df, initial_cash, fee_pct, slippage_pct, risk_per_trade, stop_loss_pct, take_profit_pct):
    cash=initial_cash; qty=0.0; entry_price=None; entry_time=None; trades=[]; equity=[]
    for i in range(1, len(df)):
        prev=df.iloc[i-1]; row=df.iloc[i]
        ts=int(row["timestamp"]); op=float(row["open"]); hi=float(row["high"]); lo=float(row["low"]); close=float(row["close"])
        if qty == 0 and float(prev["cross"]) > 0:
            entry=op*(1+slippage_pct); new_qty=_position_size(cash,entry,stop_loss_pct,risk_per_trade)
            cost=new_qty*entry; fee=cost*fee_pct
            if new_qty > 0 and cost+fee <= cash:
                cash -= cost+fee; qty=new_qty; entry_price=entry; entry_time=ts
        if qty > 0:
            stop=entry_price*(1-stop_loss_pct); target=entry_price*(1+take_profit_pct); reason=None; exit_price=None
            if lo <= stop:
                exit_price=stop*(1-slippage_pct); reason="stop_loss"
            elif hi >= target:
                exit_price=target*(1-slippage_pct); reason="take_profit"
            elif float(prev["cross"]) < 0:
                exit_price=op*(1-slippage_pct); reason="signal_exit"
            if reason:
                proceeds=qty*exit_price; exit_fee=proceeds*fee_pct; cash += proceeds-exit_fee
                entry_fee=entry_price*qty*fee_pct; pnl=((exit_price-entry_price)*qty)-entry_fee-exit_fee
                trades.append(Trade(entry_time,ts,entry_price,exit_price,qty,pnl,reason))
                qty=0.0; entry_price=None; entry_time=None
        equity.append({"index":i,"equity":float(cash + (qty*close if qty > 0 else 0.0))})
    if qty > 0:
        row=df.iloc[-1]; exit_price=float(row["close"])*(1-slippage_pct); proceeds=qty*exit_price; exit_fee=proceeds*fee_pct; cash += proceeds-exit_fee
        entry_fee=entry_price*qty*fee_pct; pnl=((exit_price-entry_price)*qty)-entry_fee-exit_fee
        trades.append(Trade(entry_time,int(row["timestamp"]),entry_price,exit_price,qty,pnl,"end_of_data"))
        if equity: equity[-1]["equity"]=float(cash)
    running_max=initial_cash; max_dd=0.0
    for p in equity or [{"equity":initial_cash}]:
        running_max=max(running_max,p["equity"]); max_dd=min(max_dd,p["equity"]/running_max-1)
    wins=[t for t in trades if t.pnl>0]; losses=[t for t in trades if t.pnl<0]
    gross_profit=sum(t.pnl for t in wins); gross_loss=abs(sum(t.pnl for t in losses)); pf=None if gross_loss==0 else gross_profit/gross_loss
    first=float(df.iloc[0]["open"])*(1+slippage_pct); last=float(df.iloc[-1]["close"])*(1-slippage_pct)
    buy_hold=((last*(1-fee_pct))/(first*(1+fee_pct))-1)*100
    metrics={"capital_initial":initial_cash,"capital_final":cash,"return_pct":(cash/initial_cash-1)*100,"max_drawdown_pct":max_dd*100,"trades":len(trades),"winners":len(wins),"win_rate_pct":(len(wins)/len(trades)*100 if trades else 0.0),"profit_factor":pf,"buy_hold_return_pct":buy_hold}
    return metrics,equity,[t.__dict__ for t in trades]
