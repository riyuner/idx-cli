package com.riyuner;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.riyuner.model.StockData;
import com.riyuner.service.ChartService;
import com.riyuner.service.DisplayService;
import com.riyuner.service.MarketStateService;
import com.riyuner.service.StockDataService;
import com.riyuner.util.DisplayUtil;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@TopCommand
@Command(name = "idx", mixinStandardHelpOptions = true, version = "1.0",
        description = "CLI application for IDX stock information")
public class IdxStockCommand implements Callable<Integer> {

    @Option(names = {"-s", "--symbol"}, description = "Stock symbol (e.g., BBCA)", required = true)
    String symbol;

    @Option(names = {"-d", "--detailed"}, description = "Show detailed information")
    boolean detailed;

    @Option(names = {"-i", "--interval"}, description = "Refresh interval in seconds", defaultValue = "5")
    int interval;

    @Option(names = {"-n", "--no-color"}, description = "Disable colored output")
    boolean noColor;

    private String stockColor;
    private int terminalWidth;

    // Services
    private MarketStateService marketStateService;
    private StockDataService stockDataService;
    private ChartService chartService;
    private DisplayService displayService;

    public IdxStockCommand() {
        this.stockColor = DisplayUtil.generateRandomColor();
        this.terminalWidth = DisplayUtil.detectTerminalWidth();
    }

    private void initializeServices() {
        this.marketStateService = new MarketStateService(noColor);
        this.stockDataService = new StockDataService(noColor);
        this.chartService = new ChartService(terminalWidth, stockColor, noColor);
        this.displayService = new DisplayService(terminalWidth, stockColor, noColor, 
            marketStateService, stockDataService);
    }

    private void fetchAndDisplayData() {
        final StringBuilder display = new StringBuilder();
        displayService.updateDisplay(display, symbol);
        
        try {
            StockData stockData = stockDataService.fetchStockData(symbol);
            updateDisplayWithStockData(display, stockData);
            System.out.print(display);
        } catch (Exception e) {
            System.err.println(DisplayUtil.color(DisplayUtil.RED, 
                "Error updating data: " + e.getMessage(), noColor));
        }
    }

    private void updateDisplayWithStockData(StringBuilder display, StockData stockData) {
        displayService.getCurrencyDisplay(display, stockData);
        
        if (detailed) {
            displayService.displayDetailedInfo(display);
        }

        if (stockDataService.hasEnoughData()) {
            display.append("Price Chart (Last 30 updates):\n");
            display.append(chartService.generateChart(
                stockDataService.getPriceHistory(), 
                stockDataService.getTimeHistory()));
            display.append("\n");
        }
    }

    @Override
    public Integer call() {
        try {
            if (!symbol.endsWith(":IDX")) {
                symbol = symbol + ":IDX";
            }

            initializeServices();

            System.out.print("\u001B[?1049h");
            System.out.println(DisplayUtil.color(DisplayUtil.YELLOW, 
                "Starting live data feed... Press Ctrl+C to exit", noColor));
            TimeUnit.SECONDS.sleep(1);

            while (true) {
                try {
                    fetchAndDisplayData();
                } catch (Exception e) {
                    System.err.println(DisplayUtil.color(DisplayUtil.RED, 
                        "Error updating data: " + e.getMessage(), noColor));
                }
                TimeUnit.SECONDS.sleep(interval);
            }

        } catch (Exception e) {
            System.err.println(DisplayUtil.color(DisplayUtil.RED, 
                "Fatal error: " + e.getMessage(), noColor));
            return 1;
        } finally {
            System.out.print("\u001B[?1049l");
        }
    }
} 