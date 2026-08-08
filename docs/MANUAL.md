# Manual de IA-TradeX

Este manual parte desde cero y no presupone experiencia previa en trading.

## Qué hace IA-TradeX

IA-TradeX descarga precios históricos reales, calcula indicadores técnicos y simula qué habría ocurrido si una estrategia hubiese operado en ese período.

El resultado de un backtest no predice el futuro. Sirve para estudiar una estrategia bajo datos pasados.

## Mercado

### Criptomonedas

Una criptomoneda como Bitcoin puede negociarse contra otra moneda. `BTC/USDT` significa Bitcoin cotizado en USDT.

IA-TradeX puede consultar actualmente Binance y Kraken.

### Acciones y ETF

Una acción representa una participación en una empresa. Un ETF es un fondo que cotiza en bolsa.

En el selector **Acciones / ETF** podés escribir un ticker o el nombre de una empresa. Ejemplos:

- `AAPL` o `Apple`
- `MSFT` o `Microsoft`
- `NVDA` o `NVIDIA`
- `YPF`

El buscador espera brevemente mientras escribís y muestra sugerencias con ticker, nombre y logo.

`YPF` obtenido desde Yahoo corresponde al ADR negociado en Estados Unidos, no a la acción local de BYMA cotizada en pesos.

## Vela

Una vela resume el movimiento de precio de un período.

Incluye:

- Open: precio de apertura
- High: máximo
- Low: mínimo
- Close: cierre
- Volume: volumen negociado

IA-TradeX permite velas de 1 hora, 4 horas y 1 día.

## Período histórico

Indica cuánto pasado se analizará:

- 1m: aproximadamente un mes
- 3m: aproximadamente tres meses
- 6m: aproximadamente seis meses
- 1y: aproximadamente un año

## EMA

La EMA es una media móvil que da más peso a los precios recientes.

La estrategia base compara:

- EMA 12
- EMA 26

Cuando la EMA rápida pasa sobre la lenta se interpreta como cambio a régimen alcista. Cuando pasa debajo, bajista.

## RSI

RSI significa Relative Strength Index.

Se expresa entre 0 y 100.

Como referencia general:

- más de 70: sobrecomprado
- menos de 30: sobrevendido
- entre 50 y 70: momentum positivo sin estar necesariamente sobrecomprado

## ATR

ATR mide volatilidad. No indica por sí solo si el precio subirá o bajará.

## Stop Loss

Es un nivel de salida destinado a limitar una pérdida.

La estrategia base usa 2%.

## Take Profit

Es un objetivo de salida con ganancia.

La estrategia base usa 4%.

## Comisión

Una operación real puede tener costos. El backtest incluye una comisión simulada.

## Slippage

El precio al que querés operar y el precio real de ejecución pueden diferir. Esa diferencia se denomina slippage.

IA-TradeX incorpora slippage en el backtest.

## Backtest

El backtest reproduce la estrategia sobre precios históricos.

Para reducir look-ahead bias, una señal detectada en una vela se ejecuta en la apertura de la vela siguiente.

## Capital final

Es cuánto dinero habría quedado al finalizar la simulación partiendo del capital inicial.

## Retorno

Es el cambio porcentual del capital.

## Buy & Hold

Es la comparación contra comprar el activo al comienzo y mantenerlo hasta el final.

Una estrategia debería compararse contra alternativas simples, no evaluarse de forma aislada.

## Drawdown

Mide cuánto cayó el capital desde un máximo anterior.

Un drawdown de -10% significa que, desde algún máximo, el capital llegó a caer 10%.

## Win Rate

Porcentaje de operaciones cerradas con ganancia.

Un Win Rate alto no garantiza una estrategia rentable: también importa cuánto se gana al acertar y cuánto se pierde al fallar.

## Profit Factor

Compara ganancias brutas contra pérdidas brutas.

- superior a 1: ganancias brutas mayores a pérdidas brutas
- inferior a 1: pérdidas brutas mayores a ganancias brutas

## Sharpe Ratio

Relaciona retorno con variabilidad de los resultados.

Debe interpretarse junto con otras métricas; no alcanza por sí solo para decidir si una estrategia es buena.

## Señal técnica

La señal visible actualmente no es Machine Learning.

Es una regla explicable basada principalmente en EMA y RSI.

## Importante

IA-TradeX es una herramienta educativa y de investigación. No garantiza resultados futuros ni constituye asesoramiento financiero.


## Ejecutable de escritorio

Durante el desarrollo se puede iniciar IA-TradeX con Maven.

La versión distribuible se genera como `IA-TradeX.app` en macOS. Esa aplicación se abre con doble clic y contiene el runtime necesario para ejecutarse, por lo que el usuario final no necesita iniciar una terminal.

