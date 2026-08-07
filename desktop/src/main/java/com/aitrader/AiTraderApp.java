package com.aitrader;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class AiTraderApp extends Application {

    private final TradingApiClient api = new TradingApiClient();

    private final ComboBox<String> marketBox = new ComboBox<>();
    private final ComboBox<String> sourceBox = new ComboBox<>();
    private final ComboBox<String> symbolBox = new ComboBox<>();
    private final TextField stockSearchField = new TextField();
    private final ContextMenu stockSearchMenu = new ContextMenu();
    private final PauseTransition stockSearchDelay =
            new PauseTransition(Duration.millis(300));
    private final StackPane assetSelectorPane = new StackPane();
    private final StackPane selectedAssetLogo = new StackPane();
    private final Label selectedAssetInitials = new Label("AA");
    private final ImageView selectedAssetImage = new ImageView();
    private String selectedStockSymbol = "AAPL";
    private final ComboBox<String> timeframeBox = new ComboBox<>();
    private final ComboBox<String> periodBox = new ComboBox<>();

    private final Label capitalValue = metricValue("—");
    private final Label returnValue = metricValue("—");
    private final Label buyHoldValue = metricValue("—");
    private final Label drawdownValue = metricValue("—");
    private final Label winRateValue = metricValue("—");
    private final Label profitFactorValue = metricValue("—");
    private final Label sharpeValue = metricValue("—");
    private final Label tradesValue = metricValue("—");
    private final Label avgWinValue = metricValue("—");
    private final Label avgLossValue = metricValue("—");

    private final Label lastPrice = new Label("—");
    private final Label trend = new Label("—");
    private final Label rsi = new Label("—");
    private final Label signal = new Label("Sin análisis");
    private final Label explanation = new Label("Ejecutá el análisis para consultar datos reales.");
    private final Label status = new Label("Backend: sin consultar");

    private final LineChart<String, Number> priceChart = createLineChart("Precio");
    private final LineChart<String, Number> equityChart = createLineChart("Capital");
    private final TableView<TradeRow> tradesTable = createTradesTable();

    private final Button analyzeButton = new Button("Analizar mercado");

    private final DateTimeFormatter chartDate =
            DateTimeFormatter.ofPattern("dd/MM").withZone(ZoneId.systemDefault());

    private final DateTimeFormatter fullDate =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");

        configureSelectors();
        root.setTop(createTopBar());

        VBox page = new VBox(12);
        page.setPadding(new Insets(16));

        FlowPane metrics = createMetrics();

        SplitPane charts = new SplitPane(
                panel("Precio real", priceChart),
                panel("Curva de capital", equityChart)
        );
        charts.setDividerPositions(0.5);

        SplitPane bottom = new SplitPane(
                panel("Operaciones del backtest", tradesTable),
                createAnalysisPanel()
        );
        bottom.setDividerPositions(0.70);

        page.getChildren().addAll(metrics, charts, bottom);
        VBox.setVgrow(charts, Priority.ALWAYS);
        VBox.setVgrow(bottom, Priority.ALWAYS);
        root.setCenter(page);

        Scene scene = new Scene(root, 1550, 930);
        scene.getStylesheets().add(
                AiTraderApp.class.getResource("/com/aitrader/theme.css").toExternalForm()
        );

        stage.setTitle("AI Trader");
        stage.setMinWidth(1180);
        stage.setMinHeight(780);
        stage.setScene(scene);
        stage.show();
    }

    private void configureSelectors() {
        marketBox.getItems().addAll("Criptomonedas", "Acciones / ETF");
        marketBox.setValue("Criptomonedas");
        marketBox.setOnAction(e -> {
            refreshMarketSelectors();
            Platform.runLater(marketBox::hide);
        });

        timeframeBox.getItems().addAll("1h", "4h", "1d");
        timeframeBox.setValue("1h");

        periodBox.getItems().addAll("1m", "3m", "6m", "1y");
        periodBox.setValue("3m");

        marketBox.setMinWidth(150);
        marketBox.setPrefWidth(150);
        marketBox.setMaxWidth(150);

        sourceBox.setMinWidth(110);
        sourceBox.setPrefWidth(110);
        sourceBox.setMaxWidth(110);

        // El bloque Activo mantiene el mismo ancho en ambos mercados
        // para que la barra superior no se mueva al alternar.
        symbolBox.setMinWidth(300);
        symbolBox.setPrefWidth(300);
        symbolBox.setMaxWidth(300);

        timeframeBox.setMinWidth(85);
        timeframeBox.setPrefWidth(85);
        timeframeBox.setMaxWidth(85);

        periodBox.setMinWidth(95);
        periodBox.setPrefWidth(95);
        periodBox.setMaxWidth(95);

        stockSearchField.setPromptText("Buscar acción o ticker...");
        stockSearchField.setText("AAPL — Apple Inc.");
        stockSearchField.setMinWidth(255);
        stockSearchField.setPrefWidth(255);
        stockSearchField.setMaxWidth(255);
        stockSearchField.getStyleClass().add("asset-search-field");

        stockSearchMenu.getStyleClass().add("asset-search-menu");

        stockSearchDelay.setOnFinished(e -> {
            if ("stocks".equals(marketType())) {
                searchStocks(stockSearchField.getText());
            }
        });

        stockSearchField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!"stocks".equals(marketType())) {
                return;
            }

            selectedStockSymbol = null;
            String value = newValue == null ? "" : newValue.trim();

            if (value.length() < 2) {
                stockSearchDelay.stop();
                stockSearchMenu.hide();
                return;
            }

            stockSearchDelay.playFromStart();
        });

        stockSearchField.setOnAction(e -> {
            stockSearchMenu.hide();
            analyze();
        });

        selectedAssetInitials.getStyleClass().add("selected-asset-initials");
        selectedAssetImage.setFitWidth(24);
        selectedAssetImage.setFitHeight(24);
        selectedAssetImage.setPreserveRatio(true);
        selectedAssetImage.getStyleClass().add("selected-asset-image");

        selectedAssetLogo.getChildren().addAll(
                selectedAssetInitials,
                selectedAssetImage
        );
        selectedAssetLogo.getStyleClass().add("selected-asset-logo");
        selectedAssetLogo.setMinSize(30, 30);
        selectedAssetLogo.setPrefSize(30, 30);
        selectedAssetLogo.setMaxSize(30, 30);

        HBox stockSearchControl = new HBox(
                8,
                selectedAssetLogo,
                stockSearchField
        );
        stockSearchControl.setAlignment(Pos.CENTER_LEFT);
        stockSearchControl.getStyleClass().add("asset-search-control");

        assetSelectorPane.getChildren().addAll(symbolBox, stockSearchControl);

        assetSelectorPane.setMinWidth(300);
        assetSelectorPane.setPrefWidth(300);
        assetSelectorPane.setMaxWidth(300);

        symbolBox.managedProperty().bind(symbolBox.visibleProperty());
        stockSearchControl.managedProperty().bind(stockSearchControl.visibleProperty());
        stockSearchControl.visibleProperty().bind(stockSearchField.visibleProperty());

        stockSearchField.setOnMouseClicked(e -> {
            if ("stocks".equals(marketType())) {
                stockSearchField.selectAll();
            }
        });

        stockSearchField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused && "stocks".equals(marketType())) {
                stockSearchField.selectAll();
            }
        });

        updateSelectedAssetVisual(
                "AAPL",
                "https://assets.parqet.com/logos/symbol/AAPL?format=png&size=80"
        );

        refreshMarketSelectors();
    }

    private void refreshMarketSelectors() {
        boolean crypto = "Criptomonedas".equals(marketBox.getValue());

        sourceBox.getItems().clear();
        symbolBox.getItems().clear();

        symbolBox.setVisible(crypto);
        stockSearchField.setVisible(!crypto);

        if (crypto) {
            stockSearchMenu.hide();

            sourceBox.getItems().addAll("binance", "kraken");
            sourceBox.setValue("binance");

            symbolBox.getItems().addAll("BTC/USDT", "ETH/USDT");
            symbolBox.setValue("BTC/USDT");
        } else {
            sourceBox.getItems().add("yahoo");
            sourceBox.setValue("yahoo");

            if (stockSearchField.getText() == null
                    || stockSearchField.getText().isBlank()) {
                stockSearchField.setText("AAPL — Apple Inc.");
                selectedStockSymbol = "AAPL";
                updateSelectedAssetVisual(
                        "AAPL",
                        "https://assets.parqet.com/logos/symbol/AAPL?format=png&size=80"
                );
            }
        }
    }

    private HBox createTopBar() {
        Label brand = new Label("AI TRADER");
        brand.getStyleClass().add("brand");

        Label mode = new Label("ANÁLISIS / BACKTEST");
        mode.getStyleClass().add("paper-badge");

        analyzeButton.getStyleClass().add("primary-button");
        analyzeButton.setOnAction(e -> analyze());

        HBox selectors = new HBox(
                9,
                labeledSelector("Mercado", marketBox),
                labeledSelector("Fuente", sourceBox),
                labeledSelector("Activo", createAssetControl()),
                labeledSelector("Vela", timeframeBox),
                labeledSelector("Período", periodBox)
        );
        selectors.setAlignment(Pos.CENTER_LEFT);
        selectors.getStyleClass().add("top-selectors");

        Region actionSpacer = new Region();
        HBox.setHgrow(actionSpacer, Priority.ALWAYS);

        HBox top = new HBox(
                18,
                brand,
                selectors,
                actionSpacer,
                mode,
                analyzeButton
        );

        top.setPadding(new Insets(10, 16, 10, 16));
        top.setAlignment(Pos.CENTER_LEFT);
        top.getStyleClass().add("top-bar");

        return top;
    }

    private VBox labeledSelector(String title, Node control) {
        VBox box = new VBox(4, muted(title), control);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private VBox createAssetControl() {
        VBox box = new VBox(assetSelectorPane);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void searchStocks(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim();

        if (query.length() < 2) {
            stockSearchMenu.hide();
            return;
        }

        Task<JsonNode> task = new Task<>() {
            @Override
            protected JsonNode call() throws Exception {
                return api.searchSymbols(query);
            }
        };

        task.setOnSucceeded(e -> {
            if (!"stocks".equals(marketType())) {
                return;
            }

            String current = stockSearchField.getText() == null
                    ? ""
                    : stockSearchField.getText().trim();

            if (!current.equals(query)) {
                return;
            }

            JsonNode results = task.getValue().path("results");
            stockSearchMenu.getItems().clear();

            for (JsonNode row : results) {
                AssetResult result = new AssetResult(
                        row.path("symbol").asText(),
                        row.path("name").asText(),
                        row.path("exchange").asText(),
                        row.path("type").asText(),
                        row.path("logo_url").asText()
                );

                CustomMenuItem item = new CustomMenuItem(
                        createAssetResultRow(result),
                        true
                );

                item.setOnAction(event -> selectStock(result));
                stockSearchMenu.getItems().add(item);
            }

            if (stockSearchMenu.getItems().isEmpty()) {
                Label empty = new Label("Sin resultados");
                empty.getStyleClass().add("asset-empty");
                stockSearchMenu.getItems().add(new CustomMenuItem(empty, false));
            }

            if (!stockSearchMenu.isShowing()) {
                stockSearchMenu.show(
                        stockSearchField,
                        javafx.geometry.Side.BOTTOM,
                        0,
                        4
                );
            }
        });

        task.setOnFailed(e -> {
            stockSearchMenu.getItems().clear();

            Label error = new Label("No se pudo buscar en Yahoo");
            error.getStyleClass().add("asset-empty");
            stockSearchMenu.getItems().add(new CustomMenuItem(error, false));

            if (!stockSearchMenu.isShowing()) {
                stockSearchMenu.show(
                        stockSearchField,
                        javafx.geometry.Side.BOTTOM,
                        0,
                        4
                );
            }
        });

        Thread thread = new Thread(task, "stock-symbol-search");
        thread.setDaemon(true);
        thread.start();
    }

    private Node createAssetResultRow(AssetResult result) {
        Label initials = new Label(assetInitials(result));
        initials.getStyleClass().add("asset-logo-fallback");

        StackPane logoHolder = new StackPane(initials);
        logoHolder.getStyleClass().add("asset-logo-holder");
        logoHolder.setMinSize(34, 34);
        logoHolder.setPrefSize(34, 34);
        logoHolder.setMaxSize(34, 34);

        if (result.logoUrl() != null && !result.logoUrl().isBlank()) {
            Image image = new Image(
                    result.logoUrl(),
                    28,
                    28,
                    true,
                    true,
                    true
            );

            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(28);
            imageView.setFitHeight(28);
            imageView.setPreserveRatio(true);
            imageView.getStyleClass().add("asset-logo");

            logoHolder.getChildren().add(imageView);
        }

        Label ticker = new Label(result.symbol());
        ticker.getStyleClass().add("asset-result-symbol");

        String details = result.name();

        if (result.exchange() != null && !result.exchange().isBlank()) {
            details += " · " + result.exchange();
        }

        Label name = new Label(details);
        name.getStyleClass().add("asset-result-name");
        name.setMaxWidth(290);
        name.setTextOverrun(OverrunStyle.ELLIPSIS);

        VBox text = new VBox(2, ticker, name);

        HBox row = new HBox(10, logoHolder, text);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("asset-result-row");
        row.setPrefWidth(355);

        return row;
    }

    private String assetInitials(AssetResult result) {
        String symbol = result.symbol() == null
                ? "?"
                : result.symbol().replaceAll("[^A-Za-z0-9]", "");

        if (symbol.isBlank()) {
            return "?";
        }

        return symbol.substring(0, Math.min(2, symbol.length()))
                .toUpperCase(Locale.ROOT);
    }

    private void selectStock(AssetResult result) {
        stockSearchDelay.stop();
        stockSearchMenu.hide();
        stockSearchField.setText(
                result.symbol() + " — " + result.name()
        );
        selectedStockSymbol = result.symbol();
        updateSelectedAssetVisual(result.symbol(), result.logoUrl());
    }

    private void updateSelectedAssetVisual(
            String symbol,
            String logoUrl
    ) {
        String cleanSymbol = symbol == null
                ? "?"
                : symbol.replaceAll("[^A-Za-z0-9]", "");

        String initials = cleanSymbol.isBlank()
                ? "?"
                : cleanSymbol.substring(0, Math.min(2, cleanSymbol.length()))
                        .toUpperCase(Locale.ROOT);

        selectedAssetInitials.setText(initials);
        selectedAssetImage.setImage(null);

        if (logoUrl == null || logoUrl.isBlank()) {
            return;
        }

        Image image = new Image(
                logoUrl,
                24,
                24,
                true,
                true,
                true
        );

        image.errorProperty().addListener((obs, hadError, hasError) -> {
            if (hasError) {
                selectedAssetImage.setImage(null);
            }
        });

        selectedAssetImage.setImage(image);
    }

    private String selectedSymbol() {
        if ("crypto".equals(marketType())) {
            return symbolBox.getValue();
        }

        if (selectedStockSymbol != null && !selectedStockSymbol.isBlank()) {
            return selectedStockSymbol;
        }

        String typed = stockSearchField.getText() == null
                ? ""
                : stockSearchField.getText().trim();

        int separator = typed.indexOf("—");

        if (separator > 0) {
            typed = typed.substring(0, separator).trim();
        }

        if (typed.isBlank()) {
            return "AAPL";
        }

        return typed.toUpperCase(Locale.ROOT);
    }

    private record AssetResult(
            String symbol,
            String name,
            String exchange,
            String type,
            String logoUrl
    ) {
    }

    private FlowPane createMetrics() {
        FlowPane pane = new FlowPane(10, 10);

        pane.getChildren().addAll(
                metricCard("CAPITAL FINAL", capitalValue),
                metricCard("RETORNO", returnValue),
                metricCard("BUY & HOLD", buyHoldValue),
                metricCard("DRAWDOWN MÁX.", drawdownValue),
                metricCard("WIN RATE", winRateValue),
                metricCard("PROFIT FACTOR", profitFactorValue),
                metricCard("SHARPE", sharpeValue),
                metricCard("OPERACIONES", tradesValue),
                metricCard("GANANCIA MEDIA", avgWinValue),
                metricCard("PÉRDIDA MEDIA", avgLossValue)
        );

        return pane;
    }

    private VBox createAnalysisPanel() {
        Label title = new Label("Análisis técnico");
        title.getStyleClass().add("panel-title");

        Label warning = new Label("NO ES IA TODAVÍA");
        warning.getStyleClass().add("warning-badge");

        lastPrice.getStyleClass().add("large-value");
        signal.getStyleClass().add("signal");

        explanation.setWrapText(true);
        explanation.getStyleClass().add("description");

        status.setWrapText(true);
        status.getStyleClass().add("muted");

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(9);

        grid.addRow(0, muted("Último precio"), lastPrice);
        grid.addRow(1, muted("Tendencia"), trend);
        grid.addRow(2, muted("RSI 14"), rsi);

        VBox box = new VBox(
                12,
                title,
                warning,
                grid,
                new Separator(),
                muted("Señal por reglas"),
                signal,
                explanation,
                new Separator(),
                status
        );

        box.setPadding(new Insets(16));
        box.getStyleClass().add("panel");

        return box;
    }

    private String marketType() {
        return "Criptomonedas".equals(marketBox.getValue())
                ? "crypto"
                : "stocks";
    }

    private void analyze() {
        analyzeButton.setDisable(true);
        analyzeButton.setText("Consultando...");
        status.setText("Descargando histórico real. Puede tardar unos segundos...");

        Task<JsonNode> task = new Task<>() {
            @Override
            protected JsonNode call() throws Exception {
                return api.analyze(
                        marketType(),
                        sourceBox.getValue(),
                        selectedSymbol(),
                        timeframeBox.getValue(),
                        periodBox.getValue()
                );
            }
        };

        task.setOnSucceeded(e -> {
            render(task.getValue());
            analyzeButton.setDisable(false);
            analyzeButton.setText("Analizar mercado");
        });

        task.setOnFailed(e -> {
            status.setText("ERROR: " + task.getException().getMessage());
            analyzeButton.setDisable(false);
            analyzeButton.setText("Analizar mercado");
        });

        Thread thread = new Thread(task, "market-analysis");
        thread.setDaemon(true);
        thread.start();
    }

    private void render(JsonNode root) {
        JsonNode metrics = root.path("metrics");
        JsonNode technical = root.path("technical");

        capitalValue.setText(money(metrics.path("capital_final").asDouble()));
        returnValue.setText(pct(metrics.path("return_pct").asDouble()));
        buyHoldValue.setText(pct(metrics.path("buy_hold_return_pct").asDouble()));
        drawdownValue.setText(pct(metrics.path("max_drawdown_pct").asDouble()));
        winRateValue.setText(pct(metrics.path("win_rate_pct").asDouble()));

        profitFactorValue.setText(nullableNumber(metrics.path("profit_factor")));
        sharpeValue.setText(nullableNumber(metrics.path("sharpe_ratio")));
        tradesValue.setText(String.valueOf(metrics.path("trades").asInt()));
        avgWinValue.setText(nullableMoney(metrics.path("avg_win")));
        avgLossValue.setText(nullableMoney(metrics.path("avg_loss")));

        lastPrice.setText(money(technical.path("last_price").asDouble()));
        trend.setText(technical.path("trend").asText());

        rsi.setText(
                technical.path("rsi14").isNull()
                        ? "—"
                        : String.format("%.2f", technical.path("rsi14").asDouble())
        );

        signal.setText(technical.path("signal").asText());
        explanation.setText(technical.path("explanation").asText());

        fillChart(priceChart, root.path("prices"), "close");
        fillChart(equityChart, root.path("equity"), "equity");
        fillTrades(root.path("trades"));

        status.setText(
                "Datos reales: "
                        + humanMarket(root.path("market_type").asText())
                        + " · "
                        + root.path("source_type").asText()
                        + " · "
                        + root.path("symbol").asText()
                        + " · "
                        + root.path("timeframe").asText()
                        + " · "
                        + root.path("period").asText()
                        + " · "
                        + root.path("candle_count").asInt()
                        + " velas"
                        + " · "
                        + fullDate.format(
                                Instant.ofEpochMilli(root.path("from_timestamp").asLong())
                        )
                        + " → "
                        + fullDate.format(
                                Instant.ofEpochMilli(root.path("to_timestamp").asLong())
                        )
        );
    }

    private String humanMarket(String market) {
        return "stocks".equals(market)
                ? "Acciones/ETF"
                : "Criptomonedas";
    }

    private void fillChart(
            LineChart<String, Number> chart,
            JsonNode array,
            String valueField
    ) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        int total = array.size();
        int step = Math.max(1, total / 180);

        for (int i = 0; i < total; i += step) {
            JsonNode row = array.get(i);
            long ts = row.path("timestamp").asLong();

            series.getData().add(
                    new XYChart.Data<>(
                            chartDate.format(Instant.ofEpochMilli(ts)),
                            row.path(valueField).asDouble()
                    )
            );
        }

        chart.getData().setAll(series);
    }

    private void fillTrades(JsonNode array) {
        tradesTable.getItems().clear();

        for (JsonNode row : array) {
            tradesTable.getItems().add(
                    new TradeRow(
                            fullDate.format(
                                    Instant.ofEpochMilli(row.path("exit_time").asLong())
                            ),
                            row.path("entry_price").asDouble(),
                            row.path("exit_price").asDouble(),
                            row.path("pnl").asDouble(),
                            row.path("reason").asText()
                    )
            );
        }
    }

    private static String money(double value) {
        return String.format("$ %,.2f", value);
    }

    private static String pct(double value) {
        return String.format("%+.2f%%", value);
    }

    private static String nullableNumber(JsonNode node) {
        if (node == null || node.isNull()) {
            return "—";
        }

        return String.format("%.2f", node.asDouble());
    }

    private static String nullableMoney(JsonNode node) {
        if (node == null || node.isNull()) {
            return "—";
        }

        return money(node.asDouble());
    }

    private static Label muted(String value) {
        Label label = new Label(value);
        label.getStyleClass().add("muted");
        return label;
    }

    private static Label metricValue(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("metric-value");
        return label;
    }

    private VBox metricCard(String title, Label value) {
        VBox box = new VBox(5, muted(title), value);
        box.setPadding(new Insets(12));
        box.getStyleClass().add("metric-card");
        box.setPrefWidth(190);
        return box;
    }

    private VBox panel(String titleText, javafx.scene.Node node) {
        Label title = new Label(titleText);
        title.getStyleClass().add("panel-title");

        VBox box = new VBox(8, title, node);
        box.setPadding(new Insets(14));
        box.getStyleClass().add("panel");

        VBox.setVgrow(node, Priority.ALWAYS);

        return box;
    }

    private static LineChart<String, Number> createLineChart(String yLabel) {
        CategoryAxis x = new CategoryAxis();
        x.setTickLabelRotation(-45);

        NumberAxis y = new NumberAxis();
        y.setForceZeroInRange(false);
        y.setLabel(yLabel);

        LineChart<String, Number> chart = new LineChart<>(x, y);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCreateSymbols(false);

        return chart;
    }

    private TableView<TradeRow> createTradesTable() {
        TableView<TradeRow> table = new TableView<>();

        TableColumn<TradeRow, String> time = new TableColumn<>("Salida");
        time.setCellValueFactory(v -> v.getValue().timeProperty());

        TableColumn<TradeRow, Number> entry = new TableColumn<>("Entrada");
        entry.setCellValueFactory(v -> v.getValue().entryProperty());

        TableColumn<TradeRow, Number> exit = new TableColumn<>("Salida $");
        exit.setCellValueFactory(v -> v.getValue().exitProperty());

        TableColumn<TradeRow, Number> pnl = new TableColumn<>("P&L");
        pnl.setCellValueFactory(v -> v.getValue().pnlProperty());

        TableColumn<TradeRow, String> reason = new TableColumn<>("Motivo");
        reason.setCellValueFactory(v -> v.getValue().reasonProperty());

        table.getColumns().addAll(time, entry, exit, pnl, reason);
        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        return table;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
