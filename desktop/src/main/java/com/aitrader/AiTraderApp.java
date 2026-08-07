package com.aitrader;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class AiTraderApp extends Application {
    private final TradingApiClient api=new TradingApiClient();
    private final ComboBox<String> exchange=new ComboBox<>(),symbol=new ComboBox<>(),timeframe=new ComboBox<>();
    private final Label capital=value("—"),ret=value("—"),buyHold=value("—"),dd=value("—"),win=value("—");
    private final Label price=new Label("—"),trend=new Label("—"),rsi=new Label("—"),signal=new Label("Sin análisis"),explanation=new Label("Ejecutá el análisis para consultar datos reales."),status=new Label("Backend: sin consultar");
    private final LineChart<Number,Number> priceChart=chart("Precio"),equityChart=chart("Capital");
    private final TableView<TradeRow> trades=table(); private final Button analyze=new Button("Analizar mercado");

    @Override public void start(Stage stage){
        exchange.getItems().addAll("binance","kraken"); exchange.setValue("binance");
        symbol.getItems().addAll("BTC/USDT","ETH/USDT"); symbol.setValue("BTC/USDT");
        timeframe.getItems().addAll("1h","4h","1d"); timeframe.setValue("1h");
        BorderPane root=new BorderPane(); root.getStyleClass().add("app-root"); root.setTop(top());
        VBox page=new VBox(14); page.setPadding(new Insets(18));
        HBox metrics=new HBox(10,card("CAPITAL FINAL",capital),card("RETORNO",ret),card("BUY & HOLD",buyHold),card("DRAWDOWN MÁX.",dd),card("WIN RATE",win)); metrics.getChildren().forEach(n->HBox.setHgrow(n,Priority.ALWAYS));
        SplitPane charts=new SplitPane(panel("Precio real",priceChart),panel("Curva de capital",equityChart)); charts.setDividerPositions(.5);
        SplitPane bottom=new SplitPane(panel("Operaciones del backtest",trades),analysisPanel()); bottom.setDividerPositions(.70);
        page.getChildren().addAll(metrics,charts,bottom); VBox.setVgrow(charts,Priority.ALWAYS); VBox.setVgrow(bottom,Priority.ALWAYS); root.setCenter(page);
        Scene scene=new Scene(root,1380,900); scene.getStylesheets().add(AiTraderApp.class.getResource("/com/aitrader/theme.css").toExternalForm());
        stage.setTitle("AI Trader v0.2"); stage.setMinWidth(1100); stage.setMinHeight(760); stage.setScene(scene); stage.show();
    }
    private HBox top(){Label brand=new Label("AI TRADER");brand.getStyleClass().add("brand");Label mode=new Label("ANÁLISIS / PAPER");mode.getStyleClass().add("paper-badge");analyze.getStyleClass().add("primary-button");analyze.setOnAction(e->run());Region sp=new Region();HBox.setHgrow(sp,Priority.ALWAYS);HBox h=new HBox(10,brand,sp,exchange,symbol,timeframe,mode,analyze);h.setPadding(new Insets(12,18,12,18));h.setAlignment(Pos.CENTER_LEFT);h.getStyleClass().add("top-bar");return h;}
    private VBox analysisPanel(){Label t=new Label("Análisis técnico");t.getStyleClass().add("panel-title");Label w=new Label("NO ES IA TODAVÍA");w.getStyleClass().add("warning-badge");price.getStyleClass().add("large-value");signal.getStyleClass().add("signal");explanation.setWrapText(true);explanation.getStyleClass().add("description");status.getStyleClass().add("muted");GridPane g=new GridPane();g.setHgap(12);g.setVgap(8);g.addRow(0,muted("Último precio"),price);g.addRow(1,muted("Tendencia"),trend);g.addRow(2,muted("RSI 14"),rsi);VBox v=new VBox(12,t,w,g,new Separator(),muted("Señal por reglas"),signal,explanation,new Separator(),status);v.setPadding(new Insets(16));v.getStyleClass().add("panel");return v;}
    private void run(){analyze.setDisable(true);analyze.setText("Consultando...");status.setText("Descargando velas reales...");Task<JsonNode> task=new Task<>(){protected JsonNode call() throws Exception{return api.analyze(exchange.getValue(),symbol.getValue(),timeframe.getValue());}};task.setOnSucceeded(e->{render(task.getValue());analyze.setDisable(false);analyze.setText("Analizar mercado");});task.setOnFailed(e->{status.setText("ERROR: "+task.getException().getMessage());analyze.setDisable(false);analyze.setText("Analizar mercado");});Thread th=new Thread(task,"market-analysis");th.setDaemon(true);th.start();}
    private void render(JsonNode root){JsonNode m=root.path("metrics"),t=root.path("technical");capital.setText(money(m.path("capital_final").asDouble()));ret.setText(pct(m.path("return_pct").asDouble()));buyHold.setText(pct(m.path("buy_hold_return_pct").asDouble()));dd.setText(pct(m.path("max_drawdown_pct").asDouble()));win.setText(pct(m.path("win_rate_pct").asDouble()));price.setText(money(t.path("last_price").asDouble()));trend.setText(t.path("trend").asText());rsi.setText(t.path("rsi14").isNull()?"—":String.format("%.2f",t.path("rsi14").asDouble()));signal.setText(t.path("signal").asText());explanation.setText(t.path("explanation").asText());fill(priceChart,root.path("prices"),"close");fill(equityChart,root.path("equity"),"equity");fillTrades(root.path("trades"));status.setText("Datos reales: "+root.path("exchange").asText()+" · "+root.path("symbol").asText()+" · "+root.path("timeframe").asText());}
    private void fill(LineChart<Number,Number> c,JsonNode a,String f){XYChart.Series<Number,Number> s=new XYChart.Series<>();int i=0;for(JsonNode r:a)s.getData().add(new XYChart.Data<>(i++,r.path(f).asDouble()));c.getData().setAll(s);}
    private void fillTrades(JsonNode a){trades.getItems().clear();DateTimeFormatter fmt=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());for(JsonNode r:a){trades.getItems().add(new TradeRow(fmt.format(Instant.ofEpochMilli(r.path("exit_time").asLong())),r.path("entry_price").asDouble(),r.path("exit_price").asDouble(),r.path("pnl").asDouble(),r.path("reason").asText()));}}
    private static String money(double v){return String.format("$ %,.2f",v);} private static String pct(double v){return String.format("%+.2f%%",v);} private static Label muted(String s){Label l=new Label(s);l.getStyleClass().add("muted");return l;} private static Label value(String s){Label l=new Label(s);l.getStyleClass().add("metric-value");return l;}
    private VBox card(String t,Label v){VBox b=new VBox(6,muted(t),v);b.setPadding(new Insets(14));b.getStyleClass().add("metric-card");b.setMaxWidth(Double.MAX_VALUE);return b;} private VBox panel(String t,javafx.scene.Node n){Label l=new Label(t);l.getStyleClass().add("panel-title");VBox b=new VBox(8,l,n);b.setPadding(new Insets(14));b.getStyleClass().add("panel");VBox.setVgrow(n,Priority.ALWAYS);return b;}
    private static LineChart<Number,Number> chart(String yLabel){NumberAxis x=new NumberAxis();x.setForceZeroInRange(false);x.setTickLabelsVisible(false);NumberAxis y=new NumberAxis();y.setForceZeroInRange(false);y.setLabel(yLabel);LineChart<Number,Number> c=new LineChart<>(x,y);c.setLegendVisible(false);c.setAnimated(false);c.setCreateSymbols(false);return c;}
    private TableView<TradeRow> table(){TableView<TradeRow> t=new TableView<>();TableColumn<TradeRow,String> time=new TableColumn<>("Salida");time.setCellValueFactory(v->v.getValue().timeProperty());TableColumn<TradeRow,Number> entry=new TableColumn<>("Entrada");entry.setCellValueFactory(v->v.getValue().entryProperty());TableColumn<TradeRow,Number> exit=new TableColumn<>("Salida $");exit.setCellValueFactory(v->v.getValue().exitProperty());TableColumn<TradeRow,Number> pnl=new TableColumn<>("P&L");pnl.setCellValueFactory(v->v.getValue().pnlProperty());TableColumn<TradeRow,String> reason=new TableColumn<>("Motivo");reason.setCellValueFactory(v->v.getValue().reasonProperty());t.getColumns().addAll(time,entry,exit,pnl,reason);t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);return t;}
    public static void main(String[] args){launch(args);}
}
