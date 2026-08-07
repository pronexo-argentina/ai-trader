# Manual de AI Trader — versión de trabajo 0.2

Este manual parte de cero: no presupone experiencia previa en trading, criptomonedas ni acciones.

## 1. Qué es AI Trader
AI Trader es una aplicación de análisis de mercados. En esta etapa descarga datos públicos reales, calcula indicadores, prueba una estrategia sobre datos históricos y presenta resultados. Todavía NO usa Machine Learning para predecir precios y NO ejecuta órdenes reales.

## 2. Qué es un activo
Un activo es aquello cuyo precio queremos analizar. Ejemplos: BTC (Bitcoin), ETH (Ether), AAPL (acción de Apple) y SPY (ETF ligado al S&P 500).

## 3. Qué significa BTC/USDT
BTC/USDT es un par. BTC es el activo y USDT es la moneda en la que se expresa su precio. Si BTC/USDT cotizara a 70.000, un BTC valdría aproximadamente 70.000 USDT en ese mercado.

## 4. Qué es un exchange
Un exchange es una plataforma electrónica donde se negocian activos, especialmente criptomonedas. AI Trader usa datos públicos vía CCXT y, en esta versión, puede consultar Binance o Kraken. Consultar datos públicos no implica enviar órdenes.

## 5. Qué es una vela
Una vela resume un período y contiene OHLCV: Open (apertura), High (máximo), Low (mínimo), Close (cierre) y Volume (volumen).

## 6. Qué significa timeframe
Es la duración de cada vela. 1h = una hora, 4h = cuatro horas, 1d = un día. Cambiar timeframe cambia la información que ve la estrategia.

## 7. Qué es un backtest
Un backtest simula qué habría ocurrido si una estrategia hubiera aplicado reglas sobre datos históricos. No demuestra que vaya a ganar en el futuro.

## 8. Estrategia inicial
La estrategia usa EMA 12 y EMA 26. La EMA 12 reacciona más rápido; la EMA 26, más lento. Un cruce alcista puede habilitar una entrada larga en la apertura de la vela siguiente. Esto es análisis técnico, NO IA.

## 9. Por qué la entrada se hace en la vela siguiente
Si una señal depende del cierre de una vela, ese cierre solo se conoce cuando la vela terminó. Por eso la simulación usa la apertura de la vela siguiente; así evitamos introducir deliberadamente información futura (look-ahead bias).

## 10. RSI
RSI (Relative Strength Index) mide la intensidad relativa de movimientos recientes. Referencia clásica: <30 sobreventa, 30–50 momentum débil, 50–70 positivo, >70 sobrecompra. No significa automáticamente comprar o vender.

## 11. ATR
ATR (Average True Range) mide cuánto se mueve normalmente el precio. Es una medida de volatilidad, no de dirección.

## 12. Stop-loss
Nivel de salida destinado a limitar una pérdida. Ejemplo: entrada 100, stop 98.

## 13. Take-profit
Nivel de salida previsto para realizar una ganancia. Ejemplo: entrada 100, objetivo 104.

## 14. Comisión
Una plataforma puede cobrar por comprar y vender. Ignorar comisiones hace que un backtest parezca artificialmente mejor. La v0.2 usa 0,10% como hipótesis de trabajo, no como tarifa real garantizada.

## 15. Slippage
Diferencia entre el precio teórico y el precio al que razonablemente podría ejecutarse una orden. La v0.2 incluye una hipótesis de slippage para reducir optimismo.

## 16. Capital final y retorno
Capital final es el dinero ficticio al terminar la simulación. Retorno compara ese capital contra el inicial.

## 17. Drawdown máximo
Es la peor caída desde un máximo previo del capital. Ejemplo: si pasa de 10.000 a 12.000 y baja a 10.800, la caída desde el máximo es 10%.

## 18. Win rate
Porcentaje de operaciones cerradas con ganancia. Un win rate alto no garantiza rentabilidad.

## 19. Profit factor
Relación entre ganancias brutas de operaciones ganadoras y pérdidas brutas de operaciones perdedoras. Más de 1 indica que, en ese backtest, las ganancias brutas superaron a las pérdidas.

## 20. Buy & Hold
Benchmark básico: qué habría pasado si simplemente se compraba al inicio y se mantenía hasta el final. Una estrategia compleja debería justificar por qué aporta valor frente a esta referencia.

## 21. Qué no hace todavía
No hay Machine Learning, LSTM, Transformers, noticias, sentimiento, operaciones reales, paper trading en tiempo real, múltiples estrategias ni walk-forward validation.

## 22. Flujo de uso actual
1. Arrancar backend Python.
2. Arrancar JavaFX.
3. Elegir exchange.
4. Elegir BTC/USDT.
5. Elegir timeframe.
6. Pulsar Analizar mercado.
7. Leer análisis técnico.
8. Comparar estrategia contra Buy & Hold.
9. Observar drawdown, win rate y operaciones.
10. No interpretar el resultado como predicción del futuro.

## 23. Principios del proyecto
Primero datos correctos; luego evitar sesgos; medir riesgo; explicar señales; comparar benchmarks; hacer paper trading; recién después agregar ML; y dejar cualquier uso de dinero real para una etapa posterior y deliberada.
