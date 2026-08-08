package com.iatradex.model;

public final class Candle {
    private final long timestamp;
    private final double open;
    private final double high;
    private final double low;
    private final double close;
    private final double volume;

    private double emaFast = Double.NaN;
    private double emaSlow = Double.NaN;
    private double rsi14 = Double.NaN;
    private double atr14 = Double.NaN;
    private int regime = 0;
    private int cross = 0;

    public Candle(long timestamp, double open, double high, double low, double close, double volume) {
        this.timestamp = timestamp;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = Double.isFinite(volume) ? volume : 0.0;
    }

    public long timestamp() { return timestamp; }
    public double open() { return open; }
    public double high() { return high; }
    public double low() { return low; }
    public double close() { return close; }
    public double volume() { return volume; }

    public double emaFast() { return emaFast; }
    public void emaFast(double value) { this.emaFast = value; }

    public double emaSlow() { return emaSlow; }
    public void emaSlow(double value) { this.emaSlow = value; }

    public double rsi14() { return rsi14; }
    public void rsi14(double value) { this.rsi14 = value; }

    public double atr14() { return atr14; }
    public void atr14(double value) { this.atr14 = value; }

    public int regime() { return regime; }
    public void regime(int value) { this.regime = value; }

    public int cross() { return cross; }
    public void cross(int value) { this.cross = value; }
}