El proyecto incluye además el icono oficial de IA-TradeX y un script para generar un instalador `.dmg`.


## Comparador de estrategias

Después de analizar un mercado, IA-TradeX ejecuta cuatro estrategias sobre los mismos precios históricos.

### EMA Cross

Busca cruces entre EMA 12 y EMA 26. Es la estrategia base con la que comenzó el proyecto.

### Momentum

Busca activos que ya muestran impulso positivo. No intenta comprar simplemente porque el precio subió: exige también tendencia EMA favorable y un RSI compatible.

### Mean Reversion

Parte de la idea de que un precio temporalmente muy alejado de su media puede regresar hacia ella. Busca RSI bajo y precio por debajo de la EMA lenta.

### Breakout

Busca una ruptura por encima de máximos recientes. La entrada exige que el cierre supere el máximo de las 20 velas anteriores.

### Qué significa "MEJOR HISTÓRICO"

La etiqueta **MEJOR HISTÓRICO** significa solamente que esa estrategia obtuvo el mayor retorno en el período que acabás de analizar.

No significa:

- que sea la mejor estrategia en general;
- que vaya a ganar en el futuro;
- que deba usarse con dinero real;
- que haya sido validada fuera de muestra.

Podés seleccionar cualquier fila del comparador para ver sus métricas, curva de capital y operaciones.



## Mercado Argentina e Internacional

El selector **Mercado** distingue tres grupos:

### Criptomonedas

Usa exchanges como Binance o Kraken.

### Argentina

Busca instrumentos negociados en Buenos Aires. En Yahoo Finance suelen identificarse con el sufijo `.BA`.

Ejemplos:

- `AAPL.BA`: instrumento de Apple negociado en Buenos Aires.
- otros CEDEARs y acciones locales pueden aparecer con su ticker específico de Buenos Aires.

En este modo IA-TradeX muestra los precios con el prefijo **AR$**.

### Internacional

Busca acciones y ETF en mercados internacionales.

Por ejemplo, `AAPL` representa la acción estadounidense de Apple y se muestra como **US$** en la interfaz actual.

### Por qué los precios son distintos

Un CEDEAR argentino no es una acción extranjera individual convertida simplemente a pesos. Tiene un ratio de conversión, cotiza localmente y su precio también refleja las condiciones del mercado argentino y el tipo de cambio implícito.

Por eso IA-TradeX mantiene separados **Argentina** e **Internacional**.





## Datos argentinos con Open BYMADATA

El mercado **Argentina** no necesita configuración previa.

IA-TradeX utiliza Open BYMADATA para buscar CEDEARs y acciones argentinas y obtener cotizaciones locales.

La información puede incluir:

- último precio en pesos;
- variación;
- compra;
- cantidad compradora;
- venta;
- cantidad vendedora;
- apertura;
- máximo;
- mínimo;
- volumen;
- histórico diario.

Los datos públicos tienen aproximadamente 20 minutos de demora.

### CEDEAR vs acción internacional

Ejemplo:

- `ASML` en **Argentina**: CEDEAR negociado localmente y expresado en ARS.
- `ASML` en **Internacional**: acción extranjera obtenida mediante Yahoo y expresada en USD.

Aunque compartan un nombre/ticker parecido, son instrumentos diferentes.



## Régimen de mercado

Un mercado no se comporta siempre de la misma manera. Puede pasar por etapas de tendencia, lateralidad y distintos niveles de volatilidad.

IA-TradeX intenta describir ese contexto mediante el panel **Régimen de mercado**.

### Tendencia alcista

La EMA rápida se encuentra suficientemente por encima de la EMA lenta y el precio tuvo avance positivo durante las últimas 20 velas.

### Tendencia bajista

La EMA rápida está suficientemente por debajo de la lenta y el retorno reciente también es negativo.

### Mercado lateral

No hay evidencia suficiente para clasificarlo como tendencia alcista o bajista.

### Volatilidad

IA-TradeX utiliza ATR como medida de movimiento.

Para evitar comparar con la misma escala un CEDEAR, una acción estadounidense y Bitcoin, compara el ATR porcentual actual con el comportamiento típico del mismo activo dentro del período seleccionado.

### Estrategias compatibles

El sistema muestra estrategias cuya lógica encaja conceptualmente con el régimen detectado.

Por ejemplo, Momentum y Breakout suelen tener más sentido para estudiar movimientos tendenciales, mientras Mean Reversion parte de una lógica más compatible con mercados laterales o desviaciones respecto de la media.

**Compatible no significa rentable ni recomendada.** La tabla de backtesting sigue siendo necesaria para observar qué ocurrió históricamente.



## Scroll y tamaño de ventana

IA-TradeX adapta su interfaz al ancho disponible.

Si la ventana es angosta:

