# IA-TradeX

![Java](https://img.shields.io/badge/Java-21-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-23-blue)
![Architecture](https://img.shields.io/badge/Architecture-100%25%20Java-success)
![License](https://img.shields.io/badge/License-AGPLv3-blue)
![Status](https://img.shields.io/badge/status-active%20development-yellow)

**IA-TradeX** es una plataforma open source de análisis de mercados, backtesting y experimentación cuantitativa desarrollada **100% en Java**.

No requiere Python, FastAPI, `uvicorn` ni entornos virtuales. La aplicación JavaFX obtiene datos de mercado directamente y ejecuta indicadores, estrategias, backtesting, métricas y clasificación de régimen dentro del mismo proceso Java.

> Proyecto educativo, de investigación y desarrollo de software.  
> No constituye asesoramiento financiero ni garantiza resultados futuros.

## Capturas

### Dashboard · Criptomonedas

![Dashboard de criptomonedas](https://raw.githubusercontent.com/pronexo-argentina/ia-tradex/main/docs/images/dashboard-crypto.png)

### Dashboard · Mercado argentino

![Dashboard de mercado argentino](https://raw.githubusercontent.com/pronexo-argentina/ia-tradex/main/docs/images/dashboard-argentina.png)

### Análisis técnico · Argentina

![Análisis técnico de YPFD](https://raw.githubusercontent.com/pronexo-argentina/ia-tradex/main/docs/images/analysis-argentina.png)

### Análisis técnico · Criptomonedas

![Análisis técnico de BTC/USDT](https://raw.githubusercontent.com/pronexo-argentina/ia-tradex/main/docs/images/analysis-crypto.png)

### Paper Trading

![Paper Trading de IA-TradeX](https://raw.githubusercontent.com/pronexo-argentina/ia-tradex/main/docs/images/paper-trading.png)


> En macOS, **Acerca de IA-TradeX** está integrado en el menú nativo de la aplicación y no ocupa espacio en la barra interna.

## Funcionalidades actuales

### Mercados

**Criptomonedas**
- Binance
- Kraken
- BTC/USDT
- ETH/USDT
- Datos OHLCV públicos por REST

**Argentina**
- CEDEARs
- Acciones argentinas
- Open BYMADATA
- Cotizaciones locales en ARS
- Bid / Ask y cantidades cuando están disponibles
- Histórico diario
- Datos públicos con aproximadamente 20 minutos de demora

**Internacional**
- Acciones y ETF
- Yahoo Finance
- Búsqueda por ticker o nombre
- Autocomplete con logo, ticker, empresa y mercado
- Cotización internacional separada del equivalente argentino

## Estrategias

IA-TradeX compara actualmente cuatro estrategias:

- **EMA Cross**
- **Momentum**
- **Mean Reversion**
- **Breakout**

Todas se prueban sobre el mismo histórico y con los mismos costos simulados.

El comparador muestra:

- retorno;
- Profit Factor;
- Drawdown;
- Sharpe;
- cantidad de operaciones;
- mejor resultado histórico del período.

**MEJOR HISTÓRICO** no significa que esa estrategia vaya a rendir mejor en el futuro.

## Indicadores

- EMA 12
- EMA 26
- RSI 14
- ATR 14
- cruces EMA
- tendencia actual
- señales técnicas explicables

## Régimen de mercado

IA-TradeX clasifica el contexto actual como:

- ALCISTA
- BAJISTA
- LATERAL

También clasifica:

- fuerza: BAJA / MEDIA / ALTA;
- volatilidad: BAJA / MEDIA / ALTA;
- estrategias compatibles con el contexto.

La volatilidad se compara contra el comportamiento histórico del mismo activo usando ATR porcentual.

## Backtesting

- señal calculada sobre la vela anterior;
- ejecución en la apertura de la vela siguiente;
- comisión;
- slippage;
- riesgo por operación;
- stop-loss;
- take-profit;
- salida por señal;
- hipótesis conservadora si stop y objetivo ocurren dentro de una misma vela.

## Métricas

- capital final;
- retorno;
- Buy & Hold;
- Drawdown máximo;
- Win Rate;
- Profit Factor;
- Sharpe Ratio;
- cantidad de operaciones;
- ganancia media;
- pérdida media.

## Interfaz

- JavaFX 23;
- dashboard oscuro;
- gráficos de precio y equity;
- tabla de operaciones;
- comparador de estrategias;
- panel de régimen;
- buscador/autocomplete de activos;
- scroll vertical;
- layout responsive;
- aplicación macOS de doble clic.


## Paper Trading

IA-TradeX incluye una primera etapa de **Paper Trading manual**, sin dinero real.

Características actuales:

- cuentas simuladas independientes en **ARS** y **USD**;
- capital inicial configurable;
- saldo disponible;
- equity;
- P&L no realizado;
- compra manual del activo analizado;
- cierre manual de posiciones;
- cantidad, precio de entrada y precio actual;
- Stop Loss y Take Profit registrados;
- estrategia/contexto elegido al abrir;
- régimen de mercado registrado al abrir;
- historial de operaciones cerradas;
- persistencia local automática.

Los datos se guardan en:

```text
~/.ia-tradex/paper-trading.json
```

El precio actual de una posición se actualiza cuando se vuelve a analizar ese activo en IA-TradeX.

**Importante:** esta etapa no envía órdenes a brokers ni exchanges.

Mientras la ventana de Paper Trading está abierta, IA-TradeX actualiza automáticamente las posiciones cada **60 segundos**. Si el último precio consultado alcanza un Stop Loss o Take Profit configurado, la posición simulada se cierra automáticamente y el motivo queda registrado en el historial.

La ejecución es una simulación por sondeo periódico: no es tick a tick y el precio de cierre utilizado es el último precio disponible en el refresco.




## Watchlist + Scanner

IA-TradeX incorpora un **Scanner manual** para analizar varios activos desde una watchlist persistente.

Las listas se separan por:

- Argentina;
- Internacional;
- Criptomonedas.

El Scanner muestra:

- activo;
- mercado;
- régimen;
- volatilidad;
- RSI;
- estrategia técnica seleccionada;
- señal;
- score técnico;
- explicación del score.

### Score técnico 0–100

El score **no representa una probabilidad de ganancia**. Es un ranking explicable que suma o resta puntos según:

- tendencia;
- fuerza del régimen;
- RSI;
- compatibilidad entre régimen y estrategia;
- existencia de señal de entrada;
- retorno histórico de la estrategia en el período analizado;
- volatilidad.

Los resultados se ordenan de mayor a menor score.

Desde un resultado se puede:

- abrir el activo directamente en el dashboard;
- hacer doble clic para abrirlo;
- enviarlo a la configuración de **Paper Trading AUTO**.

El escaneo se ejecuta en segundo plano para no bloquear la interfaz. Si un activo falla, el resto de la watchlist continúa analizándose.


## Paper Trading automático por estrategia

IA-TradeX puede automatizar una estrategia sobre **un activo previamente analizado**.

Configuración disponible:

- activar / pausar modo automático;
- estrategia: EMA Cross, Momentum, Mean Reversion o Breakout;
- capital máximo asignado;
- riesgo porcentual por operación;
- Stop Loss porcentual;
- Take Profit porcentual.

El motor:

1. actualiza las posiciones abiertas;
2. ejecuta Stop Loss / Take Profit simulados;
3. descarga nuevamente el histórico del activo configurado;
4. recalcula indicadores;
5. evalúa la **última vela cerrada**, evitando usar una vela todavía en formación;
6. abre una posición si existe señal de entrada y no hay otra posición automática equivalente;
7. calcula la cantidad usando simultáneamente el capital máximo y el riesgo permitido;
8. cierra por señal de estrategia cuando corresponde;
9. registra cada evaluación en **Actividad AUTO**.

En instrumentos argentinos la cantidad automática se redondea a unidades enteras. Para crypto e internacional la simulación admite cantidades fraccionarias.

La automatización se evalúa cada **60 segundos mientras la ventana Paper Trading permanece abierta**. La configuración queda persistida localmente.

> Esta funcionalidad es exclusivamente Paper Trading. No se conecta a brokers ni envía órdenes reales.


## Arquitectura

```text
┌────────────────────────────────────────┐
│             JavaFX Desktop             │
│                                        │
│ UI · búsqueda · gráficos · portfolio   │
└────────────────────┬───────────────────┘
                     │ Java
                     ▼
┌────────────────────────────────────────┐
│           Motor IA-TradeX              │
│                                        │
│ MarketService                          │
│ IndicatorEngine                        │
│ MarketRegimeEngine                     │
│ StrategySignalEngine                   │
│ BacktestEngine                         │
│ AnalysisService                        │
└────────────────────┬───────────────────┘
                     │ HTTP REST
          ┌──────────┼──────────────┐
          ▼          ▼              ▼
      Binance      Kraken      Yahoo / BYMADATA
```

## Requisitos de desarrollo

- Java 21+
- Maven

## Ejecutar durante el desarrollo

```bash
mvn clean javafx:run
```

## Crear la aplicación macOS

```bash
./package-macos.sh
```

Resultado:

```text
dist/IA-TradeX.app
```

Para generar un instalador DMG:

```bash
./package-macos-dmg.sh
```

La aplicación empaquetada incluye su runtime Java, por lo que el usuario final no necesita Maven ni Python.

## Documentación

- [Manual de usuario](docs/MANUAL.md)
- [Changelog](docs/CHANGELOG.md)

El manual se mantiene durante el desarrollo para que la documentación final no tenga que reconstruirse desde cero.

## Próximas etapas

- ejecución automática de Paper Trading por estrategia
- actualización más frecuente mediante streaming/WebSocket
- WebSockets / datos en vivo
- validación walk-forward
- validación fuera de muestra
- Machine Learning
- noticias y sentimiento
- explicabilidad de modelos

## Acerca de

**Juan Manuel De Castro**  
**Email:** jm@pronexo.com  
**Web:** https://www.pronexo.com

## Licencia

IA-TradeX se distribuye bajo la **GNU Affero General Public License v3.0 (AGPL-3.0)**.

La AGPLv3 exige que las modificaciones de la aplicación también permanezcan disponibles bajo los términos de la licencia cuando el software se distribuye o se ofrece a usuarios a través de una red.

Consulta el archivo [LICENSE](LICENSE) para el texto completo.


## Scanner automático + Cartera AUTO multi-activo

IA-TradeX v0.11.0 extiende el Scanner y Paper Trading para trabajar con varios activos simultáneamente.

### Scanner automático

En la ventana Scanner se puede activar **Auto 60 s**. Mientras la ventana permanezca abierta, IA-TradeX vuelve a analizar periódicamente la watchlist seleccionada y actualiza el ranking técnico.

### Cartera AUTO

Desde Paper Trading, el botón **Cartera AUTO** permite configurar:

- Score mínimo requerido;
- máximo de posiciones simultáneas;
- riesgo global máximo;
- riesgo por operación;
- capital máximo por operación;
- Stop Loss;
- Take Profit.

Cada ciclo:

1. actualiza precios de las posiciones abiertas;
2. ejecuta Stop Loss / Take Profit simulados;
3. evalúa señales de salida de posiciones creadas por el Scanner;
4. escanea todas las watchlists;
5. ordena candidatos por Score;
6. exige señal `ENTRADA`;
7. evita posiciones duplicadas;
8. verifica límite de posiciones, efectivo y riesgo;
9. calcula el tamaño de la nueva posición;
10. registra entradas, salidas y decisiones en Actividad AUTO.

El riesgo global se controla por cuenta simulada ARS/USD a partir de la distancia entre precio de entrada y Stop Loss.

**Cartera AUTO y el modo AUTO de un solo activo no se ejecutan simultáneamente.**

> Sigue siendo Paper Trading. IA-TradeX no envía órdenes a brokers ni exchanges.


## Gestión avanzada de Cartera AUTO

IA-TradeX v0.12.0 incorpora una capa adicional de control sobre el Paper Trading multi-activo.

### Límites por mercado

Además del máximo total de posiciones, se puede definir un máximo independiente para:

- Argentina;
- Internacional;
- Criptomonedas.

Una entrada solo se ejecuta si hay espacio tanto en el límite total como en el límite de su mercado.

### Exposición y riesgo

La ventana **Cartera AUTO** muestra en tiempo real:

- posiciones abiertas totales;
- posiciones abiertas por mercado;
- exposición total ARS;
- exposición total USD;
- porcentaje de exposición sobre equity;
- riesgo abierto ARS;
- riesgo abierto USD;
- porcentaje de riesgo abierto sobre equity.

La exposición utiliza el último precio disponible de cada posición.

### Ranking continuo

Cada ciclo de Cartera AUTO guarda el ranking técnico más reciente del Scanner.

La pantalla muestra para cada candidato:

- símbolo;
- mercado;
- Score;
- señal;
- estrategia;
- decisión tomada por el motor.

Ejemplos de decisiones:

- `ABIERTA`;
- `SIN ENTRADA`;
- `SCORE BAJO`;
- `LÍMITE TOTAL`;
- `LÍMITE Argentina`;
- `RIESGO GLOBAL`;
- `DUPLICADA`;
- `SIN EFECTIVO`.

El ranking se refresca visualmente mientras la ventana de Cartera AUTO está abierta.

> El Score sigue siendo un ranking técnico explicable, no una probabilidad de ganancia.
