# Changelog

## 0.12.0

- Límites independientes de posiciones para Argentina, Internacional y Crypto.
- Compatibilidad automática con configuración persistida de v0.11.x.
- Panel de exposición y riesgo de Cartera AUTO.
- Conteo de posiciones abiertas por mercado.
- Exposición monetaria y porcentual separada para ARS/USD.
- Riesgo abierto monetario y porcentual separado para ARS/USD.
- Ranking continuo persistente del Scanner utilizado por Cartera AUTO.
- Registro de decisión por candidato: abierta, score bajo, sin entrada, límite, riesgo, duplicada, etc.
- La pantalla de Cartera AUTO refresca exposición y ranking mientras permanece abierta.
- README y manual actualizados.


## 0.11.1

- Eliminado el botón `Menú` de la barra interna de IA-TradeX.
- `Acerca de IA-TradeX` pasó al menú nativo de la aplicación en macOS.
- Agregado `Salir de IA-TradeX` al menú nativo.
- Se utiliza `MenuBar.setUseSystemMenuBar(true)` para integrarlo en la barra superior de macOS, junto al menú de la aplicación.
- Sin cambios funcionales en Scanner automático, Cartera AUTO ni Paper Trading.


## 0.11.0

- Scanner automático opcional cada 60 segundos.
- Nueva Cartera AUTO multi-activo para Paper Trading.
- Escaneo de todas las watchlists en cada ciclo de Cartera AUTO.
- Apertura solo con señal ENTRADA y Score mínimo configurable.
- Límite configurable de posiciones simultáneas.
- Riesgo global máximo por cuenta ARS/USD.
- Riesgo configurable por operación.
- Capital máximo configurable por operación.
- Prevención de posiciones duplicadas.
- Stop Loss y Take Profit independientes por posición.
- Salida automática por señal de estrategia para posiciones de Cartera AUTO.
- Registro de actividad en el log AUTO.
- Cartera AUTO y AUTO de activo único son modos excluyentes.
- Botón `Cartera AUTO` en Paper Trading.
- Corregido recorte vertical del botón `Menú`.
- README y manual actualizados.


## 0.10.5

- Botones superiores alineados con la fila de selectores, no con las etiquetas.
- Analizar mercado, Scanner, Paper Trading y Menú usan la misma altura de 34 px que los ComboBox.
- Isotipo IX superior aumentado de tamaño para recuperar presencia visual.
- El isotipo sigue sin texto inferior y conserva el recurso limpio para Dock/macOS.
- Se mantienen intactas las métricas compactas y la corrección de Período de v0.10.4.


## 0.10.4

- Corregido el selector `Período`, que todavía podía mostrar `...`.
- `Período` ahora usa un `buttonCell` dedicado con texto sin elipsis.
- Padding interno reducido para mostrar correctamente `1m`, `3m`, `6m` y `1y`.
- Se mantienen sin cambios las métricas compactas corregidas en v0.10.3.
- Vela, Scanner y Paper Trading permanecen sin cambios funcionales.


## 0.10.3

- Selector `Vela` corregido con `buttonCell` dedicado para evitar que JavaFX muestre `...`.
- Padding interno de `Vela` reducido y ancho ajustado para `1h`, `4h` y `1d`.
- Tarjetas de métricas rediseñadas con anchos base mucho más compactos.
- Retorno, Buy & Hold, Win Rate, Profit Factor, Sharpe y Operaciones ya no reservan espacio innecesario.
- Las tarjetas crecen dinámicamente solo cuando el valor mostrado necesita más ancho.
- Espaciado horizontal de métricas reducido.
- La ventana principal y `Acerca de` muestran `v0.10.3` para poder verificar qué build está ejecutándose.
- Se conservan el isotipo-only y los iconos de packaging de v0.10.2.
- Sin cambios funcionales en Scanner ni Paper Trading.


## 0.10.2

- Corrección visual del selector `Vela`: ancho útil aumentado para mostrar `1h`, `4h` y `1d`.
- Métricas redimensionadas con anchos compactos específicos por tipo.
- Las tarjetas ahora crecen dinámicamente cuando el valor numérico realmente lo necesita.
- Menor espacio vacío en Retorno, Buy & Hold, Win Rate, Profit Factor, Sharpe y Operaciones.
- Recurso `icon.png` reemplazado por una versión con solo el isotipo IX.
- Iconos de packaging regenerados con solo el isotipo.
- `IA-TradeX.icns` regenerado para que el icono del Dock de macOS no incluya texto.
- Isotipo superior reducido para evitar recortes y ahorrar espacio.


## 0.10.1

