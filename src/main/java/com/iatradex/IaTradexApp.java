package com.iatradex;

import com.iatradex.analysis.AnalysisService;
import com.iatradex.market.MarketService;
import com.iatradex.model.AnalysisResult;
import com.iatradex.model.AssetSearchResult;
import com.iatradex.model.Candle;
import com.iatradex.model.EquityPoint;
import com.iatradex.model.Metrics;
import com.iatradex.model.MarketQuote;
import com.iatradex.model.MarketRegime;
import com.iatradex.model.StrategyPerformance;
import com.iatradex.model.StrategyType;
import com.iatradex.model.TechnicalSnapshot;
import com.iatradex.model.Trade;
import com.iatradex.paper.PaperAccount;
import com.iatradex.paper.PaperAutoConfig;
import com.iatradex.paper.PaperAutoResult;
import com.iatradex.paper.PaperAutoTradingEngine;
import com.iatradex.paper.PaperPortfolioAutoConfig;
import com.iatradex.paper.PaperPortfolioAutoResult;
import com.iatradex.paper.PaperPortfolioAutoEngine;
import com.iatradex.paper.PaperPosition;
import com.iatradex.paper.PaperRefreshResult;
import com.iatradex.paper.PaperTradingService;
import com.iatradex.scanner.ScannerEngine;
import com.iatradex.scanner.ScannerResult;
import com.iatradex.scanner.WatchlistItem;
import com.iatradex.scanner.WatchlistService;
import com.iatradex.ui.ScannerRow;
import com.iatradex.ui.TradeRow;
import com.iatradex.ui.StrategyRow;
import com.iatradex.ui.PaperHistoryRow;
import com.iatradex.ui.PaperAutoLogRow;
import com.iatradex.ui.PaperPositionRow;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
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
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public final class IaTradexApp extends Application {

    private final MarketService marketService = new MarketService();
    private final AnalysisService analysisService = new AnalysisService(marketService);
    private final PaperTradingService paperTradingService = new PaperTradingService();
    private final WatchlistService watchlistService = new WatchlistService();
    private final ScannerEngine scannerEngine = new ScannerEngine(analysisService);
    private final PaperAutoTradingEngine paperAutoTradingEngine = new PaperAutoTradingEngine(analysisService, marketService, paperTradingService);
    private final PaperPortfolioAutoEngine paperPortfolioAutoEngine = new PaperPortfolioAutoEngine(scannerEngine, watchlistService, marketService, paperTradingService);

    private final ComboBox<String> marketBox = new ComboBox<>();
    private final ComboBox<String> sourceBox = new ComboBox<>();
    private final ComboBox<String> cryptoSymbolBox = new ComboBox<>();
    private final ComboBox<String> timeframeBox = new ComboBox<>();
    private final ComboBox<String> periodBox = new ComboBox<>();

    private final TextField stockSearchField = new TextField();
    private final ContextMenu stockSearchMenu = new ContextMenu();
    private final PauseTransition stockSearchDelay = new PauseTransition(Duration.millis(300));
    private final StackPane assetSelectorPane = new StackPane();

    private final StackPane selectedAssetLogo = new StackPane();
    private final Label selectedAssetInitials = new Label("AA");
    private final ImageView selectedAssetImage = new ImageView();

    private String selectedStockSymbol = "AAPL";

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
    private final Label marketChange = new Label("—");
    private final Label bid = new Label("—");
    private final Label ask = new Label("—");
    private final Label marketMode = new Label("—");
    private final Label signal = new Label("Sin análisis");
    private final Label explanation = new Label(
            "Ejecutá el análisis para consultar datos reales."
    );
    private final Label status = new Label("Motor Java: listo");

    private final LineChart<String, Number> priceChart = createLineChart("Precio");
    private final LineChart<String, Number> equityChart = createLineChart("Capital");
    private final TableView<TradeRow> tradesTable = createTradesTable();
    private final TableView<StrategyRow> strategyTable = createStrategyTable();
    private final Label bestStrategyLabel = new Label("Sin comparar");
    private final Label regimeTrendLabel = new Label("Sin analizar");
    private final Label regimeVolatilityLabel = new Label("—");
    private final Label regimeStrengthLabel = new Label("—");
    private final Label regimeCompatibleLabel = new Label("—");
    private final Label regimeExplanationLabel = new Label("Ejecutá el análisis para detectar el régimen.");
    private AnalysisResult currentAnalysis;

    private final Button analyzeButton = new Button("Analizar mercado");
    private final Button paperTradingButton = new Button("Paper Trading");
    private final Button scannerButton = new Button("Scanner");

    private final DateTimeFormatter chartDate =
            DateTimeFormatter.ofPattern("dd/MM")
                    .withZone(ZoneId.systemDefault());

    private final DateTimeFormatter fullDate =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneId.systemDefault());

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");

        configureSelectors();
        MenuBar appMenuBar = createSystemMenuBar();
        VBox topArea = new VBox(appMenuBar, createTopBar());
        root.setTop(topArea);

        VBox page = new VBox(12);
        page.setPadding(new Insets(16));
        page.setFillWidth(true);
        page.getStyleClass().add("dashboard-page");

        FlowPane metrics = createMetrics();

        SplitPane charts = new SplitPane(
                panel("Precio real", priceChart),
                panel("Curva de capital", equityChart)
        );
        charts.setDividerPositions(0.5);
        charts.getStyleClass().add("dashboard-split");
        charts.setMinHeight(300);
        charts.setPrefHeight(320);

        SplitPane bottom = new SplitPane(
                panel("Operaciones del backtest", tradesTable),
                createAnalysisPanel()
        );
        bottom.setDividerPositions(0.70);
        bottom.getStyleClass().add("dashboard-split");
        bottom.setMinHeight(300);
        bottom.setPrefHeight(330);

        VBox regimePanel = createRegimePanel();
        VBox strategyComparison = createStrategyComparisonPanel();

        page.getChildren().addAll(
                metrics,
                regimePanel,
                strategyComparison,
                charts,
                bottom
        );

        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("dashboard-scroll");

        // Mantiene el ancho del contenido sincronizado con el viewport
        // y evita que los SplitPane fuercen scroll horizontal.
        page.prefWidthProperty().bind(
                scrollPane.widthProperty().subtract(18)
        );

        root.setCenter(scrollPane);

        Scene scene = new Scene(root, 1550, 930);
        scene.getStylesheets().add(
                IaTradexApp.class
                        .getResource("/com/iatradex/theme.css")
                        .toExternalForm()
        );

        stage.setTitle("IA-TradeX v0.12.0");

        try {
            stage.getIcons().add(
                    new Image(
                            IaTradexApp.class
                                    .getResourceAsStream("/com/iatradex/icon.png")
                    )
            );
        } catch (Exception ignored) {
            // El icono no debe impedir que la aplicación arranque.
        }

        stage.setMinWidth(760);
        stage.setMinHeight(620);
        stage.setScene(scene);
        stage.show();
    }

    private void configureSelectors() {
        marketBox.getItems().addAll(
                "Criptomonedas",
                "Argentina",
                "Internacional"
        );
        marketBox.setValue("Criptomonedas");
        marketBox.setOnAction(e -> {
            refreshMarketSelectors();
            Platform.runLater(marketBox::hide);
        });

        timeframeBox.getItems().addAll("1h", "4h", "1d");
        timeframeBox.setValue("1h");

        timeframeBox.getStyleClass().add("timeframe-compact");

        timeframeBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
                setTextOverrun(OverrunStyle.CLIP);
                setPadding(new Insets(0, 2, 0, 4));
            }
        });

        timeframeBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
            }
        });

        periodBox.getItems().addAll("1m", "3m", "6m", "1y");
        periodBox.setValue("3m");
        periodBox.getStyleClass().add("period-compact");

        periodBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
                setTextOverrun(OverrunStyle.CLIP);
                setPadding(new Insets(0, 2, 0, 4));
            }
        });

        periodBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
            }
        });

        fixedWidth(marketBox, 125);
        fixedWidth(sourceBox, 98);
        fixedWidth(cryptoSymbolBox, 245);
        fixedWidth(timeframeBox, 78);
        fixedWidth(periodBox, 78);

        stockSearchField.setPromptText("Buscar acción o ticker...");
        stockSearchField.setText("AAPL — Apple Inc.");
        fixedWidth(stockSearchField, 205);
        stockSearchField.getStyleClass().add("asset-search-field");

        stockSearchMenu.getStyleClass().add("asset-search-menu");

        stockSearchDelay.setOnFinished(e -> {
            if (!"crypto".equals(marketType())) {
                searchStocks(stockSearchField.getText());
            }
        });

        stockSearchField.textProperty().addListener((obs, oldValue, newValue) -> {
            if ("crypto".equals(marketType())) {
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

        stockSearchField.setOnMouseClicked(e -> {
            if (!"crypto".equals(marketType())) {
                stockSearchField.selectAll();
            }
        });

        stockSearchField.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (focused && !"crypto".equals(marketType())) {
                Platform.runLater(stockSearchField::selectAll);
            }
        });

        stockSearchField.setOnAction(e -> {
            stockSearchMenu.hide();
            analyze();
        });

        selectedAssetInitials.getStyleClass().add("selected-asset-initials");

        selectedAssetImage.setFitWidth(24);
        selectedAssetImage.setFitHeight(24);
        selectedAssetImage.setPreserveRatio(true);

        selectedAssetLogo.getChildren().addAll(
                selectedAssetInitials,
                selectedAssetImage
        );
        selectedAssetLogo.getStyleClass().add("selected-asset-logo");
        fixedSize(selectedAssetLogo, 30, 30);

        HBox stockSearchControl = new HBox(
                8,
                selectedAssetLogo,
                stockSearchField
        );
        stockSearchControl.setAlignment(Pos.CENTER_LEFT);
        stockSearchControl.getStyleClass().add("asset-search-control");
        fixedWidth(stockSearchControl, 245);

        assetSelectorPane.getChildren().addAll(
                cryptoSymbolBox,
                stockSearchControl
        );
        fixedWidth(assetSelectorPane, 245);

        cryptoSymbolBox.managedProperty().bind(cryptoSymbolBox.visibleProperty());
        stockSearchControl.managedProperty().bind(stockSearchControl.visibleProperty());

        updateSelectedAssetVisual(
                "AAPL",
                "https://assets.parqet.com/logos/symbol/AAPL?format=png&size=80"
        );

        refreshMarketSelectors();
    }

    private void refreshMarketSelectors() {
        boolean crypto = "Criptomonedas".equals(marketBox.getValue());
        boolean argentina = "Argentina".equals(marketBox.getValue());

        sourceBox.getItems().clear();
        cryptoSymbolBox.getItems().clear();

        cryptoSymbolBox.setVisible(crypto);

        Node stockControl = assetSelectorPane.getChildren().size() > 1
                ? assetSelectorPane.getChildren().get(1)
                : null;

        if (stockControl != null) {
            stockControl.setVisible(!crypto);
        }

        timeframeBox.getItems().clear();

        if (argentina) {
            timeframeBox.getItems().add("1d");
            timeframeBox.setValue("1d");
        } else {
            timeframeBox.getItems().addAll("1h", "4h", "1d");
            timeframeBox.setValue("1h");
        }

        if (crypto) {
            stockSearchMenu.hide();

            sourceBox.getItems().addAll("binance", "kraken");
            sourceBox.setValue("binance");

            cryptoSymbolBox.getItems().addAll(
                    "BTC/USDT",
                    "ETH/USDT"
            );
            cryptoSymbolBox.setValue("BTC/USDT");
            return;
        }

        if (argentina) {
            sourceBox.getItems().add("Open BYMADATA");
            sourceBox.setValue("Open BYMADATA");

            selectedStockSymbol = null;
            stockSearchField.clear();
            stockSearchField.setPromptText(
                    "Buscar CEDEAR o acción BYMA..."
            );
            updateSelectedAssetVisual("", "");
            return;
        }

        sourceBox.getItems().add("yahoo · global");
        sourceBox.setValue("yahoo · global");

        selectedStockSymbol = "AAPL";
        stockSearchField.setPromptText("Buscar acción o ticker...");
        stockSearchField.setText(
                "AAPL — Apple Inc. · Internacional"
        );

        updateSelectedAssetVisual(
                "AAPL",
                "https://assets.parqet.com/logos/symbol/AAPL?format=png&size=80"
        );
    }

    private MenuBar createSystemMenuBar() {
        Menu appMenu = new Menu("IA-TradeX");

        MenuItem aboutItem = new MenuItem(
                "Acerca de IA-TradeX"
        );
        aboutItem.setOnAction(e -> showAbout());

        SeparatorMenuItem separator =
                new SeparatorMenuItem();

        MenuItem quitItem = new MenuItem(
                "Salir de IA-TradeX"
        );
        quitItem.setOnAction(e -> Platform.exit());

        appMenu.getItems().addAll(
                aboutItem,
                separator,
                quitItem
        );

        MenuBar menuBar = new MenuBar(appMenu);

        // En macOS JavaFX integra este MenuBar en la barra superior
        // del sistema, junto al menú de la aplicación.
        menuBar.setUseSystemMenuBar(true);
        menuBar.getStyleClass().add("system-menu-bar");

        return menuBar;
    }

    private Node createTopBar() {
        ImageView brandIcon = new ImageView();

        try {
            Image sourceIcon = new Image(
                    IaTradexApp.class.getResourceAsStream(
                            "/com/iatradex/icon.png"
                    )
            );

            brandIcon.setImage(sourceIcon);
        } catch (Exception ignored) {
        }

        brandIcon.setFitWidth(40);
        brandIcon.setFitHeight(40);
        brandIcon.setPreserveRatio(true);
        brandIcon.setSmooth(true);

        StackPane brandHolder = new StackPane(brandIcon);
        brandHolder.getStyleClass().add("brand-icon-holder");
        brandHolder.setMinSize(46, 46);
        brandHolder.setPrefSize(46, 46);
        brandHolder.setMaxSize(46, 46);

        Label mode = new Label(
                "100% JAVA · ANÁLISIS / BACKTEST"
        );
        mode.getStyleClass().add("top-mode-label");

        HBox modeStrip = new HBox(mode);
        modeStrip.setAlignment(Pos.CENTER_RIGHT);
        modeStrip.setPadding(new Insets(3, 14, 3, 14));
        modeStrip.getStyleClass().add("top-mode-strip");

        analyzeButton.getStyleClass().add("primary-button");
        analyzeButton.setOnAction(e -> analyze());

        paperTradingButton.getStyleClass().add("secondary-button");
        paperTradingButton.setOnAction(e -> showPaperTrading());

        scannerButton.getStyleClass().add("secondary-button");
        scannerButton.setOnAction(e -> showScanner());

        matchTopControlHeight(
                analyzeButton,
                scannerButton,
                paperTradingButton
        );

        VBox marketSelector = labeledSelector(
                "Mercado",
                marketBox
        );
        VBox sourceSelector = labeledSelector(
                "Fuente",
                sourceBox
        );
        VBox assetSelector = labeledSelector(
                "Activo",
                assetSelectorPane
        );
        VBox timeframeSelector = labeledSelector(
                "Vela",
                timeframeBox
        );
        VBox periodSelector = labeledSelector(
                "Período",
                periodBox
        );

        HBox controls = new HBox(
                9,
                brandHolder,
                marketSelector,
                sourceSelector,
                assetSelector,
                timeframeSelector,
                periodSelector,
                analyzeButton,
                scannerButton,
                paperTradingButton
        );

        controls.setPadding(new Insets(6, 14, 7, 14));
        controls.setAlignment(Pos.BOTTOM_LEFT);
        controls.getStyleClass().addAll(
                "top-bar",
                "single-row-top-bar"
        );

        // Los selectores y acciones permanecen SIEMPRE en una fila.
        // Si una ventana excepcionalmente angosta no alcanza, se permite
        // desplazamiento horizontal en lugar de recortar controles.
        ScrollPane controlsScroll = new ScrollPane(controls);
        controlsScroll.setFitToHeight(true);
        controlsScroll.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.AS_NEEDED
        );
        controlsScroll.setVbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );
        controlsScroll.setPannable(true);
        controlsScroll.getStyleClass().add("top-bar-scroll");

        VBox top = new VBox(
                0,
                modeStrip,
                controlsScroll
        );
        top.getStyleClass().add("top-header");

        return top;
    }


    private VBox labeledSelector(String title, Node control) {
        VBox box = new VBox(4, muted(title), control);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void searchStocks(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim();

        if (query.contains("—")) {
            query = query.substring(0, query.indexOf("—")).trim();
        }

        if (query.length() < 2) {
            stockSearchMenu.hide();
            return;
        }

        final String finalQuery = query;
        final boolean argentinaSearch = argentinaMarket();

        Task<List<AssetSearchResult>> task = new Task<>() {
            @Override
            protected List<AssetSearchResult> call() throws Exception {
                return marketService.searchStocks(
                        finalQuery,
                        argentinaSearch
                );
            }
        };

        task.setOnSucceeded(e -> {
            // El autocomplete aplica tanto a Argentina como Internacional.
            // Solo se ignora si el usuario cambió a Criptomonedas
            // mientras la búsqueda estaba en curso.
            if ("crypto".equals(marketType())) {
                return;
            }

            String current = stockSearchField.getText() == null
                    ? ""
                    : stockSearchField.getText().trim();

            if (current.contains("—")) {
                current = current.substring(0, current.indexOf("—")).trim();
            }

            // Evita mostrar resultados de una búsqueda anterior cuando
            // el usuario siguió escribiendo.
            if (!current.equalsIgnoreCase(finalQuery)) {
                return;
            }

            stockSearchMenu.getItems().clear();

            for (AssetSearchResult result : task.getValue()) {
                CustomMenuItem item = new CustomMenuItem(
                        createAssetResultRow(result),
                        true
                );

                item.setOnAction(event -> selectStock(result));
                stockSearchMenu.getItems().add(item);
            }

            if (stockSearchMenu.getItems().isEmpty()) {
                Label empty = new Label(
                        argentinaSearch
                                ? "Sin resultados en Argentina"
                                : "Sin resultados"
                );
                empty.getStyleClass().add("asset-empty");
                stockSearchMenu.getItems().add(
                        new CustomMenuItem(empty, false)
                );
            }

            stockSearchMenu.show(
                    stockSearchField,
                    Side.BOTTOM,
                    -38,
                    5
            );
        });

        task.setOnFailed(e -> {
            if ("crypto".equals(marketType())) {
                return;
            }

            stockSearchMenu.getItems().clear();

            Throwable cause = task.getException();

            Label error = new Label(
                    argentinaSearch
                            ? "Error Open BYMADATA: "
                                    + (cause == null
                                    ? "consulta fallida"
                                    : cause.getMessage())
                            : "No se pudo consultar Yahoo"
            );

            error.setWrapText(true);
            error.setMaxWidth(330);
            error.getStyleClass().add("asset-empty");

            stockSearchMenu.getItems().add(
                    new CustomMenuItem(error, false)
            );

            stockSearchMenu.show(
                    stockSearchField,
                    Side.BOTTOM,
                    -38,
                    5
            );
        });

        Thread thread = new Thread(task, "stock-search");
        thread.setDaemon(true);
        thread.start();
    }

    private Node createAssetResultRow(AssetSearchResult result) {
        Label initials = new Label(assetInitials(result.symbol()));
        initials.getStyleClass().add("asset-logo-fallback");

        StackPane logoHolder = new StackPane(initials);
        logoHolder.getStyleClass().add("asset-logo-holder");
        fixedSize(logoHolder, 34, 34);

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
            logoHolder.getChildren().add(imageView);
        }

        Label ticker = new Label(result.symbol());
        ticker.getStyleClass().add("asset-result-symbol");

        String detail = result.name();

        if (result.exchange() != null && !result.exchange().isBlank()) {
            detail += " · " + result.exchange();
        }

        if (argentinaMarket()) {
            detail += " · ARS";
        }

        Label name = new Label(detail);
        name.getStyleClass().add("asset-result-name");
        name.setMaxWidth(310);
        name.setTextOverrun(OverrunStyle.ELLIPSIS);

        VBox text = new VBox(2, ticker, name);

        HBox row = new HBox(10, logoHolder, text);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("asset-result-row");
        row.setPrefWidth(370);

        return row;
    }

    private void selectStock(AssetSearchResult result) {
        stockSearchDelay.stop();
        stockSearchMenu.hide();

        stockSearchField.setText(
                result.symbol() + " — " + result.name()
        );
        selectedStockSymbol = result.symbol();

        updateSelectedAssetVisual(
                result.symbol(),
                result.logoUrl()
        );
    }

    private void updateSelectedAssetVisual(String symbol, String logoUrl) {
        selectedAssetInitials.setText(assetInitials(symbol));
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

        image.errorProperty().addListener((obs, oldValue, hasError) -> {
            if (hasError) {
                selectedAssetImage.setImage(null);
            }
        });

        selectedAssetImage.setImage(image);
    }

    private String assetInitials(String symbol) {
        String clean = symbol == null
                ? "?"
                : symbol.replaceAll("[^A-Za-z0-9]", "");

        if (clean.isBlank()) {
            return "?";
        }

        return clean.substring(0, Math.min(2, clean.length()))
                .toUpperCase(Locale.ROOT);
    }

    private String marketType() {
        if ("Criptomonedas".equals(marketBox.getValue())) {
            return "crypto";
        }

        if ("Argentina".equals(marketBox.getValue())) {
            return "argentina";
        }

        return "stocks";
    }

    private boolean argentinaMarket() {
        return "Argentina".equals(marketBox.getValue());
    }

    private String currency() {
        if (argentinaMarket()) {
            return "ARS";
        }

        if ("crypto".equals(marketType())) {
            return "USD";
        }

        return "USD";
    }

    private String dataSource() {
        if ("crypto".equals(marketType())) {
            return sourceBox.getValue();
        }

        if (argentinaMarket()) {
            return "open-bymadata";
        }

        return "yahoo";
    }


    private String selectedSymbol() {
        if ("crypto".equals(marketType())) {
            return cryptoSymbolBox.getValue();
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
            if (argentinaMarket()) {
                throw new IllegalStateException(
                        "Seleccioná primero un CEDEAR o acción BYMA."
                );
            }

            return "AAPL";
        }

        return typed.toUpperCase(Locale.ROOT);
    }


    private void showScanner() {
        Stage window = new Stage();
        window.initModality(Modality.NONE);
        window.setTitle("IA-TradeX · Scanner");

        Label title = new Label("Watchlist + Scanner");
        title.getStyleClass().add("paper-title");

        Label subtitle = new Label(
                "Ranking técnico explicable · no representa probabilidad de ganancia"
        );
        subtitle.getStyleClass().add("paper-subtitle");

        ComboBox<String> marketFilter = new ComboBox<>();
        marketFilter.getItems().addAll(
                "Argentina",
                "Internacional",
                "Criptomonedas"
        );
        marketFilter.setValue(
                switch (marketType()) {
                    case "argentina" -> "Argentina";
                    case "stocks" -> "Internacional";
                    default -> "Criptomonedas";
                }
        );
        fixedWidth(marketFilter, 150);

        ComboBox<String> sourceFilter = new ComboBox<>();
        fixedWidth(sourceFilter, 120);

        ComboBox<String> timeframeFilter = new ComboBox<>();
        fixedWidth(timeframeFilter, 85);

        ComboBox<String> periodFilter = new ComboBox<>();
        periodFilter.getItems().addAll("1m", "3m", "6m", "1y");
        periodFilter.setValue("3m");
        fixedWidth(periodFilter, 85);

        TextField symbolField = new TextField();
        symbolField.setPromptText("Ticker / símbolo");
        fixedWidth(symbolField, 155);

        Button addButton = new Button("Agregar");
        addButton.getStyleClass().add("secondary-button");

        Button removeButton = new Button("Quitar");
        removeButton.getStyleClass().add("secondary-button");

        Button scanButton = new Button("Escanear");
        scanButton.getStyleClass().add("primary-button");

        CheckBox autoScan = new CheckBox("Auto 60 s");
        autoScan.getStyleClass().add("scanner-auto-check");

        Button analyzeSelected = new Button("Abrir en análisis");
        analyzeSelected.getStyleClass().add("secondary-button");

        Button sendAuto = new Button("Enviar a Paper AUTO");
        sendAuto.getStyleClass().add("secondary-button");

        Label scanStatus = new Label("Listo para escanear.");
        scanStatus.getStyleClass().add("scanner-status");

        Label scoreHelp = new Label(
                "Score: 0–100 según tendencia, fuerza, RSI, compatibilidad de estrategia, "
                        + "señal actual, retorno histórico y volatilidad. No es una probabilidad."
        );
        scoreHelp.setWrapText(true);
        scoreHelp.getStyleClass().add("scanner-score-help");

        ListView<WatchlistItem> watchlist = new ListView<>();
        watchlist.getStyleClass().add("scanner-watchlist");
        watchlist.setPrefWidth(245);
        watchlist.setMinWidth(220);
        watchlist.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(
                    WatchlistItem item,
                    boolean empty
            ) {
                super.updateItem(item, empty);
                setText(
                        empty || item == null
                                ? null
                                : item.symbol()
                                + " · "
                                + item.timeframe()
                                + " · "
                                + item.source()
                );
            }
        });

        TableView<ScannerRow> table = createScannerTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        Runnable configureMarketControls = () -> {
            String market = scannerMarketType(
                    marketFilter.getValue()
            );

            sourceFilter.getItems().clear();
            timeframeFilter.getItems().clear();

            if ("argentina".equals(market)) {
                sourceFilter.getItems().add("open-bymadata");
                sourceFilter.setValue("open-bymadata");
                timeframeFilter.getItems().add("1d");
                timeframeFilter.setValue("1d");
            } else if ("crypto".equals(market)) {
                sourceFilter.getItems().addAll(
                        "binance",
                        "kraken"
                );
                sourceFilter.setValue("binance");
                timeframeFilter.getItems().addAll(
                        "1h",
                        "4h",
                        "1d"
                );
                timeframeFilter.setValue("1h");
            } else {
                sourceFilter.getItems().add("yahoo");
                sourceFilter.setValue("yahoo");
                timeframeFilter.getItems().addAll(
                        "1h",
                        "4h",
                        "1d"
                );
                timeframeFilter.setValue("1h");
            }
        };

        Runnable refreshWatchlist = () -> {
            String market = scannerMarketType(
                    marketFilter.getValue()
            );

            watchlist.setItems(
                    FXCollections.observableArrayList(
                            watchlistService.itemsForMarket(market)
                    )
            );
        };

        marketFilter.setOnAction(e -> {
            configureMarketControls.run();
            refreshWatchlist.run();
            table.getItems().clear();
        });

        configureMarketControls.run();
        refreshWatchlist.run();

        addButton.setOnAction(e -> {
            String symbol = symbolField.getText() == null
                    ? ""
                    : symbolField.getText().trim().toUpperCase(Locale.ROOT);

            if (symbol.isBlank()) {
                showScannerError("Ingresá un ticker o símbolo.");
                return;
            }

            String market = scannerMarketType(
                    marketFilter.getValue()
            );

            if ("crypto".equals(market)
                    && !symbol.contains("/")) {
                if (symbol.endsWith("USDT")) {
                    symbol = symbol.substring(
                            0,
                            symbol.length() - 4
                    ) + "/USDT";
                }
            }

            try {
                watchlistService.add(
                        new WatchlistItem(
                                market,
                                sourceFilter.getValue(),
                                symbol,
                                "argentina".equals(market)
                                        ? "ARS"
                                        : "USD",
                                timeframeFilter.getValue(),
                                periodFilter.getValue()
                        )
                );

                symbolField.clear();
                refreshWatchlist.run();
            } catch (Exception ex) {
                showScannerError(ex.getMessage());
            }
        });

        removeButton.setOnAction(e -> {
            WatchlistItem selected =
                    watchlist.getSelectionModel().getSelectedItem();

            if (selected == null) {
                showScannerError(
                        "Seleccioná un activo de la watchlist."
                );
                return;
            }

            try {
                watchlistService.remove(selected);
                refreshWatchlist.run();
                table.getItems().removeIf(
                        row -> row.result().item().key()
                                .equalsIgnoreCase(selected.key())
                );
            } catch (Exception ex) {
                showScannerError(ex.getMessage());
            }
        });

        AtomicBoolean scanning = new AtomicBoolean(false);

        Runnable scan = () -> {
            if (!scanning.compareAndSet(false, true)) {
                return;
            }

            List<WatchlistItem> items = new ArrayList<>(
                    watchlist.getItems()
            );

            if (items.isEmpty()) {
                scanning.set(false);
                showScannerError(
                        "La watchlist está vacía."
                );
                return;
            }

            scanButton.setDisable(true);
            table.getItems().clear();
            scanStatus.setText(
                    "Escaneando 0/" + items.size() + "..."
            );

            Task<List<ScannerResult>> task = new Task<>() {
                @Override
                protected List<ScannerResult> call() {
                    List<ScannerResult> results =
                            new ArrayList<>();

                    for (int i = 0; i < items.size(); i++) {
                        WatchlistItem item = items.get(i);
                        results.add(scannerEngine.scan(item));

                        updateMessage(
                                "Escaneando "
                                        + (i + 1)
                                        + "/"
                                        + items.size()
                                        + " · "
                                        + item.symbol()
                        );
                    }

                    results.sort(
                            Comparator.comparingInt(
                                    ScannerResult::score
                            ).reversed()
                    );

                    return results;
                }
            };

            scanStatus.textProperty().bind(
                    task.messageProperty()
            );

            task.setOnSucceeded(e -> {
                scanStatus.textProperty().unbind();

                List<ScannerResult> results = task.getValue();

                table.setItems(
                        FXCollections.observableArrayList(
                                results.stream()
                                        .map(ScannerRow::new)
                                        .toList()
                        )
                );

                long errors = results.stream()
                        .filter(result -> !result.successful())
                        .count();

                scanStatus.setText(
                        "Escaneo finalizado · "
                                + results.size()
                                + " activos"
                                + (
                                errors > 0
                                        ? " · " + errors + " con error"
                                        : ""
                        )
                );

                scanButton.setDisable(false);
                scanning.set(false);
            });

            task.setOnFailed(e -> {
                scanStatus.textProperty().unbind();
                Throwable error = task.getException();

                scanStatus.setText(
                        "Error de scanner: "
                                + (
                                error == null
                                        ? "desconocido"
                                        : error.getMessage()
                        )
                );

                scanButton.setDisable(false);
                scanning.set(false);
            });

            Thread thread = new Thread(
                    task,
                    "market-scanner"
            );
            thread.setDaemon(true);
            thread.start();
        };

        scanButton.setOnAction(e -> scan.run());

        Runnable openSelected = () -> {
            ScannerRow row =
                    table.getSelectionModel().getSelectedItem();

            if (row == null || !row.result().successful()) {
                showScannerError(
                        "Seleccioná un resultado válido."
                );
                return;
            }

            applyScannerResult(row.result());
            window.close();
        };

        analyzeSelected.setOnAction(e -> openSelected.run());

        table.setRowFactory(tv -> {
            TableRow<ScannerRow> row = new TableRow<>();

            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2
                        && !row.isEmpty()) {
                    table.getSelectionModel().select(
                            row.getItem()
                    );
                    openSelected.run();
                }
            });

            return row;
        });

        sendAuto.setOnAction(e -> {
            ScannerRow row =
                    table.getSelectionModel().getSelectedItem();

            if (row == null || !row.result().successful()) {
                showScannerError(
                        "Seleccioná un resultado válido."
                );
                return;
            }

            ScannerResult result = row.result();

            applyScannerResult(result);

            try {
                PaperAutoConfig previous =
                        paperTradingService.autoConfig();

                double maxCapital =
                        "ARS".equalsIgnoreCase(
                                result.item().currency()
                        )
                                ? 200_000.0
                                : 2_000.0;

                double risk = previous == null
                        ? 1.0
                        : previous.riskPct();
                double stop = previous == null
                        ? 2.0
                        : previous.stopLossPct();
                double take = previous == null
                        ? 4.0
                        : previous.takeProfitPct();

                paperTradingService.setAutoConfig(
                        new PaperAutoConfig(
                                false,
                                result.item().symbol(),
                                result.item().marketType(),
                                result.item().source(),
                                result.item().currency(),
                                result.item().timeframe(),
                                result.item().period(),
                                result.strategy().name(),
                                maxCapital,
                                risk,
                                stop,
                                take
                        )
                );

                window.close();
                showPaperTrading();
            } catch (Exception ex) {
                showScannerError(ex.getMessage());
            }
        });

        FlowPane controls = new FlowPane(
                8,
                8,
                labeledSelector("Mercado", marketFilter),
                labeledSelector("Fuente", sourceFilter),
                labeledSelector("Vela", timeframeFilter),
                labeledSelector("Período", periodFilter),
                labeledSelector("Nuevo activo", symbolField),
                addButton,
                removeButton,
                scanButton,
                autoScan
        );
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.getStyleClass().add("scanner-controls");

        VBox left = new VBox(
                8,
                new Label("Watchlist"),
                watchlist
        );
        left.getStyleClass().add("scanner-side");
        VBox.setVgrow(watchlist, Priority.ALWAYS);

        HBox resultActions = new HBox(
                10,
                analyzeSelected,
                sendAuto
        );
        resultActions.setAlignment(Pos.CENTER_RIGHT);

        VBox right = new VBox(
                8,
                scanStatus,
                table,
                scoreHelp,
                resultActions
        );
        HBox.setHgrow(right, Priority.ALWAYS);
        VBox.setVgrow(table, Priority.ALWAYS);

        HBox body = new HBox(
                12,
                left,
                right
        );
        VBox.setVgrow(body, Priority.ALWAYS);

        VBox root = new VBox(
                12,
                title,
                subtitle,
                controls,
                body
        );
        root.setPadding(new Insets(16));
        root.getStyleClass().add("scanner-root");

        Scene scene = new Scene(root, 1380, 760);
        scene.getStylesheets().add(
                IaTradexApp.class
                        .getResource("/com/iatradex/theme.css")
                        .toExternalForm()
        );

        window.setScene(scene);
        window.setMinWidth(900);
        window.setMinHeight(620);

        try {
            window.getIcons().add(
                    new Image(
                            IaTradexApp.class.getResourceAsStream(
                                    "/com/iatradex/icon.png"
                            )
                    )
            );
        } catch (Exception ignored) {
        }

        Timeline scannerAutoRefresh = new Timeline(
                new KeyFrame(
                        Duration.seconds(60),
                        e -> {
                            if (autoScan.isSelected()) {
                                scan.run();
                            }
                        }
                )
        );
        scannerAutoRefresh.setCycleCount(Timeline.INDEFINITE);

        autoScan.selectedProperty().addListener(
                (obs, oldValue, enabled) -> {
                    if (enabled) {
                        scan.run();
                        scannerAutoRefresh.play();
                        scanStatus.setText(
                                "Scanner automático activo · cada 60 s"
                        );
                    } else {
                        scannerAutoRefresh.stop();
                        scanStatus.setText(
                                "Scanner automático pausado."
                        );
                    }
                }
        );

        window.setOnHidden(e -> scannerAutoRefresh.stop());

        window.show();
    }

    private TableView<ScannerRow> createScannerTable() {
        TableView<ScannerRow> table = new TableView<>();
        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );
        table.getStyleClass().add("scanner-table");

        TableColumn<ScannerRow, String> symbol =
                new TableColumn<>("Activo");
        symbol.setCellValueFactory(v ->
                v.getValue().symbolProperty()
        );

        TableColumn<ScannerRow, String> market =
                new TableColumn<>("Mercado");
        market.setCellValueFactory(v ->
                v.getValue().marketProperty()
        );

        TableColumn<ScannerRow, String> regime =
                new TableColumn<>("Régimen");
        regime.setCellValueFactory(v ->
                v.getValue().regimeProperty()
        );

        TableColumn<ScannerRow, String> volatility =
                new TableColumn<>("Volatilidad");
        volatility.setCellValueFactory(v ->
                v.getValue().volatilityProperty()
        );

        TableColumn<ScannerRow, String> rsi =
                new TableColumn<>("RSI");
        rsi.setCellValueFactory(v ->
                v.getValue().rsiProperty()
        );

        TableColumn<ScannerRow, String> strategy =
                new TableColumn<>("Estrategia");
        strategy.setCellValueFactory(v ->
                v.getValue().strategyProperty()
        );

        TableColumn<ScannerRow, String> signal =
                new TableColumn<>("Señal");
        signal.setCellValueFactory(v ->
                v.getValue().signalProperty()
        );

        TableColumn<ScannerRow, String> score =
                new TableColumn<>("Score");
        score.setCellValueFactory(v ->
                v.getValue().scoreProperty()
        );

        TableColumn<ScannerRow, String> detail =
                new TableColumn<>("Explicación");
        detail.setCellValueFactory(v ->
                v.getValue().detailProperty()
        );

        symbol.setPrefWidth(90);
        market.setPrefWidth(100);
        regime.setPrefWidth(90);
        volatility.setPrefWidth(90);
        rsi.setPrefWidth(70);
        strategy.setPrefWidth(130);
        signal.setPrefWidth(90);
        score.setPrefWidth(70);
        detail.setPrefWidth(470);

        table.getColumns().addAll(
                symbol,
                market,
                regime,
                volatility,
                rsi,
                strategy,
                signal,
                score,
                detail
        );

        return table;
    }

    private String scannerMarketType(String label) {
        if ("Argentina".equals(label)) {
            return "argentina";
        }

        if ("Criptomonedas".equals(label)) {
            return "crypto";
        }

        return "stocks";
    }

    private void applyScannerResult(ScannerResult result) {
        WatchlistItem item = result.item();

        if ("argentina".equals(item.marketType())) {
            marketBox.setValue("Argentina");
        } else if ("crypto".equals(item.marketType())) {
            marketBox.setValue("Criptomonedas");
        } else {
            marketBox.setValue("Internacional");
        }

        refreshMarketSelectors();

        timeframeBox.setValue(item.timeframe());
        periodBox.setValue(item.period());

        if ("crypto".equals(item.marketType())) {
            sourceBox.setValue(item.source());

            if (!cryptoSymbolBox.getItems().contains(
                    item.symbol()
            )) {
                cryptoSymbolBox.getItems().add(
                        item.symbol()
                );
            }

            cryptoSymbolBox.setValue(item.symbol());
        } else {
            selectedStockSymbol = item.symbol();
            stockSearchField.setText(item.symbol());

            updateSelectedAssetVisual(
                    item.symbol(),
                    "stocks".equals(item.marketType())
                            ? "https://assets.parqet.com/logos/symbol/"
                            + item.symbol()
                            + "?format=png&size=80"
                            : null
            );
        }

        render(result.analysis());
    }

    private void showScannerError(String message) {
        Alert alert = new Alert(
                Alert.AlertType.ERROR,
                message == null
                        ? "Error de Scanner."
                        : message,
                ButtonType.OK
        );
        alert.setTitle("Scanner");
        alert.setHeaderText(
                "No se pudo completar la operación"
        );
        alert.showAndWait();
    }


    private void showAbout() {
        Stage dialog = new Stage(StageStyle.UNDECORATED);
        dialog.initModality(Modality.APPLICATION_MODAL);

        Label title = new Label("IA-TradeX");
        title.getStyleClass().add("about-title");

        Label subtitle = new Label(
                "Análisis de mercados y backtesting · 100% Java · v0.12.0"
        );
        subtitle.getStyleClass().add("about-subtitle");

        Label author = new Label("Juan Manuel De Castro");
        author.getStyleClass().add("about-author");

        Hyperlink email = new Hyperlink("jm@pronexo.com");
        email.setOnAction(e ->
                getHostServices().showDocument("mailto:jm@pronexo.com")
        );

        Hyperlink website = new Hyperlink("www.pronexo.com");
        website.setOnAction(e ->
                getHostServices().showDocument("https://www.pronexo.com")
        );

        Label license = new Label(
                "GNU Affero General Public License v3.0 (AGPL-3.0)"
        );
        license.setWrapText(true);
        license.getStyleClass().add("about-license");

        ImageView icon = new ImageView();
        try {
            icon.setImage(
                    new Image(
                            IaTradexApp.class.getResourceAsStream(
                                    "/com/iatradex/icon.png"
                            )
                    )
            );
        } catch (Exception ignored) {
        }
        icon.setFitWidth(68);
        icon.setFitHeight(68);
        icon.setPreserveRatio(true);

        VBox identity = new VBox(
                4,
                title,
                subtitle
        );
        identity.setAlignment(Pos.CENTER_LEFT);

        HBox header = new HBox(
                16,
                icon,
                identity
        );
        header.setAlignment(Pos.CENTER_LEFT);

        VBox details = new VBox(
                7,
                author,
                email,
                website
        );

        Label licenseCaption = new Label("Licencia");
        licenseCaption.getStyleClass().add("about-caption");

        VBox licenseBox = new VBox(
                4,
                licenseCaption,
                license
        );

        Button close = new Button("Cerrar");
        close.getStyleClass().add("primary-button");
        close.setOnAction(e -> dialog.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox footer = new HBox(
                10,
                spacer,
                close
        );

        VBox card = new VBox(
                20,
                header,
                new Separator(),
                details,
                licenseBox,
                footer
        );
        card.setPadding(new Insets(24));
        card.getStyleClass().add("about-card");

        Scene scene = new Scene(card, 520, 365);
        scene.getStylesheets().add(
                IaTradexApp.class
                        .getResource("/com/iatradex/theme.css")
                        .toExternalForm()
        );

        dialog.setScene(scene);
        dialog.setTitle("Acerca de IA-TradeX");
        dialog.setResizable(false);

        try {
            dialog.getIcons().add(
                    new Image(
                            IaTradexApp.class.getResourceAsStream(
                                    "/com/iatradex/icon.png"
                            )
                    )
            );
        } catch (Exception ignored) {
        }

        dialog.showAndWait();
    }

    private void showPaperTrading() {
        Stage window = new Stage();
        window.initModality(Modality.NONE);
        window.setTitle("IA-TradeX · Paper Trading");

        Label title = new Label("Paper Trading");
        title.getStyleClass().add("paper-title");

        Label subtitle = new Label(
                "Cuenta simulada · no opera dinero real"
        );
        subtitle.getStyleClass().add("paper-subtitle");

        VBox heading = new VBox(2, title, subtitle);

        ToggleGroup currencyGroup = new ToggleGroup();

        ToggleButton arsButton = new ToggleButton("ARS");
        ToggleButton usdButton = new ToggleButton("USD");

        arsButton.setToggleGroup(currencyGroup);
        usdButton.setToggleGroup(currencyGroup);

        arsButton.getStyleClass().add("paper-currency-toggle");
        usdButton.getStyleClass().add("paper-currency-toggle");

        HBox currencySelector = new HBox(4, arsButton, usdButton);
        currencySelector.getStyleClass().add("paper-currency-selector");

        String initialCurrency = currentAnalysis != null
                ? currentAnalysis.currency()
                : "ARS";

        if ("USD".equalsIgnoreCase(initialCurrency)) {
            usdButton.setSelected(true);
        } else {
            arsButton.setSelected(true);
        }

        Label accountCurrencyLabel = new Label("Cuenta");
        accountCurrencyLabel.getStyleClass().add("paper-account-caption");

        VBox accountSelectorBox = new VBox(
                5,
                accountCurrencyLabel,
                currencySelector
        );
        accountSelectorBox.setAlignment(Pos.CENTER_LEFT);

        Label initialCapital = new Label();
        Label cash = new Label();
        Label equity = new Label();
        Label unrealized = new Label();

        FlowPane accountCards = new FlowPane(10, 10);
        accountCards.setAlignment(Pos.CENTER_LEFT);
        accountCards.setPrefWrapLength(900);

        TableView<PaperPositionRow> positions =
                createPaperPositionsTable();
        TableView<PaperHistoryRow> history =
                createPaperHistoryTable();
        TableView<PaperAutoLogRow> autoLog =
                createPaperAutoLogTable();

        Button buyCurrent = new Button("Comprar activo actual");
        buyCurrent.getStyleClass().add("primary-button");

        Button closeSelected = new Button("Cerrar posición");
        closeSelected.getStyleClass().add("secondary-button");

        Button configureCapital = new Button("Capital inicial");
        configureCapital.getStyleClass().add("secondary-button");

        Button refreshNow = new Button("Actualizar ahora");
        refreshNow.getStyleClass().add("secondary-button");

        Button portfolioAutoButton = new Button("Cartera AUTO");
        portfolioAutoButton.getStyleClass().add("secondary-button");
        portfolioAutoButton.setOnAction(
                e -> showPortfolioAutoDialog()
        );

        Label activeAccount = new Label();
        activeAccount.getStyleClass().add("paper-active-account");

        Label lastUpdate = new Label("Última actualización: pendiente");
        lastUpdate.getStyleClass().add("paper-last-update");

        Label autoStatus = new Label(
                "PRECIOS · cada 60 s · Stop/Take activos"
        );
        autoStatus.getStyleClass().add("paper-auto-status");

        java.util.function.Supplier<String> selectedCurrency = () ->
                usdButton.isSelected() ? "USD" : "ARS";

        // ---------------- Auto strategy controls ----------------
        CheckBox autoEnabled = new CheckBox("Automático");
        autoEnabled.getStyleClass().add("paper-auto-checkbox");

        ComboBox<String> autoStrategy = new ComboBox<>();
        for (StrategyType type : StrategyType.values()) {
            autoStrategy.getItems().add(type.name());
        }
        autoStrategy.setValue(StrategyType.MOMENTUM.name());
        autoStrategy.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(
                        empty || item == null
                                ? null
                                : StrategyType.valueOf(item).displayName()
                );
            }
        });
        autoStrategy.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(
                        empty || item == null
                                ? null
                                : StrategyType.valueOf(item).displayName()
                );
            }
        });
        fixedWidth(autoStrategy, 150);

        TextField maxCapitalField = new TextField("2000");
        maxCapitalField.setPromptText("Capital máximo");
        fixedWidth(maxCapitalField, 120);

        TextField riskField = new TextField("1");
        riskField.setPromptText("1");
        fixedWidth(riskField, 70);

        TextField stopPctField = new TextField("2");
        stopPctField.setPromptText("2");
        fixedWidth(stopPctField, 70);

        TextField takePctField = new TextField("4");
        takePctField.setPromptText("4");
        fixedWidth(takePctField, 70);

        Label autoAssetLabel = new Label("Activo AUTO: sin configurar");
        autoAssetLabel.getStyleClass().add("paper-auto-asset");

        Label autoRunStatus = new Label("AUTO estrategias: pausado");
        autoRunStatus.getStyleClass().add("paper-auto-run-status");

        Button useCurrentForAuto = new Button("Usar activo analizado");
        useCurrentForAuto.getStyleClass().add("secondary-button");

        Button saveAuto = new Button("Guardar AUTO");
        saveAuto.getStyleClass().add("primary-button");

        PaperAutoConfig storedAuto = paperTradingService.autoConfig();

        if (storedAuto != null) {
            autoEnabled.setSelected(storedAuto.enabled());

            if (storedAuto.strategy() != null
                    && !storedAuto.strategy().isBlank()) {
                autoStrategy.setValue(storedAuto.strategy());
            }

            maxCapitalField.setText(
                    String.format("%.2f", storedAuto.maxCapital())
            );
            riskField.setText(
                    String.format("%.2f", storedAuto.riskPct())
            );
            stopPctField.setText(
                    String.format("%.2f", storedAuto.stopLossPct())
            );
            takePctField.setText(
                    String.format("%.2f", storedAuto.takeProfitPct())
            );

            if (storedAuto.symbol() != null
                    && !storedAuto.symbol().isBlank()) {
                autoAssetLabel.setText(
                        "Activo AUTO: "
                                + storedAuto.symbol()
                                + " · "
                                + storedAuto.timeframe()
                                + " · "
                                + storedAuto.period()
                );
            }

            autoRunStatus.setText(
                    storedAuto.enabled()
                            ? "AUTO estrategias: ACTIVO"
                            : "AUTO estrategias: pausado"
            );
        }

        final AnalysisResult[] autoTarget = new AnalysisResult[1];

        if (currentAnalysis != null
                && storedAuto.symbol() != null
                && storedAuto.symbol().equalsIgnoreCase(
                        currentAnalysis.symbol()
                )) {
            autoTarget[0] = currentAnalysis;
        }

        Runnable refreshUi = () -> {
            String currency = selectedCurrency.get();
            PaperAccount account = paperTradingService.account(currency);

            activeAccount.setText("Cuenta activa: " + currency);

            initialCapital.setText(
                    paperMoney(account.initialCapital(), currency)
            );
            cash.setText(
                    paperMoney(account.cash(), currency)
            );
            equity.setText(
                    paperMoney(
                            paperTradingService.equity(currency),
                            currency
                    )
            );
            unrealized.setText(
                    String.format(
                            "%s %+.2f",
                            paperPrefix(currency),
                            paperTradingService.unrealizedPnl(currency)
                    )
            );

            accountCards.getChildren().setAll(
                    paperMetric("Capital inicial", initialCapital),
                    paperMetric("Disponible", cash),
                    paperMetric("Equity", equity),
                    paperMetric("P&L no realizado", unrealized)
            );

            positions.setItems(
                    FXCollections.observableArrayList(
                            paperTradingService.state().positions.stream()
                                    .filter(position ->
                                            currency.equalsIgnoreCase(
                                                    position.currency()
                                            )
                                    )
                                    .map(PaperPositionRow::new)
                                    .toList()
                    )
            );

            history.setItems(
                    FXCollections.observableArrayList(
                            paperTradingService.state().history.stream()
                                    .filter(trade ->
                                            currency.equalsIgnoreCase(
                                                    trade.currency()
                                            )
                                    )
                                    .map(PaperHistoryRow::new)
                                    .toList()
                    )
            );

            autoLog.setItems(
                    FXCollections.observableArrayList(
                            paperTradingService.autoLog().stream()
                                    .map(PaperAutoLogRow::new)
                                    .toList()
                    )
            );
        };

        AtomicBoolean refreshing = new AtomicBoolean(false);
        AtomicBoolean autoRunning = new AtomicBoolean(false);
        AtomicBoolean portfolioRunning = new AtomicBoolean(false);

        Runnable runPortfolioAuto = () -> {
            PaperPortfolioAutoConfig config =
                    paperTradingService.portfolioAutoConfig();

            if (config == null || !config.enabled()) {
                return;
            }

            if (!portfolioRunning.compareAndSet(false, true)) {
                return;
            }

            autoRunStatus.setText(
                    "Cartera AUTO: escaneando watchlists..."
            );

            Task<PaperPortfolioAutoResult> task = new Task<>() {
                @Override
                protected PaperPortfolioAutoResult call()
                        throws Exception {
                    return paperPortfolioAutoEngine.runOnce(config);
                }
            };

            task.setOnSucceeded(e -> {
                PaperPortfolioAutoResult result = task.getValue();

                autoRunStatus.setText(
                        "Cartera AUTO: "
                                + result.scanned()
                                + " escaneados · "
                                + result.entries()
                                + " entradas · "
                                + result.exits()
                                + " salidas"
                );

                lastUpdate.setText(
                        "Última cartera AUTO: "
                                + paperUpdateTime(result.timestamp())
                );

                refreshUi.run();
                portfolioRunning.set(false);
            });

            task.setOnFailed(e -> {
                Throwable error = task.getException();

                autoRunStatus.setText(
                        "Cartera AUTO: ERROR · "
                                + (
                                error == null
                                        ? "desconocido"
                                        : error.getMessage()
                        )
                );

                portfolioRunning.set(false);
            });

            Thread thread = new Thread(
                    task,
                    "paper-portfolio-auto"
            );
            thread.setDaemon(true);
            thread.start();
        };

        Runnable refreshMarket = () -> {
            if (!refreshing.compareAndSet(false, true)) {
                return;
            }

            refreshNow.setDisable(true);
            lastUpdate.setText("Actualizando posiciones...");

            Task<PaperRefreshResult> task = new Task<>() {
                @Override
                protected PaperRefreshResult call() throws Exception {
                    return paperTradingService.refreshOpenPositions(
                            marketService
                    );
                }
            };

            task.setOnSucceeded(e -> {
                PaperRefreshResult result = task.getValue();

                refreshUi.run();
                lastUpdate.setText(
                        "Última actualización: "
                                + paperUpdateTime(result.updatedAt())
                                + " · "
                                + result.updatedPositions()
                                + " actualizadas"
                                + (
                                result.closedPositions() > 0
                                        ? " · "
                                        + result.closedPositions()
                                        + " cerradas por Stop/Take"
                                        : ""
                        )
                );

                refreshing.set(false);
                refreshNow.setDisable(false);
            });

            task.setOnFailed(e -> {
                Throwable error = task.getException();

                lastUpdate.setText(
                        "Actualización con error: "
                                + (
                                error == null
                                        ? "desconocido"
                                        : error.getMessage()
                        )
                );

                refreshing.set(false);
                refreshNow.setDisable(false);
            });

            Thread thread = new Thread(
                    task,
                    "paper-trading-refresh"
            );
            thread.setDaemon(true);
            thread.start();
        };

        Runnable runAutoStrategy = () -> {
            PaperAutoConfig config = paperTradingService.autoConfig();

            if (config == null || !config.enabled()) {
                autoRunStatus.setText("AUTO estrategias: pausado");
                return;
            }

            if (!autoRunning.compareAndSet(false, true)) {
                return;
            }

            autoRunStatus.setText(
                    "AUTO estrategias: evaluando "
                            + config.symbol()
                            + "..."
            );

            Task<PaperAutoResult> task = new Task<>() {
                @Override
                protected PaperAutoResult call() throws Exception {
                    return paperAutoTradingEngine.runOnce(config);
                }
            };

            task.setOnSucceeded(e -> {
                PaperAutoResult result = task.getValue();

                autoRunStatus.setText(
                        "AUTO estrategias: "
                                + result.action()
                                + " · "
                                + result.message()
                );

                lastUpdate.setText(
                        "Última evaluación AUTO: "
                                + paperUpdateTime(result.timestamp())
                );

                refreshUi.run();
                autoRunning.set(false);
            });

            task.setOnFailed(e -> {
                Throwable error = task.getException();

                autoRunStatus.setText(
                        "AUTO estrategias: ERROR · "
                                + (
                                error == null
                                        ? "desconocido"
                                        : error.getMessage()
                        )
                );

                try {
                    paperTradingService.addAutoLog(
                            "ERROR",
                            error == null
                                    ? "Error desconocido"
                                    : error.getMessage()
                    );
                } catch (Exception ignored) {
                }

                refreshUi.run();
                autoRunning.set(false);
            });

            Thread thread = new Thread(
                    task,
                    "paper-auto-strategy"
            );
            thread.setDaemon(true);
            thread.start();
        };

        useCurrentForAuto.setOnAction(e -> {
            if (currentAnalysis == null) {
                showPaperError(
                        "Primero analizá un activo en la pantalla principal."
                );
                return;
            }

            autoTarget[0] = currentAnalysis;

            maxCapitalField.setText(
                    "ARS".equalsIgnoreCase(currentAnalysis.currency())
                            ? "200000"
                            : "2000"
            );

            autoAssetLabel.setText(
                    "Activo AUTO: "
                            + currentAnalysis.symbol()
                            + " · "
                            + currentAnalysis.timeframe()
                            + " · "
                            + currentAnalysis.period()
            );

            if ("USD".equalsIgnoreCase(currentAnalysis.currency())) {
                usdButton.setSelected(true);
            } else {
                arsButton.setSelected(true);
            }
        });

        saveAuto.setOnAction(e -> {
            AnalysisResult target = autoTarget[0];

            if (target == null) {
                PaperAutoConfig previous = paperTradingService.autoConfig();

                if (previous != null
                        && previous.symbol() != null
                        && !previous.symbol().isBlank()) {
                    try {
                        PaperAutoConfig updated = new PaperAutoConfig(
                                autoEnabled.isSelected(),
                                previous.symbol(),
                                previous.marketType(),
                                previous.source(),
                                previous.currency(),
                                previous.timeframe(),
                                previous.period(),
                                autoStrategy.getValue(),
                                parsePaperNumber(
                                        maxCapitalField.getText(),
                                        true
                                ),
                                parsePaperNumber(
                                        riskField.getText(),
                                        true
                                ),
                                parsePaperNumber(
                                        stopPctField.getText(),
                                        true
                                ),
                                parsePaperNumber(
                                        takePctField.getText(),
                                        true
                                )
                        );

                        validatePaperAutoConfig(updated);
                        paperTradingService.setAutoConfig(updated);

                        autoRunStatus.setText(
                                updated.enabled()
                                        ? "AUTO estrategias: ACTIVO"
                                        : "AUTO estrategias: pausado"
                        );

                        return;
                    } catch (Exception ex) {
                        showPaperError(ex.getMessage());
                        return;
                    }
                }

                showPaperError(
                        "Elegí 'Usar activo analizado' para configurar el automático."
                );
                return;
            }

            try {
                PaperAutoConfig config = new PaperAutoConfig(
                        autoEnabled.isSelected(),
                        target.symbol(),
                        target.marketType(),
                        target.source(),
                        target.currency(),
                        target.timeframe(),
                        target.period(),
                        autoStrategy.getValue(),
                        parsePaperNumber(
                                maxCapitalField.getText(),
                                true
                        ),
                        parsePaperNumber(
                                riskField.getText(),
                                true
                        ),
                        parsePaperNumber(
                                stopPctField.getText(),
                                true
                        ),
                        parsePaperNumber(
                                takePctField.getText(),
                                true
                        )
                );

                validatePaperAutoConfig(config);
                paperTradingService.setAutoConfig(config);

                autoAssetLabel.setText(
                        "Activo AUTO: "
                                + config.symbol()
                                + " · "
                                + config.timeframe()
                                + " · "
                                + config.period()
                );

                autoRunStatus.setText(
                        config.enabled()
                                ? "AUTO estrategias: ACTIVO"
                                : "AUTO estrategias: pausado"
                );

                paperTradingService.addAutoLog(
                        "CONFIG",
                        config.enabled()
                                ? "Automatización activada: "
                                + config.symbol()
                                + " · "
                                + StrategyType
                                        .valueOf(config.strategy())
                                        .displayName()
                                : "Automatización pausada."
                );

                refreshUi.run();

                if (config.enabled()) {
                    runAutoStrategy.run();
                }
            } catch (Exception ex) {
                showPaperError(ex.getMessage());
            }
        });

        currencyGroup.selectedToggleProperty().addListener(
                (obs, oldToggle, newToggle) -> {
                    if (newToggle == null) {
                        if ("USD".equalsIgnoreCase(
                                selectedCurrency.get()
                        )) {
                            usdButton.setSelected(true);
                        } else {
                            arsButton.setSelected(true);
                        }
                        return;
                    }

                    refreshUi.run();
                }
        );

        configureCapital.setOnAction(e -> {
            String currency = selectedCurrency.get();

            TextInputDialog dialog = new TextInputDialog(
                    String.format(
                            "%.2f",
                            paperTradingService
                                    .account(currency)
                                    .initialCapital()
                    )
            );

            dialog.setTitle("Capital inicial");
            dialog.setHeaderText(
                    "Cuenta simulada " + currency
            );
            dialog.setContentText("Capital:");

            dialog.showAndWait().ifPresent(value -> {
                try {
                    double amount = Double.parseDouble(
                            value.trim().replace(",", ".")
                    );

                    paperTradingService.resetAccount(
                            currency,
                            amount
                    );

                    refreshUi.run();
                } catch (Exception ex) {
                    showPaperError(ex.getMessage());
                }
            });
        });

        buyCurrent.setOnAction(e -> {
            if (currentAnalysis == null) {
                showPaperError(
                        "Primero ejecutá un análisis de mercado "
                                + "para tener un precio actual."
                );
                return;
            }

            if ("USD".equalsIgnoreCase(currentAnalysis.currency())) {
                usdButton.setSelected(true);
            } else {
                arsButton.setSelected(true);
            }

            refreshUi.run();
            showBuyPaperTrade(refreshUi);
        });

        closeSelected.setOnAction(e -> {
            PaperPositionRow row =
                    positions.getSelectionModel().getSelectedItem();

            if (row == null) {
                showPaperError(
                        "Seleccioná una posición abierta."
                );
                return;
            }

            PaperPosition position = row.position();

            double currentPrice = position.lastPrice() == null
                    ? position.entryPrice()
                    : position.lastPrice();

            TextInputDialog dialog = new TextInputDialog(
                    String.format("%.4f", currentPrice)
            );

            dialog.setTitle("Cerrar posición");
            dialog.setHeaderText(
                    position.symbol()
                            + " · "
                            + paperMoney(
                                    currentPrice,
                                    position.currency()
                            )
            );
            dialog.setContentText("Precio de salida:");

            dialog.showAndWait().ifPresent(value -> {
                try {
                    double exitPrice = Double.parseDouble(
                            value.trim().replace(",", ".")
                    );

                    paperTradingService.close(
                            position.id(),
                            exitPrice,
                            "Cierre manual"
                    );

                    refreshUi.run();
                } catch (Exception ex) {
                    showPaperError(ex.getMessage());
                }
            });
        });

        refreshNow.setOnAction(e -> {
            PaperPortfolioAutoConfig portfolioConfig =
                    paperTradingService.portfolioAutoConfig();

            if (portfolioConfig != null
                    && portfolioConfig.enabled()) {
                runPortfolioAuto.run();
                return;
            }

            PaperAutoConfig config =
                    paperTradingService.autoConfig();

            if (config != null && config.enabled()) {
                runAutoStrategy.run();
            } else {
                refreshMarket.run();
            }
        });

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        HBox header = new HBox(
                18,
                heading,
                headerSpacer,
                accountSelectorBox
        );
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("paper-header");

        Region actionSpacer = new Region();

        HBox actionBar = new HBox(
                10,
                activeAccount,
                autoStatus,
                actionSpacer,
                refreshNow,
                portfolioAutoButton,
                configureCapital,
                buyCurrent,
                closeSelected
        );
        HBox.setHgrow(actionSpacer, Priority.ALWAYS);
        actionBar.setAlignment(Pos.CENTER_LEFT);
        actionBar.getStyleClass().add("paper-action-bar");

        FlowPane autoControls = new FlowPane(10, 8);
        autoControls.setAlignment(Pos.CENTER_LEFT);
        autoControls.getStyleClass().add("paper-auto-config");

        autoControls.getChildren().addAll(
                autoEnabled,
                labeledPaperControl("Estrategia", autoStrategy),
                labeledPaperControl("Capital máx.", maxCapitalField),
                labeledPaperControl("Riesgo %", riskField),
                labeledPaperControl("Stop %", stopPctField),
                labeledPaperControl("Take %", takePctField),
                useCurrentForAuto,
                saveAuto
        );

        VBox autoPanel = new VBox(
                8,
                autoAssetLabel,
                autoControls,
                autoRunStatus
        );
        autoPanel.getStyleClass().add("paper-auto-panel");
        autoPanel.setPadding(new Insets(12));

        Tab positionsTab = new Tab(
                "Posiciones abiertas",
                positions
        );
        positionsTab.setClosable(false);

        Tab historyTab = new Tab(
                "Historial",
                history
        );
        historyTab.setClosable(false);

        Tab autoLogTab = new Tab(
                "Actividad AUTO",
                autoLog
        );
        autoLogTab.setClosable(false);

        TabPane tabs = new TabPane(
                positionsTab,
                historyTab,
                autoLogTab
        );
        tabs.setTabClosingPolicy(
                TabPane.TabClosingPolicy.UNAVAILABLE
        );
        VBox.setVgrow(tabs, Priority.ALWAYS);

        Label note = new Label(
                "Precios y automatización se evalúan cada 60 segundos mientras "
                        + "esta ventana está abierta. Las señales automáticas usan "
                        + "la última vela cerrada para evitar operar con una vela "
                        + "todavía en formación. Es Paper Trading: no envía órdenes reales."
        );
        note.setWrapText(true);
        note.getStyleClass().add("paper-note");

        VBox footer = new VBox(
                4,
                lastUpdate,
                note
        );

        VBox root = new VBox(
                14,
                header,
                accountCards,
                actionBar,
                autoPanel,
                tabs,
                footer
        );

        root.setPadding(new Insets(18));
        root.getStyleClass().add("paper-root");

        Scene scene = new Scene(root, 1320, 800);
        scene.getStylesheets().add(
                IaTradexApp.class
                        .getResource("/com/iatradex/theme.css")
                        .toExternalForm()
        );

        window.setScene(scene);
        window.setMinWidth(820);
        window.setMinHeight(640);

        try {
            window.getIcons().add(
                    new Image(
                            IaTradexApp.class.getResourceAsStream(
                                    "/com/iatradex/icon.png"
                            )
                    )
            );
        } catch (Exception ignored) {
        }

        Timeline autoRefresh = new Timeline(
                new KeyFrame(
                        Duration.seconds(60),
                        e -> {
                            PaperPortfolioAutoConfig portfolioConfig =
                                    paperTradingService.portfolioAutoConfig();

                            if (portfolioConfig != null
                                    && portfolioConfig.enabled()) {
                                runPortfolioAuto.run();
                                return;
                            }

                            PaperAutoConfig config =
                                    paperTradingService.autoConfig();

                            if (config != null && config.enabled()) {
                                runAutoStrategy.run();
                            } else {
                                refreshMarket.run();
                            }
                        }
                )
        );
        autoRefresh.setCycleCount(Timeline.INDEFINITE);

        window.setOnShown(e -> {
            refreshUi.run();

            PaperPortfolioAutoConfig portfolioConfig =
                    paperTradingService.portfolioAutoConfig();

            if (portfolioConfig != null
                    && portfolioConfig.enabled()) {
                runPortfolioAuto.run();
            } else {
                PaperAutoConfig config =
                        paperTradingService.autoConfig();

                if (config != null && config.enabled()) {
                    runAutoStrategy.run();
                } else {
                    refreshMarket.run();
                }
            }

            autoRefresh.play();
        });

        window.setOnHidden(e -> autoRefresh.stop());

        window.show();
    }

    private void showPortfolioAutoDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("IA-TradeX · Cartera AUTO");

        PaperPortfolioAutoConfig current =
                paperTradingService.portfolioAutoConfig();

        CheckBox enabled = new CheckBox(
                "Activar Scanner + Paper Trading multi-activo"
        );
        enabled.setSelected(current.enabled());

        TextField minScore = new TextField(
                String.valueOf(current.minScore())
        );
        TextField maxPositions = new TextField(
                String.valueOf(current.maxPositions())
        );
        TextField maxArgentina = new TextField(
                String.valueOf(current.maxArgentinaPositions())
        );
        TextField maxInternational = new TextField(
                String.valueOf(current.maxInternationalPositions())
        );
        TextField maxCrypto = new TextField(
                String.valueOf(current.maxCryptoPositions())
        );
        TextField globalRisk = new TextField(
                String.format("%.2f", current.maxGlobalRiskPct())
        );
        TextField tradeRisk = new TextField(
                String.format("%.2f", current.riskPerTradePct())
        );
        TextField capitalPerTrade = new TextField(
                String.format("%.2f", current.maxCapitalPerTradePct())
        );
        TextField stop = new TextField(
                String.format("%.2f", current.stopLossPct())
        );
        TextField take = new TextField(
                String.format("%.2f", current.takeProfitPct())
        );

        for (TextField field : List.of(
                minScore,
                maxPositions,
                maxArgentina,
                maxInternational,
                maxCrypto,
                globalRisk,
                tradeRisk,
                capitalPerTrade,
                stop,
                take
        )) {
            fixedWidth(field, 96);
        }

        Label positions = new Label();
        Label argentinaCount = new Label();
        Label internationalCount = new Label();
        Label cryptoCount = new Label();

        Label arsExposure = new Label();
        Label usdExposure = new Label();
        Label arsRisk = new Label();
        Label usdRisk = new Label();

        for (Label label : List.of(
                positions,
                argentinaCount,
                internationalCount,
                cryptoCount,
                arsExposure,
                usdExposure,
                arsRisk,
                usdRisk
        )) {
            label.getStyleClass().add("portfolio-metric-value");
        }

        GridPane configGrid = new GridPane();
        configGrid.setHgap(12);
        configGrid.setVgap(8);

        configGrid.addRow(
                0,
                new Label("Score mínimo"),
                minScore,
                new Label("Máx. total"),
                maxPositions
        );
        configGrid.addRow(
                1,
                new Label("Máx. Argentina"),
                maxArgentina,
                new Label("Máx. Internacional"),
                maxInternational
        );
        configGrid.addRow(
                2,
                new Label("Máx. Crypto"),
                maxCrypto,
                new Label("Riesgo global %"),
                globalRisk
        );
        configGrid.addRow(
                3,
                new Label("Riesgo por operación %"),
                tradeRisk,
                new Label("Capital por operación %"),
                capitalPerTrade
        );
        configGrid.addRow(
                4,
                new Label("Stop Loss %"),
                stop,
                new Label("Take Profit %"),
                take
        );

        ListView<String> ranking = new ListView<>();
        ranking.getStyleClass().add("portfolio-ranking");
        ranking.setPrefHeight(220);

        Label rankingTitle = new Label(
                "Ranking continuo del Scanner"
        );
        rankingTitle.getStyleClass().add("paper-active-account");

        Label rankingHelp = new Label(
                "Se actualiza con cada ciclo de Cartera AUTO. "
                        + "Muestra Score, señal, estrategia y decisión."
        );
        rankingHelp.setWrapText(true);
        rankingHelp.getStyleClass().add("paper-note");

        Runnable refreshPortfolioUi = () -> {
            positions.setText(
                    "Total abiertas: "
                            + paperTradingService.openPositionCount()
            );

            argentinaCount.setText(
                    "Argentina: "
                            + paperTradingService
                                    .openPositionCountByMarket(
                                            "argentina"
                                    )
            );
            internationalCount.setText(
                    "Internacional: "
                            + paperTradingService
                                    .openPositionCountByMarket(
                                            "stocks"
                                    )
            );
            cryptoCount.setText(
                    "Crypto: "
                            + paperTradingService
                                    .openPositionCountByMarket(
                                            "crypto"
                                    )
            );

            arsExposure.setText(
                    String.format(
                            "Exposición ARS: AR$ %,.2f · %.2f%%",
                            paperTradingService
                                    .totalExposureAmount("ARS"),
                            paperTradingService
                                    .exposurePct("ARS")
                    )
            );
            usdExposure.setText(
                    String.format(
                            "Exposición USD: US$ %,.2f · %.2f%%",
                            paperTradingService
                                    .totalExposureAmount("USD"),
                            paperTradingService
                                    .exposurePct("USD")
                    )
            );

            arsRisk.setText(
                    String.format(
                            "Riesgo abierto ARS: AR$ %,.2f · %.2f%%",
                            paperTradingService.openRiskAmount("ARS"),
                            paperTradingService.openRiskPct("ARS")
                    )
            );
            usdRisk.setText(
                    String.format(
                            "Riesgo abierto USD: US$ %,.2f · %.2f%%",
                            paperTradingService.openRiskAmount("USD"),
                            paperTradingService.openRiskPct("USD")
                    )
            );

            ranking.setItems(
                    FXCollections.observableArrayList(
                            paperTradingService
                                    .portfolioRanking()
                                    .stream()
                                    .map(candidate ->
                                            String.format(
                                                    "%s · %s · Score %d · %s · %s · %s",
                                                    candidate.symbol(),
                                                    candidate.market(),
                                                    candidate.score(),
                                                    candidate.signal(),
                                                    candidate.strategy(),
                                                    candidate.decision()
                                            )
                                    )
                                    .toList()
                    )
            );
        };

        refreshPortfolioUi.run();

        VBox exposureBox = new VBox(
                6,
                new Label("Exposición y riesgo"),
                positions,
                new HBox(
                        16,
                        argentinaCount,
                        internationalCount,
                        cryptoCount
                ),
                arsExposure,
                arsRisk,
                usdExposure,
                usdRisk
        );
        exposureBox.getStyleClass().add("portfolio-exposure-box");
        exposureBox.setPadding(new Insets(12));

        Label explanation = new Label(
                "Cada 60 segundos se escanean todas las watchlists. "
                        + "Para abrir una posición se exige señal ENTRADA, "
                        + "Score mínimo, espacio en el límite total y en el "
                        + "límite del mercado, efectivo disponible y margen "
                        + "de riesgo global. ARS y USD se controlan por separado."
        );
        explanation.setWrapText(true);
        explanation.getStyleClass().add("paper-note");

        Label warning = new Label(
                "Paper Trading solamente · no envía órdenes reales"
        );
        warning.getStyleClass().add("warning-badge");

        Button save = new Button("Guardar");
        save.getStyleClass().add("primary-button");

        Button runNow = new Button("Ejecutar ahora");
        runNow.getStyleClass().add("secondary-button");

        Button close = new Button("Cerrar");
        close.getStyleClass().add("secondary-button");

        Label statusLabel = new Label(
                current.enabled()
                        ? "Cartera AUTO activa"
                        : "Cartera AUTO pausada"
        );
        statusLabel.getStyleClass().add("paper-auto-run-status");

        Runnable saveConfig = () -> {
            try {
                PaperPortfolioAutoConfig config =
                        new PaperPortfolioAutoConfig(
                                enabled.isSelected(),
                                Integer.parseInt(
                                        minScore.getText().trim()
                                ),
                                Integer.parseInt(
                                        maxPositions.getText().trim()
                                ),
                                Integer.parseInt(
                                        maxArgentina.getText().trim()
                                ),
                                Integer.parseInt(
                                        maxInternational.getText().trim()
                                ),
                                Integer.parseInt(
                                        maxCrypto.getText().trim()
                                ),
                                parsePaperNumber(
                                        globalRisk.getText(),
                                        true
                                ),
                                parsePaperNumber(
                                        tradeRisk.getText(),
                                        true
                                ),
                                parsePaperNumber(
                                        capitalPerTrade.getText(),
                                        true
                                ),
                                parsePaperNumber(
                                        stop.getText(),
                                        true
                                ),
                                parsePaperNumber(
                                        take.getText(),
                                        true
                                )
                        );

                validatePortfolioAutoConfig(config);
                paperTradingService.setPortfolioAutoConfig(config);

                // Cartera AUTO y AUTO de un solo activo son excluyentes.
                if (config.enabled()) {
                    PaperAutoConfig single =
                            paperTradingService.autoConfig();

                    if (single != null && single.enabled()) {
                        paperTradingService.setAutoConfig(
                                new PaperAutoConfig(
                                        false,
                                        single.symbol(),
                                        single.marketType(),
                                        single.source(),
                                        single.currency(),
                                        single.timeframe(),
                                        single.period(),
                                        single.strategy(),
                                        single.maxCapital(),
                                        single.riskPct(),
                                        single.stopLossPct(),
                                        single.takeProfitPct()
                                )
                        );
                    }
                }

                statusLabel.setText(
                        config.enabled()
                                ? "Cartera AUTO activa"
                                : "Cartera AUTO pausada"
                );

                paperTradingService.addAutoLog(
                        "PORTFOLIO_CONFIG",
                        config.enabled()
                                ? "Cartera AUTO activada · score >= "
                                + config.minScore()
                                + " · máximo "
                                + config.maxPositions()
                                + " posiciones"
                                : "Cartera AUTO pausada"
                );

                refreshPortfolioUi.run();
            } catch (Exception ex) {
                showPaperError(ex.getMessage());
            }
        };

        save.setOnAction(e -> saveConfig.run());

        runNow.setOnAction(e -> {
            saveConfig.run();

            PaperPortfolioAutoConfig config =
                    paperTradingService.portfolioAutoConfig();

            if (!config.enabled()) {
                statusLabel.setText(
                        "Activá Cartera AUTO para ejecutar."
                );
                return;
            }

            runNow.setDisable(true);
            statusLabel.setText(
                    "Escaneando watchlists y evaluando cartera..."
            );

            Task<PaperPortfolioAutoResult> task =
                    new Task<>() {
                        @Override
                        protected PaperPortfolioAutoResult call()
                                throws Exception {
                            return paperPortfolioAutoEngine
                                    .runOnce(config);
                        }
                    };

            task.setOnSucceeded(event -> {
                PaperPortfolioAutoResult result =
                        task.getValue();

                statusLabel.setText(
                        result.scanned()
                                + " escaneados · "
                                + result.entries()
                                + " entradas · "
                                + result.exits()
                                + " salidas · "
                                + result.skipped()
                                + " omitidos"
                );

                refreshPortfolioUi.run();
                runNow.setDisable(false);
            });

            task.setOnFailed(event -> {
                Throwable error = task.getException();
                statusLabel.setText(
                        "ERROR · "
                                + (
                                error == null
                                        ? "desconocido"
                                        : error.getMessage()
                        )
                );
                runNow.setDisable(false);
            });

            Thread thread = new Thread(
                    task,
                    "portfolio-auto-manual"
            );
            thread.setDaemon(true);
            thread.start();
        });

        close.setOnAction(e -> dialog.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(
                10,
                statusLabel,
                spacer,
                runNow,
                save,
                close
        );
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox rankingBox = new VBox(
                6,
                rankingTitle,
                rankingHelp,
                ranking
        );
        VBox.setVgrow(ranking, Priority.ALWAYS);

        VBox content = new VBox(
                12,
                new Label("Cartera AUTO multi-activo"),
                warning,
                enabled,
                configGrid,
                exposureBox,
                rankingBox,
                explanation,
                actions
        );
        content.setPadding(new Insets(18));
        content.getStyleClass().addAll(
                "paper-root",
                "portfolio-auto-root"
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );
        scroll.setVbarPolicy(
                ScrollPane.ScrollBarPolicy.AS_NEEDED
        );
        scroll.getStyleClass().add("paper-root");

        Scene scene = new Scene(scroll, 820, 760);
        scene.getStylesheets().add(
                IaTradexApp.class
                        .getResource("/com/iatradex/theme.css")
                        .toExternalForm()
        );

        dialog.setScene(scene);
        dialog.setMinWidth(720);
        dialog.setMinHeight(620);

        Timeline uiRefresh = new Timeline(
                new KeyFrame(
                        Duration.seconds(2),
                        e -> refreshPortfolioUi.run()
                )
        );
        uiRefresh.setCycleCount(Timeline.INDEFINITE);
        dialog.setOnShown(e -> uiRefresh.play());
        dialog.setOnHidden(e -> uiRefresh.stop());

        try {
            dialog.getIcons().add(
                    new Image(
                            IaTradexApp.class.getResourceAsStream(
                                    "/com/iatradex/icon.png"
                            )
                    )
            );
        } catch (Exception ignored) {
        }

        dialog.showAndWait();
    }


    private void validatePortfolioAutoConfig(
            PaperPortfolioAutoConfig config
    ) {
        if (config.minScore() < 0
                || config.minScore() > 100) {
            throw new IllegalArgumentException(
                    "El Score mínimo debe estar entre 0 y 100."
            );
        }

        if (config.maxPositions() < 1
                || config.maxPositions() > 50) {
            throw new IllegalArgumentException(
                    "Máximo de posiciones debe estar entre 1 y 50."
            );
        }

        if (config.maxArgentinaPositions() < 1
                || config.maxArgentinaPositions() > 50
                || config.maxInternationalPositions() < 1
                || config.maxInternationalPositions() > 50
                || config.maxCryptoPositions() < 1
                || config.maxCryptoPositions() > 50) {
            throw new IllegalArgumentException(
                    "Los límites por mercado deben estar entre 1 y 50."
            );
        }

        if (config.maxGlobalRiskPct() <= 0.0
                || config.maxGlobalRiskPct() > 100.0) {
            throw new IllegalArgumentException(
                    "Riesgo global debe estar entre 0 y 100%."
            );
        }

        if (config.riskPerTradePct() <= 0.0
                || config.riskPerTradePct()
                > config.maxGlobalRiskPct()) {
            throw new IllegalArgumentException(
                    "Riesgo por operación debe ser mayor que 0 "
                            + "y no superar el riesgo global."
            );
        }

        if (config.maxCapitalPerTradePct() <= 0.0
                || config.maxCapitalPerTradePct() > 100.0) {
            throw new IllegalArgumentException(
                    "Capital por operación debe estar entre 0 y 100%."
            );
        }

        if (config.stopLossPct() <= 0.0
                || config.stopLossPct() >= 100.0) {
            throw new IllegalArgumentException(
                    "Stop Loss debe estar entre 0 y 100%."
            );
        }

        if (config.takeProfitPct() <= 0.0) {
            throw new IllegalArgumentException(
                    "Take Profit debe ser mayor que 0%."
            );
        }
    }

    private VBox labeledPaperControl(
            String captionText,
            Node control
    ) {
        Label caption = new Label(captionText);
        caption.getStyleClass().add("paper-account-caption");

        VBox box = new VBox(4, caption, control);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void validatePaperAutoConfig(
            PaperAutoConfig config
    ) {
        if (config.maxCapital() <= 0.0) {
            throw new IllegalArgumentException(
                    "El capital máximo debe ser mayor que cero."
            );
        }

        if (config.riskPct() <= 0.0 || config.riskPct() > 100.0) {
            throw new IllegalArgumentException(
                    "El riesgo debe estar entre 0 y 100%."
            );
        }

        if (config.stopLossPct() <= 0.0
                || config.stopLossPct() >= 100.0) {
            throw new IllegalArgumentException(
                    "El Stop Loss debe estar entre 0 y 100%."
            );
        }

        if (config.takeProfitPct() <= 0.0) {
            throw new IllegalArgumentException(
                    "El Take Profit debe ser mayor que 0%."
            );
        }
    }


    private String paperUpdateTime(String instantText) {
        try {
            return DateTimeFormatter
                    .ofPattern("HH:mm:ss")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.parse(instantText));
        } catch (Exception ignored) {
            return "ahora";
        }
    }

    private void showBuyPaperTrade(Runnable refresh) {
        AnalysisResult analysis = currentAnalysis;
        double price = analysis.technical().lastPrice();
        String currency = analysis.currency();

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Comprar · Paper Trading");

        Label title = new Label(
                "Comprar " + analysis.symbol()
        );
        title.getStyleClass().add("paper-title");

        Label current = new Label(
                "Precio actual: " + paperMoney(price, currency)
        );
        current.getStyleClass().add("paper-subtitle");

        TextField quantity = new TextField("1");
        TextField stop = new TextField();
        TextField take = new TextField();

        stop.setPromptText("Opcional");
        take.setPromptText("Opcional");

        ComboBox<String> strategy = new ComboBox<>();
        strategy.getItems().add("Manual");
        for (StrategyType type : StrategyType.values()) {
            strategy.getItems().add(type.displayName());
        }
        strategy.setValue("Manual");

        String regime = analysis.regime() == null
                ? "Sin análisis"
                : analysis.regime().trend()
                        + " / "
                        + analysis.regime().volatility();

        Label regimeLabel = new Label(regime);
        regimeLabel.getStyleClass().add("paper-context");

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);

        form.add(new Label("Cantidad"), 0, 0);
        form.add(quantity, 1, 0);
        form.add(new Label("Stop Loss"), 0, 1);
        form.add(stop, 1, 1);
        form.add(new Label("Take Profit"), 0, 2);
        form.add(take, 1, 2);
        form.add(new Label("Contexto / estrategia"), 0, 3);
        form.add(strategy, 1, 3);
        form.add(new Label("Régimen actual"), 0, 4);
        form.add(regimeLabel, 1, 4);

        Button cancel = new Button("Cancelar");
        cancel.getStyleClass().add("secondary-button");
        cancel.setOnAction(e -> dialog.close());

        Button confirm = new Button("Comprar simulado");
        confirm.getStyleClass().add("primary-button");

        confirm.setOnAction(e -> {
            try {
                double qty = parsePaperNumber(quantity.getText(), true);
                Double stopValue = parsePaperOptional(stop.getText());
                Double takeValue = parsePaperOptional(take.getText());

                paperTradingService.buy(
                        analysis.symbol(),
                        humanMarket(analysis.marketType()),
                        analysis.marketType(),
                        analysis.source(),
                        currency,
                        qty,
                        price,
                        stopValue,
                        takeValue,
                        strategy.getValue(),
                        regime
                );

                refresh.run();
                dialog.close();
            } catch (Exception ex) {
                showPaperError(ex.getMessage());
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox buttons = new HBox(
                10,
                spacer,
                cancel,
                confirm
        );

        VBox box = new VBox(
                10,
                title,
                current,
                new Separator(),
                form,
                buttons
        );
        box.setPadding(new Insets(20));
        box.getStyleClass().add("paper-root");

        Scene scene = new Scene(box, 520, 430);
        scene.getStylesheets().add(
                IaTradexApp.class
                        .getResource("/com/iatradex/theme.css")
                        .toExternalForm()
        );
        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.showAndWait();
    }

    private VBox paperMetric(String captionText, Label value) {
        Label caption = new Label(captionText);
        caption.getStyleClass().add("metric-label");
        value.getStyleClass().add("metric-value");

        VBox box = new VBox(6, caption, value);
        box.setPadding(new Insets(12));
        box.setMinWidth(190);
        box.getStyleClass().add("metric-card");
        return box;
    }

    private TableView<PaperPositionRow> createPaperPositionsTable() {
        TableView<PaperPositionRow> table = new TableView<>();
        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        TableColumn<PaperPositionRow, String> symbol =
                new TableColumn<>("Activo");
        symbol.setCellValueFactory(v -> v.getValue().symbolProperty());

        TableColumn<PaperPositionRow, String> quantity =
                new TableColumn<>("Cantidad");
        quantity.setCellValueFactory(v -> v.getValue().quantityProperty());

        TableColumn<PaperPositionRow, String> entry =
                new TableColumn<>("Entrada");
        entry.setCellValueFactory(v -> v.getValue().entryProperty());

        TableColumn<PaperPositionRow, String> current =
                new TableColumn<>("Actual");
        current.setCellValueFactory(v -> v.getValue().currentProperty());

        TableColumn<PaperPositionRow, String> pnl =
                new TableColumn<>("P&L");
        pnl.setCellValueFactory(v -> v.getValue().pnlProperty());

        TableColumn<PaperPositionRow, String> stop =
                new TableColumn<>("Stop");
        stop.setCellValueFactory(v -> v.getValue().stopProperty());

        TableColumn<PaperPositionRow, String> take =
                new TableColumn<>("Take");
        take.setCellValueFactory(v -> v.getValue().takeProperty());

        TableColumn<PaperPositionRow, String> context =
                new TableColumn<>("Contexto");
        context.setCellValueFactory(v -> v.getValue().contextProperty());

        table.getColumns().addAll(
                symbol,
                quantity,
                entry,
                current,
                pnl,
                stop,
                take,
                context
        );

        return table;
    }

    private TableView<PaperHistoryRow> createPaperHistoryTable() {
        TableView<PaperHistoryRow> table = new TableView<>();
        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        TableColumn<PaperHistoryRow, String> symbol =
                new TableColumn<>("Activo");
        symbol.setCellValueFactory(v -> v.getValue().symbolProperty());

        TableColumn<PaperHistoryRow, String> quantity =
                new TableColumn<>("Cantidad");
        quantity.setCellValueFactory(v -> v.getValue().quantityProperty());

        TableColumn<PaperHistoryRow, String> entry =
                new TableColumn<>("Entrada");
        entry.setCellValueFactory(v -> v.getValue().entryProperty());

        TableColumn<PaperHistoryRow, String> exit =
                new TableColumn<>("Salida");
        exit.setCellValueFactory(v -> v.getValue().exitProperty());

        TableColumn<PaperHistoryRow, String> pnl =
                new TableColumn<>("P&L");
        pnl.setCellValueFactory(v -> v.getValue().pnlProperty());

        TableColumn<PaperHistoryRow, String> context =
                new TableColumn<>("Contexto");
        context.setCellValueFactory(v -> v.getValue().contextProperty());

        TableColumn<PaperHistoryRow, String> reason =
                new TableColumn<>("Motivo");
        reason.setCellValueFactory(v -> v.getValue().reasonProperty());

        table.getColumns().addAll(
                symbol,
                quantity,
                entry,
                exit,
                pnl,
                context,
                reason
        );

        return table;
    }

    private TableView<PaperAutoLogRow> createPaperAutoLogTable() {
        TableView<PaperAutoLogRow> table = new TableView<>();
        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        TableColumn<PaperAutoLogRow, String> time =
                new TableColumn<>("Hora");
        time.setCellValueFactory(v -> v.getValue().timeProperty());
        time.setPrefWidth(130);

        TableColumn<PaperAutoLogRow, String> level =
                new TableColumn<>("Evento");
        level.setCellValueFactory(v -> v.getValue().levelProperty());
        level.setPrefWidth(100);

        TableColumn<PaperAutoLogRow, String> message =
                new TableColumn<>("Detalle");
        message.setCellValueFactory(v -> v.getValue().messageProperty());

        table.getColumns().addAll(
                time,
                level,
                message
        );

        return table;
    }

    private void showPaperError(String message) {
        Alert alert = new Alert(
                Alert.AlertType.ERROR,
                message == null ? "Error de Paper Trading." : message,
                ButtonType.OK
        );
        alert.setTitle("Paper Trading");
        alert.setHeaderText("No se pudo completar la operación");
        alert.showAndWait();
    }

    private double parsePaperNumber(
            String value,
            boolean required
    ) {
        String text = value == null
                ? ""
                : value.trim().replace(",", ".");

        if (text.isBlank()) {
            if (required) {
                throw new IllegalArgumentException(
                        "Completá la cantidad."
                );
            }
            return 0.0;
        }

        return Double.parseDouble(text);
    }

    private Double parsePaperOptional(String value) {
        String text = value == null
                ? ""
                : value.trim();

        if (text.isBlank()) {
            return null;
        }

        double parsed = parsePaperNumber(text, false);
        return parsed <= 0.0 ? null : parsed;
    }

    private String paperMoney(double value, String currency) {
        return String.format(
                "%s %,.2f",
                paperPrefix(currency),
                value
        );
    }

    private String paperPrefix(String currency) {
        return "ARS".equalsIgnoreCase(currency)
                ? "AR$"
                : "US$";
    }


    private void analyze() {
        analyzeButton.setDisable(true);
        analyzeButton.setText("Analizando...");
        status.setText("Descargando mercado y ejecutando motor Java...");

        String marketType = marketType();
        String source = dataSource();
        String symbol = selectedSymbol();
        if ("stocks".equals(marketType)
                && (selectedStockSymbol == null || selectedStockSymbol.isBlank())) {
            updateSelectedAssetVisual(
                    symbol,
                    "https://assets.parqet.com/logos/symbol/"
                            + symbol
                            + "?format=png&size=80"
            );
        }
        String timeframe = timeframeBox.getValue();
        String period = periodBox.getValue();

        Task<AnalysisResult> task = new Task<>() {
            @Override
            protected AnalysisResult call() throws Exception {
                return analysisService.analyze(
                        marketType,
                        source,
                        symbol,
                        timeframe,
                        period,
                        currency()
                );
            }
        };

        task.setOnSucceeded(e -> {
            render(task.getValue());
            analyzeButton.setDisable(false);
            analyzeButton.setText("Analizar mercado");
        });

        task.setOnFailed(e -> {
            Throwable error = task.getException();
            status.setText(
                    "ERROR: "
                            + (error == null
                            ? "Error desconocido"
                            : error.getMessage())
            );
            analyzeButton.setDisable(false);
            analyzeButton.setText("Analizar mercado");
        });

        Thread thread = new Thread(task, "market-analysis");
        thread.setDaemon(true);
        thread.start();
    }

    private void render(AnalysisResult result) {
        currentAnalysis = result;

        TechnicalSnapshot technical = result.technical();

        try {
            paperTradingService.updateMarketPrice(
                    result.symbol(),
                    result.currency(),
                    technical.lastPrice()
            );
        } catch (Exception ignored) {
            // Paper Trading no debe bloquear el análisis de mercado.
        }

        lastPrice.setText(money(technical.lastPrice(), result.currency()));
        trend.setText(technical.trend());

        rsi.setText(
                technical.rsi14() == null
                        ? "—"
                        : String.format("%.2f", technical.rsi14())
        );

        signal.setText(technical.signal());
        explanation.setText(technical.explanation());

        renderMarketQuote(result.quote(), result.currency());
        renderRegime(result.regime());

        fillPriceChart(result.candles());
        fillStrategyComparison(result);

        StrategyPerformance primary = result.primaryStrategy();
        renderStrategy(primary);

        List<Candle> candles = result.candles();

        status.setText(
                "Java nativo · "
                        + humanMarket(result.marketType())
                        + " · "
                        + result.source()
                        + " · "
                        + result.symbol()
                        + " · "
                        + result.timeframe()
                        + " · "
                        + result.period()
                        + " · "
                        + result.currency()
                        + " · "
                        + candles.size()
                        + " velas · "
                        + fullDate.format(
                                Instant.ofEpochMilli(candles.get(0).timestamp())
                        )
                        + " → "
                        + fullDate.format(
                                Instant.ofEpochMilli(
                                        candles.get(candles.size() - 1).timestamp()
                                )
                        )
        );
    }

    private void renderStrategy(StrategyPerformance strategy) {
        Metrics metrics = strategy.metrics();

        capitalValue.setText(money(metrics.capitalFinal(), currentAnalysis.currency()));
        returnValue.setText(pct(metrics.returnPct()));
        buyHoldValue.setText(pct(metrics.buyHoldReturnPct()));
        drawdownValue.setText(pct(metrics.maxDrawdownPct()));
        winRateValue.setText(pct(metrics.winRatePct()));
        profitFactorValue.setText(nullableNumber(metrics.profitFactor()));
        sharpeValue.setText(nullableNumber(metrics.sharpeRatio()));
        tradesValue.setText(String.valueOf(metrics.trades()));
        avgWinValue.setText(nullableMoney(metrics.avgWin(), currentAnalysis.currency()));
        avgLossValue.setText(nullableMoney(metrics.avgLoss(), currentAnalysis.currency()));

        fillEquityChart(strategy.equity());
        fillTrades(strategy.trades());

        strategyTable.getSelectionModel().select(
                strategyTable.getItems()
                        .stream()
                        .filter(row -> row.performance().strategy() == strategy.strategy())
                        .findFirst()
                        .orElse(null)
        );
    }

    private void fillStrategyComparison(AnalysisResult result) {
        strategyTable.getItems().clear();

        for (StrategyPerformance performance : result.strategies()) {
            strategyTable.getItems().add(
                    new StrategyRow(
                            performance,
                            performance.strategy() == result.bestHistoricalStrategy()
                    )
            );
        }

        bestStrategyLabel.setText(
                result.bestHistoricalStrategy().displayName()
                        + " · mejor retorno histórico del período"
        );
    }


    private VBox createRegimePanel() {
        Label title = new Label("Régimen de mercado");
        title.getStyleClass().add("panel-title");

        regimeTrendLabel.getStyleClass().add("regime-main");
        regimeVolatilityLabel.getStyleClass().add("regime-value");
        regimeStrengthLabel.getStyleClass().add("regime-value");
        regimeCompatibleLabel.getStyleClass().add("regime-compatible");

        regimeExplanationLabel.setWrapText(true);
        regimeExplanationLabel.getStyleClass().add("regime-explanation");

        VBox trendBox = regimeMetric(
                "Tendencia",
                regimeTrendLabel
        );

        VBox volatilityBox = regimeMetric(
                "Volatilidad",
                regimeVolatilityLabel
        );

        VBox strengthBox = regimeMetric(
                "Fuerza",
                regimeStrengthLabel
        );

        VBox compatibleBox = regimeMetric(
                "Estrategias compatibles",
                regimeCompatibleLabel
        );
        HBox.setHgrow(compatibleBox, Priority.ALWAYS);

        FlowPane values = new FlowPane(
                26,
                6,
                trendBox,
                volatilityBox,
                strengthBox,
                compatibleBox
        );
        values.setAlignment(Pos.CENTER_LEFT);
        values.setPrefWrapLength(1000);

        VBox box = new VBox(
                6,
                title,
                values,
                regimeExplanationLabel
        );

        box.setPadding(new Insets(10, 14, 9, 14));
        box.getStyleClass().addAll(
                "panel",
                "regime-panel"
        );

        return box;
    }

    private VBox regimeMetric(String title, Label value) {
        Label caption = muted(title);

        VBox box = new VBox(
                2,
                caption,
                value
        );

        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void renderRegime(MarketRegime regime) {
        if (regime == null) {
            regimeTrendLabel.setText("Sin datos");
            regimeVolatilityLabel.setText("—");
            regimeStrengthLabel.setText("—");
            regimeCompatibleLabel.setText("—");
            regimeExplanationLabel.setText(
                    "No se pudo determinar el régimen."
            );
            return;
        }

        regimeTrendLabel.setText(regime.trend());
        regimeVolatilityLabel.setText(regime.volatility());
        regimeStrengthLabel.setText(regime.strength());

        String compatible = regime.compatibleStrategies()
                .stream()
                .map(StrategyType::displayName)
                .reduce((a, b) -> a + " · " + b)
                .orElse("Ninguna estrategia long-only prioritaria");

        regimeCompatibleLabel.setText(compatible);
        regimeExplanationLabel.setText(regime.explanation());

        regimeTrendLabel.getStyleClass().removeAll(
                "regime-bull",
                "regime-bear",
                "regime-sideways"
        );

        switch (regime.trend()) {
            case "ALCISTA" ->
                    regimeTrendLabel.getStyleClass().add("regime-bull");
            case "BAJISTA" ->
                    regimeTrendLabel.getStyleClass().add("regime-bear");
            default ->
                    regimeTrendLabel.getStyleClass().add("regime-sideways");
        }
    }

    private VBox createStrategyComparisonPanel() {
        Label title = new Label("Comparador de estrategias");
        title.getStyleClass().add("panel-title");

        bestStrategyLabel.getStyleClass().add("best-strategy-label");

        Label note = new Label(
                "Resultados del período seleccionado. No son una predicción futura."
        );
        note.getStyleClass().add("strategy-note");

        Label bestCaption = new Label("Mejor histórico:");
        bestCaption.getStyleClass().add("strategy-best-caption");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(
                8,
                title,
                spacer,
                bestCaption,
                bestStrategyLabel
        );
        header.setAlignment(Pos.CENTER_LEFT);

        // 4 estrategias + encabezado: altura compacta y fija.
        strategyTable.setFixedCellSize(28);
        strategyTable.setPrefHeight(137);
        strategyTable.setMinHeight(137);
        strategyTable.setMaxHeight(137);

        strategyTable.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> {
                    if (newValue != null) {
                        renderStrategy(newValue.performance());
                    }
                });

        VBox box = new VBox(
                5,
                header,
                strategyTable,
                note
        );

        box.setPadding(new Insets(10, 14, 8, 14));
        box.getStyleClass().add("panel");

        return box;
    }

    private void renderMarketQuote(
            MarketQuote quote,
            String currency
    ) {
        if (quote == null) {
            marketChange.setText("—");
            bid.setText("—");
            ask.setText("—");
            marketMode.setText("—");
            return;
        }

        marketChange.setText(
                quote.changePct() == null
                        ? "—"
                        : pct(quote.changePct())
        );

        bid.setText(
                quote.bidPrice() == null || quote.bidPrice() <= 0
                        ? "—"
                        : money(quote.bidPrice(), currency)
                                + (quote.bidSize() == null
                                ? ""
                                : " · " + String.format("%.0f", quote.bidSize()))
        );

        ask.setText(
                quote.askPrice() == null || quote.askPrice() <= 0
                        ? "—"
                        : money(quote.askPrice(), currency)
                                + (quote.askSize() == null
                                ? ""
                                : " · " + String.format("%.0f", quote.askSize()))
        );

        marketMode.setText(
                quote.dataMode()
                        + (quote.date() == null || quote.date().isBlank()
                        ? ""
                        : " · " + quote.date())
        );
    }

    private FlowPane createMetrics() {
        FlowPane pane = new FlowPane(4, 4);
        pane.setPrefWrapLength(1500);
        pane.setAlignment(Pos.TOP_LEFT);
        pane.getStyleClass().add("metrics-flow");

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

        Label warning = new Label("REGLAS TÉCNICAS · SIN ML TODAVÍA");
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
        grid.addRow(1, muted("Variación"), marketChange);
        grid.addRow(2, muted("Compra"), bid);
        grid.addRow(3, muted("Venta"), ask);
        grid.addRow(4, muted("Tendencia"), trend);
        grid.addRow(5, muted("RSI 14"), rsi);
        grid.addRow(6, muted("Fuente precio"), marketMode);

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
        box.setMinWidth(300);

        return box;
    }

    private void fillPriceChart(List<Candle> candles) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        int step = Math.max(1, candles.size() / 180);

        for (int i = 0; i < candles.size(); i += step) {
            Candle row = candles.get(i);

            series.getData().add(
                    new XYChart.Data<>(
                            chartDate.format(
                                    Instant.ofEpochMilli(row.timestamp())
                            ),
                            row.close()
                    )
            );
        }

        priceChart.getData().setAll(series);
    }

    private void fillEquityChart(List<EquityPoint> points) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        int step = Math.max(1, points.size() / 180);

        for (int i = 0; i < points.size(); i += step) {
            EquityPoint row = points.get(i);

            series.getData().add(
                    new XYChart.Data<>(
                            chartDate.format(
                                    Instant.ofEpochMilli(row.timestamp())
                            ),
                            row.equity()
                    )
            );
        }

        equityChart.getData().setAll(series);
    }

    private void fillTrades(List<Trade> trades) {
        tradesTable.getItems().clear();

        for (Trade row : trades) {
            tradesTable.getItems().add(
                    new TradeRow(
                            fullDate.format(
                                    Instant.ofEpochMilli(row.exitTime())
                            ),
                            row.entryPrice(),
                            row.exitPrice(),
                            row.pnl(),
                            row.reason()
                    )
            );
        }
    }

    private String humanMarket(String market) {
        if ("crypto".equals(market)) {
            return "Criptomonedas";
        }

        if ("argentina".equals(market)) {
            return "Argentina";
        }

        return "Internacional";
    }

    private static String money(double value) {
        return money(value, "USD");
    }

    private static String money(double value, String currency) {
        String prefix = "ARS".equalsIgnoreCase(currency)
                ? "AR$ "
                : "US$ ";

        return prefix + String.format("%,.2f", value);
    }

    private static String pct(double value) {
        return String.format("%+.2f%%", value);
    }

    private static String nullableNumber(Double value) {
        return value == null || !Double.isFinite(value)
                ? "—"
                : String.format("%.2f", value);
    }

    private static String nullableMoney(Double value) {
        return nullableMoney(value, "USD");
    }

    private static String nullableMoney(Double value, String currency) {
        return value == null || !Double.isFinite(value)
                ? "—"
                : money(value, currency);
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
        Label caption = muted(title);
        caption.getStyleClass().add("metric-caption");
        caption.setTextOverrun(OverrunStyle.ELLIPSIS);

        double baseWidth = switch (title) {
            case "CAPITAL FINAL" -> 108;
            case "RETORNO" -> 66;
            case "BUY & HOLD" -> 74;
            case "DRAWDOWN MÁX." -> 84;
            case "WIN RATE" -> 68;
            case "PROFIT FACTOR" -> 80;
            case "SHARPE" -> 60;
            case "OPERACIONES" -> 76;
            case "GANANCIA MEDIA",
                 "PÉRDIDA MEDIA" -> 100;
            default -> 72;
        };

        VBox box = new VBox(2, caption, value);
        box.setPadding(new Insets(5, 6, 5, 6));
        box.getStyleClass().add("metric-card");

        caption.setMinWidth(0);
        caption.setPrefWidth(baseWidth - 12);
        caption.setMaxWidth(baseWidth - 12);

        value.setMinWidth(0);
        value.setPrefWidth(Region.USE_COMPUTED_SIZE);
        value.setMaxWidth(Region.USE_COMPUTED_SIZE);

        Runnable resize = () -> {
            Text measure = new Text(
                    value.getText() == null ? "" : value.getText()
            );
            measure.setFont(value.getFont());

            double valueWidth = Math.ceil(
                    measure.getLayoutBounds().getWidth()
            );

            double width = Math.max(
                    baseWidth,
                    valueWidth + 18
            );

            box.setMinWidth(width);
            box.setPrefWidth(width);
            box.setMaxWidth(width);

            caption.setPrefWidth(width - 12);
            caption.setMaxWidth(width - 12);
        };

        value.textProperty().addListener(
                (obs, oldValue, newValue) -> resize.run()
        );

        value.fontProperty().addListener(
                (obs, oldFont, newFont) -> resize.run()
        );

        Platform.runLater(resize);

        return box;
    }


    private VBox panel(String titleText, Node node) {
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

    private TableView<StrategyRow> createStrategyTable() {
        TableView<StrategyRow> table = new TableView<>();

        TableColumn<StrategyRow, String> strategy = new TableColumn<>("Estrategia");
        strategy.setCellValueFactory(v -> v.getValue().strategyProperty());

        TableColumn<StrategyRow, String> returnPct = new TableColumn<>("Retorno");
        returnPct.setCellValueFactory(v -> v.getValue().returnPctProperty());

        TableColumn<StrategyRow, String> profitFactor = new TableColumn<>("Profit Factor");
        profitFactor.setCellValueFactory(v -> v.getValue().profitFactorProperty());

        TableColumn<StrategyRow, String> drawdown = new TableColumn<>("Drawdown");
        drawdown.setCellValueFactory(v -> v.getValue().drawdownProperty());

        TableColumn<StrategyRow, String> sharpe = new TableColumn<>("Sharpe");
        sharpe.setCellValueFactory(v -> v.getValue().sharpeProperty());

        TableColumn<StrategyRow, String> trades = new TableColumn<>("Operaciones");
        trades.setCellValueFactory(v -> v.getValue().tradesProperty());

        TableColumn<StrategyRow, String> historical = new TableColumn<>("Resultado histórico");
        historical.setCellValueFactory(v -> v.getValue().historicalProperty());

        strategy.setPrefWidth(180);
        returnPct.setPrefWidth(110);
        profitFactor.setPrefWidth(120);
        drawdown.setPrefWidth(110);
        sharpe.setPrefWidth(95);
        trades.setPrefWidth(110);
        historical.setPrefWidth(170);

        table.getColumns().addAll(
                strategy,
                returnPct,
                profitFactor,
                drawdown,
                sharpe,
                trades,
                historical
        );

        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );
        table.getStyleClass().add("strategy-table");

        return table;
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

    private static void matchTopControlHeight(
            Control... controls
    ) {
        for (Control control : controls) {
            control.setMinHeight(34);
            control.setPrefHeight(34);
            control.setMaxHeight(34);
        }
    }

    private static void fixedWidth(Control control, double width) {
        control.setMinWidth(width);
        control.setPrefWidth(width);
        control.setMaxWidth(width);
    }

    private static void fixedWidth(Region region, double width) {
        region.setMinWidth(width);
        region.setPrefWidth(width);
        region.setMaxWidth(width);
    }

    private static void fixedSize(Region region, double width, double height) {
        region.setMinSize(width, height);
        region.setPrefSize(width, height);
        region.setMaxSize(width, height);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
