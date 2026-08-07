package com.aitrader;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class AiTraderApp extends Application {

    private final TradingApiClient api = new TradingApiClient();

    private final ComboBox<String> marketBox = new ComboBox<>();
    private final ComboBox<String> sourceBox = new ComboBox<>();
    private final ComboBox<String> symbolBox = new ComboBox<>();
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
        marketBox.setOnAction(e -> refreshMarketSelectors());

        timeframeBox.getItems().addAll("1h", "4h", "1d");
        timeframeBox.setValue("1h");

        periodBox.getItems().addAll("1m", "3m", "6m", "1y");
        periodBox.setValue("3m");

        marketBox.setPrefWidth(150);
        sourceBox.setPrefWidth(110);
        symbolBox.setPrefWidth(125);
        timeframeBox.setPrefWidth(85);
        periodBox.setPrefWidth(95);

        refreshMarketSelectors();
    }

    private void refreshMarketSelectors() {
        boolean crypto = "Criptomonedas".equals(marketBox.getValue());

        sourceBox.getItems().clear();
        symbolBox.getItems().clear();

        if (crypto) {
            sourceBox.getItems().addAll("binance", "kraken");
            sourceBox.setValue("binance");

            symbolBox.getItems().addAll("BTC/USDT", "ETH/USDT");
            symbolBox.setValue("BTC/USDT");
        } else {
            sourceBox.getItems().add("yahoo");
            sourceBox.setValue("yahoo");

            symbolBox.getItems().addAll(
                    "AAPL",
                    "ASML",
                    "YPF",
                    "SPY",
                    "QQQ"
            );
            symbolBox.setValue("AAPL");
        }
    }

    private HBox createTopBar() {
        Label brand = new Label("AI TRADER");
        brand.getStyleClass().add("brand");

        Label mode = new Label("ANÁLISIS / BACKTEST");
        mode.getStyleClass().add("paper-badge");

        analyzeButton.getStyleClass().add("primary-button");
        analyzeButton.setOnAction(e -> analyze());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox top = new HBox(
                9,
                brand,
                spacer,
                labeledSelector("Mercado", marketBox),
                labeledSelector("Fuente", sourceBox),
                labeledSelector("Activo", symbolBox),
                labeledSelector("Vela", timeframeBox),
                labeledSelector("Período", periodBox),
                mode,
                analyzeButton
        );

        top.setPadding(new Insets(10, 16, 10, 16));
        top.setAlignment(Pos.CENTER_LEFT);
        top.getStyleClass().add("top-bar");

        return top;
    }

    private VBox labeledSelector(String title, ComboBox<String> combo) {
        VBox box = new VBox(4, muted(title), combo);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
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
                        symbolBox.getValue(),
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