- los controles superiores pasan a una disposición compacta;
- las tarjetas se acomodan en varias filas;
- el panel de régimen distribuye sus datos en más de una línea;
- el contenido completo puede recorrerse con scroll vertical.

No es necesario maximizar la aplicación para acceder a los gráficos, operaciones o análisis técnico.


## Acerca de IA-TradeX

**Autor:** Juan Manuel De Castro  
**Email:** jm@pronexo.com  
**Web:** https://www.pronexo.com

IA-TradeX se distribuye bajo la licencia **GNU Affero General Public License v3.0 (AGPL-3.0)**.


## Paper Trading

Paper Trading permite practicar operaciones sin usar dinero real.

### Cuentas simuladas

IA-TradeX mantiene dos cuentas separadas, seleccionables mediante los botones visibles **ARS / USD** en la parte superior de Paper Trading:

- **ARS** para instrumentos del mercado argentino;
- **USD** para instrumentos internacionales y criptomonedas.

El capital inicial es configurable. Mientras una cuenta tenga posiciones abiertas no se puede reiniciar su capital, para evitar inconsistencias.

### Abrir una operación

1. Analizá primero el activo.
2. Abrí **Paper Trading**.
3. Elegí la cuenta correspondiente.
4. Presioná **Comprar activo actual**.
5. Ingresá la cantidad.
6. Opcionalmente cargá Stop Loss y Take Profit.
7. Elegí el contexto o estrategia que querés registrar.
8. Confirmá **Comprar simulado**.

La operación se abre al último precio disponible en el análisis actual.

### Posiciones abiertas

La tabla muestra:

- activo;
- cantidad;
- precio de entrada;
- precio actual;
- P&L no realizado;
- Stop Loss;
- Take Profit;
- estrategia y régimen registrados.

El precio actual se refresca cuando volvés a analizar ese mismo activo.

### Cerrar una posición

Seleccioná una posición y presioná **Cerrar posición**.

IA-TradeX propone el último precio conocido y permite modificarlo antes de confirmar el cierre.

El resultado pasa al historial y el efectivo vuelve a la cuenta simulada.

### Persistencia

Las cuentas, posiciones e historial se guardan automáticamente en:

```text
~/.ia-tradex/paper-trading.json
```

Por eso, cerrar la aplicación no borra la cartera simulada.

### Actualización automática

Mientras la ventana de Paper Trading está abierta, IA-TradeX consulta automáticamente el precio de cada posición abierta cada **60 segundos**.

También existe el botón **Actualizar ahora** para forzar una consulta inmediata.

La pantalla muestra la hora de la última actualización.

### Stop Loss y Take Profit automáticos

Si una posición tiene Stop Loss o Take Profit:

- si el precio consultado es menor o igual al Stop Loss, IA-TradeX cierra la posición simulada con motivo **Stop Loss automático**;
- si el precio consultado es mayor o igual al Take Profit, IA-TradeX cierra la posición simulada con motivo **Take Profit automático**.

El cierre pasa al historial y el efectivo vuelve automáticamente a la cuenta simulada correspondiente.

### Limitaciones de esta etapa

- no existen órdenes reales;
- no hay conexión con brokers;
- la comprobación se realiza cada 60 segundos, no tick a tick;
- el precio de salida es el último precio disponible cuando se detecta el disparador;
- pueden existir saltos entre el nivel configurado y el precio detectado;
- no se modelan todavía comisiones específicas del broker en Paper Trading.



## Paper Trading automático por estrategia

Esta función permite que IA-TradeX abra y cierre operaciones simuladas aplicando una estrategia seleccionada.

### Configuración

Primero analizá el activo en la pantalla principal. Luego abrí **Paper Trading** y presioná **Usar activo analizado**.

Configurá:

- **Automático**: activa o pausa el motor;
- **Estrategia**: EMA Cross, Momentum, Mean Reversion o Breakout;
- **Capital máx.**: monto máximo que una entrada automática puede utilizar;
- **Riesgo %**: pérdida teórica máxima buscada según la distancia al Stop Loss;
- **Stop %**: distancia porcentual del Stop Loss respecto del precio de entrada;
- **Take %**: distancia porcentual del Take Profit.

Presioná **Guardar AUTO**.

### Cómo toma las decisiones

Cada 60 segundos, mientras Paper Trading esté abierto, el motor vuelve a analizar el activo configurado.

Para las señales utiliza la **última vela cerrada**. Esto evita basar una entrada en una vela que todavía puede cambiar antes de cerrar.

Si hay señal de entrada y no existe ya una posición automática de ese activo y estrategia, calcula la cantidad respetando:

- saldo disponible;
- capital máximo configurado;
- riesgo porcentual;
- distancia al Stop Loss.

Si existe una posición abierta y aparece una señal de salida, la cierra al último precio disponible.

