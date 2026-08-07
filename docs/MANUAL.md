# Manual de AI Trader

Este manual parte de cero.

## Mercados

AI Trader ahora diferencia dos tipos de mercado.

### Criptomonedas

Ejemplos:
- BTC/USDT
- ETH/USDT

Funcionan prácticamente 24 horas por día, 7 días por semana.

Las fuentes iniciales son Binance y Kraken mediante datos públicos.

### Acciones y ETF

Ejemplos:
- `AAPL`: Apple Inc.
- `ASML`: ASML Holding
- `YPF`: YPF S.A. ADR que cotiza en Estados Unidos
- `SPY`: ETF del S&P 500
- `QQQ`: ETF ligado al Nasdaq-100

La fuente inicial es Yahoo Finance mediante la librería yfinance.

## Diferencia importante

Las acciones no operan 24/7. Tienen:

- horarios de mercado;
- fines de semana sin negociación;
- feriados bursátiles;
- posibles dividendos;
- splits y otros eventos corporativos.

Por eso una vela de 1 hora bursátil no equivale a una hora continua de un
mercado cripto.

## YPF

El símbolo `YPF` usado en esta versión corresponde al ADR de YPF que cotiza
en Estados Unidos. No es directamente la especie local de BYMA en pesos.

Más adelante podemos agregar mercado argentino y símbolos locales por una
fuente específica.

## Fuente Yahoo

Yahoo Finance se usa como fuente práctica para investigación y backtesting.

No debe tratarse como un feed oficial de ejecución en tiempo real.

## Señales

El mismo motor de EMA, RSI, ATR y backtesting puede ejecutarse sobre cripto o
acciones, pero eso no significa que una misma estrategia sea igualmente buena
para ambos mercados.

Precisamente por eso AI Trader compara resultados y riesgo.

## Sharpe

La anualización se adapta de forma aproximada al mercado:

- cripto: mercado continuo;
- acciones: aproximadamente 252 ruedas bursátiles por año.

## Próximo objetivo

Antes de Machine Learning:

1. comparar estrategias;
2. distinguir regímenes de mercado;
3. validar fuera de muestra;
4. agregar paper trading;
5. documentar cada decisión.