- Correcciones gráficas del encabezado principal.
- El logo superior ahora muestra solo el isotipo mediante viewport, evitando que se corte el texto incluido en `icon.png`.
- La leyenda `100% JAVA · ANÁLISIS / BACKTEST` se movió a una franja superior fina.
- La fila principal conserva Mercado, Fuente, Activo, Vela, Período y acciones en una sola línea.
- Se corrigió el ancho del selector `Vela` para que muestre correctamente `1h`, `4h` y `1d`.
- Selectores superiores ligeramente compactados.
- Tarjetas de métricas más ajustadas al contenido.
- Menor margen lateral en métricas porcentuales y valores cortos.
- Las métricas monetarias conservan crecimiento automático cuando el valor necesita más ancho.
- Scanner y Paper Trading permanecen sin cambios funcionales.


## 0.10.0

- Nueva ventana Watchlist + Scanner.
- Watchlists persistentes separadas para Argentina, Internacional y Crypto.
- Alta y baja manual de activos.
- Configuración por fuente, timeframe y período.
- Scanner ejecutado en segundo plano.
- Un error individual no cancela el resto del escaneo.
- Ranking técnico 0–100 explicable.
- Score basado en tendencia, fuerza, RSI, compatibilidad de estrategia, señal, retorno histórico y volatilidad.
- Resultados ordenados automáticamente por score.
- Doble clic o botón para abrir el activo en el dashboard.
- Integración para enviar un resultado a Paper Trading AUTO.
- Paper Trading AUTO se mantiene pausado al recibir un activo desde Scanner por seguridad.
- README y manual actualizados.


## 0.9.2

- Encabezado principal rediseñado en una sola fila.
- El texto IA-TRADEX fue reemplazado por el icono oficial, sin recortes.
- `Acerca de` se movió al menú superior para liberar espacio.
- Selectores superiores compactados.
- Tarjetas de métricas más pequeñas y con ancho automático según su contenido.
- Menos espacio horizontal desperdiciado en capital, retorno, drawdown, Sharpe y demás métricas.


## 0.9.1

- Corregidas las imágenes del README para GitHub.
- Las capturas ahora usan URLs raw absolutas del repositorio.
- Se mantienen las imágenes dentro de `docs/images/` para versionarlas con Git.


## 0.9.0

- Paper Trading automático por estrategia.
- Activación/pausa persistente.
- Estrategias EMA Cross, Momentum, Mean Reversion y Breakout.
- Selección del activo desde el último análisis realizado.
- Capital máximo configurable.
- Riesgo, Stop Loss y Take Profit configurables.
- Position sizing basado en capital y riesgo.
- Prevención de posición automática duplicada para activo/estrategia.
- Entradas evaluadas con la última vela cerrada.
- Salida automática por señal de estrategia.
- Actividad AUTO persistente con hasta 200 eventos.
- Configuración AUTO persistida en el archivo local de Paper Trading.
- Capturas del README reemplazadas por nuevas imágenes limpias del producto.
- Manual y README actualizados.


## 0.8.0

- Actualización automática de posiciones abiertas cada 60 segundos.
- Botón `Actualizar ahora`.
- Precios en vivo/últimos disponibles para Binance, Kraken, Yahoo Finance y Open BYMADATA.
- Stop Loss automático en Paper Trading.
- Take Profit automático en Paper Trading.
- Cierre automático registrado en historial con motivo.
- Equity y P&L se refrescan después de cada consulta.
- Indicador de última actualización.
- Compatibilidad con posiciones persistidas de versiones anteriores mediante detección de mercado/fuente por defecto.
- Manual y README actualizados.


## 0.7.1

- Corregido selector de cuenta de Paper Trading.
- ARS y USD ahora son botones permanentes y visibles.
- La cuenta activa se muestra explícitamente.
- Acciones de Paper Trading separadas del selector de moneda.
- Mejor comportamiento en ventanas angostas.



## 0.7.0

- Primera versión de Paper Trading manual.
- Cuentas simuladas ARS y USD.
- Capital inicial configurable.
- Persistencia local en `~/.ia-tradex/paper-trading.json`.
- Compra manual del activo analizado.
- Cierre manual de posiciones.
- P&L no realizado y equity.
- Stop Loss y Take Profit registrados.
- Contexto de estrategia y régimen guardado por operación.
- Historial persistente de operaciones cerradas.
- Nuevo botón Paper Trading.
- Acerca de reemplazado por diálogo oscuro integrado visualmente con IA-TradeX.


## 0.6.3

- Licencia migrada a GNU AGPLv3.
- README renovado para GitHub.
- Capturas del producto agregadas a docs/images.
- Sección Acerca de con autor, email y web.
- Botón Acerca de agregado a la aplicación.
- .gitignore ampliado para IDE, build, macOS, logs, paquetes y secretos.


