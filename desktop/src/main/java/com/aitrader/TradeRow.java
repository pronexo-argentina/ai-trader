package com.aitrader;
import javafx.beans.property.*;
public class TradeRow {
    private final StringProperty time=new SimpleStringProperty(); private final DoubleProperty entry=new SimpleDoubleProperty(); private final DoubleProperty exit=new SimpleDoubleProperty(); private final DoubleProperty pnl=new SimpleDoubleProperty(); private final StringProperty reason=new SimpleStringProperty();
    public TradeRow(String t,double e,double x,double p,String r){time.set(t);entry.set(e);exit.set(x);pnl.set(p);reason.set(r);}    
    public StringProperty timeProperty(){return time;} public DoubleProperty entryProperty(){return entry;} public DoubleProperty exitProperty(){return exit;} public DoubleProperty pnlProperty(){return pnl;} public StringProperty reasonProperty(){return reason;}
}