Stop Loss y Take Profit siguen teniendo prioridad mediante el monitor de posiciones.

### Actividad AUTO

La pestaña **Actividad AUTO** conserva las últimas evaluaciones y eventos, por ejemplo:

```text
BUY   YPFD · Momentum · compra automática ...
WAIT  YPFD · Momentum · sin señal de entrada
EXIT  YPFD · Momentum · salida por señal
ERROR ...
```

Este registro se guarda junto con el resto del Paper Trading.

### Alcance actual

La primera versión automática administra **un activo configurado a la vez**.

Todavía no escanea todo el mercado ni una lista completa de activos. Esa función se incorporará posteriormente mediante una watchlist/scanner.



## Watchlist y Scanner

Abrí **Scanner** desde la barra superior.

La ventana permite trabajar con tres listas independientes:

- Argentina;
- Internacional;
- Criptomonedas.

### Agregar un activo

1. Elegí el mercado.
2. Elegí la fuente.
3. Seleccioná vela y período.
4. Escribí el ticker o símbolo.
5. Presioná **Agregar**.

Las watchlists se guardan en:

```text
~/.ia-tradex/watchlists.json
```

La instalación inicial incluye ejemplos para facilitar las primeras pruebas.

### Escanear

Presioná **Escanear**. IA-TradeX analiza cada activo en segundo plano.

La tabla se ordena automáticamente por score descendente.

Un error en un símbolo no detiene los demás análisis.

### Score

El score va de 0 a 100 y sirve únicamente para ordenar técnicamente los resultados.

No significa:

- porcentaje de acierto;
- probabilidad de subida;
- probabilidad de ganancia;
- recomendación financiera.

La explicación de cada fila indica qué factores sumaron o restaron puntos.

### Abrir un resultado

Seleccioná una fila y presioná **Abrir en análisis**, o hacé doble clic.

IA-TradeX carga ese análisis en el dashboard principal.

### Enviar a Paper Trading AUTO

El botón **Enviar a Paper AUTO** prepara ese activo y la estrategia del Scanner en Paper Trading.

Por seguridad, la automatización queda inicialmente **pausada**. Debe activarse desde Paper Trading después de revisar capital, riesgo, Stop Loss y Take Profit.



## Cartera AUTO multi-activo

La Cartera AUTO utiliza las watchlists del Scanner para evaluar varios activos y abrir posiciones simuladas de forma controlada.

Abrí **Paper Trading → Cartera AUTO**.

Parámetros:

- **Score mínimo**: umbral técnico exigido al Scanner;
- **Máx. posiciones**: límite de posiciones abiertas simultáneas;
- **Riesgo global %**: suma máxima del riesgo estimado de las posiciones abiertas;
- **Riesgo por operación %**: presupuesto de riesgo para cada nueva entrada;
- **Capital máx. por operación %**: limita concentración por activo;
- **Stop Loss %** y **Take Profit %**.

Para abrir una posición deben cumplirse simultáneamente señal de entrada, Score mínimo, ausencia de posición duplicada, efectivo disponible y límites de riesgo.

El riesgo estimado de una posición long se calcula a partir de la cantidad y la distancia entre el precio de entrada y su Stop Loss. ARS y USD se administran de forma independiente.

La configuración queda persistida en `~/.ia-tradex/paper-trading.json`.

### Scanner automático

El checkbox **Auto 60 s** de Scanner repite el análisis de la watchlist visible cada 60 segundos mientras esa ventana permanezca abierta.

La automatización multi-activo de Paper Trading también trabaja en ciclos de 60 segundos mientras Paper Trading permanezca abierto.

Ninguna de estas funciones envía órdenes reales.


## Gestión avanzada de cartera

Desde **Paper Trading → Cartera AUTO** se pueden definir límites separados por mercado.

Por ejemplo:

```text
Máx. total:          5
Máx. Argentina:      2
Máx. Internacional:  2
Máx. Crypto:         2
```

El máximo total siempre tiene prioridad. Los límites por mercado evitan que una sola categoría concentre todas las posiciones.

### Panel de exposición

El panel muestra la cantidad de posiciones por mercado, exposición monetaria y riesgo estimado para ARS y USD.

**Exposición %** = valor de mercado de posiciones abiertas / equity de la cuenta.

**Riesgo abierto %** = suma de la pérdida hasta Stop Loss de posiciones abiertas / equity de la cuenta.

### Ranking continuo

Cada ejecución de Cartera AUTO reemplaza el ranking anterior por el análisis más reciente de las watchlists.

La columna conceptual `decisión` explica por qué el motor abrió u omitió un candidato. Esto permite auditar las decisiones sin interpretar el Score como una predicción.

El ranking y la configuración se guardan localmente en el estado de Paper Trading.