## 0.6.2

- Corregida barra superior responsive.
- Eliminado el uso del mismo control JavaFX en dos contenedores simultáneos.
- La barra superior ahora usa un único FlowPane que reacomoda los controles automáticamente.
- Se mantiene el scroll vertical global.



## 0.6.1

- Scroll vertical global del dashboard.
- Layout responsive para ventanas angostas.
- Barra superior con modo ancho y compacto.
- Selectores superiores reacomodables.
- Métricas y régimen adaptables.
- Menor ancho mínimo de ventana.
- Se evita scroll horizontal del dashboard.



## 0.6.0

- Motor de detección de régimen 100% Java.
- Clasificación ALCISTA / BAJISTA / LATERAL.
- Fuerza BAJA / MEDIA / ALTA.
- Volatilidad relativa basada en ATR porcentual vs mediana histórica del activo.
- Panel visual de régimen.
- Estrategias compatibles según contexto.
- Explicación textual de los criterios utilizados.
- La clasificación continúa siendo por reglas, no Machine Learning.


## 0.5.2

- Corregido disparo del autocomplete en Argentina.
- El debounce de 300 ms ahora se ejecuta en Argentina e Internacional.
- El listener de texto ya no filtra únicamente `stocks`.
- Selección rápida del texto habilitada también para Argentina.


## 0.5.1

- Corregido autocomplete del mercado Argentina.
- El popup ya no descarta resultados porque el market type sea `argentina`.
- La búsqueda captura el mercado antes de iniciar el thread para no leer controles JavaFX desde un hilo secundario.
- Se descartan respuestas viejas si el usuario sigue escribiendo.



## 0.5.0

- Argentina migra de la API autenticada de BYMA a Open BYMADATA.
- Eliminadas credenciales OAuth y archivos de configuración.
- No requiere usuario, contraseña, client_id ni client_secret.
- CEDEARs, panel líder y panel general mediante endpoints públicos.
- Cotización local con último precio, variación, bid, ask y cantidades.
- Histórico OHLCV diario mediante la serie pública de Open BYMADATA.
- Plazo inicial de cotización: 24HS.
- Datos aproximadamente 20 minutos demorados.



## 0.4.0

- Argentina deja de usar Yahoo Finance.
- Integración oficial preparada con BYMA Market Data.
- OAuth 2.0 Client Credentials.
- Búsqueda BYMA de Acciones y CEDEARs.
- Histórico diario por EOD Equity.
- Caché local de respuestas EOD por fecha y categoría.
- Último precio, variación, compra, venta y cantidades en el panel técnico.
- Timeframe Argentina limitado a 1d mientras la fuente oficial usada sea EOD.
- Archivo `byma.properties.example` sin secretos.



## 0.3.0

- Selector de mercado separado en Criptomonedas, Argentina e Internacional.
- Búsqueda argentina prioriza instrumentos Yahoo de Buenos Aires (`.BA`).
- Cotizaciones argentinas mostradas explícitamente en ARS.
- Cotizaciones internacionales separadas de sus equivalentes locales.
- Evita confundir CEDEARs con acciones extranjeras subyacentes.



## 0.2.0

- Motor de múltiples estrategias 100% Java.
- EMA Cross.
- Momentum.
- Mean Reversion.
- Breakout.
- Comparador histórico de retorno, Profit Factor, Drawdown, Sharpe y operaciones.
- Selección interactiva de estrategia para actualizar métricas, equity y trades.
- Etiqueta explícita "MEJOR HISTÓRICO" para evitar confundir backtest con predicción.
- Las cuatro estrategias usan señal de vela previa y ejecución en apertura siguiente.


## 0.1.0

- Corregido launcher para aplicaciones JavaFX empaquetadas con jpackage.

- Icono oficial IA-TradeX integrado en JavaFX.
- Packaging macOS preparado con jpackage.
- Script para generar IA-TradeX.app de doble clic.
- Script opcional para generar instalador DMG.

- Primera versión de IA-TradeX.
- Arquitectura 100% Java.
- JavaFX 23.
- Java 21.
- Eliminada la dependencia de Python/FastAPI.
- Binance REST.
- Kraken REST.
- Yahoo Finance REST.
- Buscador/autocomplete de acciones y ETF.
- Logos con fallback de iniciales.
- EMA 12/26.
- RSI 14.
- ATR 14.
- Backtesting con comisión, slippage, stop-loss y take-profit.
- Profit Factor, Sharpe, Drawdown, Win Rate y Buy & Hold.
- Gráficos de precio y equity.
- Tabla de operaciones.
