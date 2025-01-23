package com.riyuner.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.riyuner.model.StockData;
import com.riyuner.util.DisplayUtil;

public class DisplayService {
    private final int terminalWidth;
    private final String stockColor;
    private final boolean noColor;
    private final MarketStateService marketStateService;
    private final StockDataService stockDataService;
    private boolean isFirstRun = true;

    public DisplayService(int terminalWidth, String stockColor, boolean noColor, 
                         MarketStateService marketStateService, StockDataService stockDataService) {
        this.terminalWidth = terminalWidth;
        this.stockColor = stockColor;
        this.noColor = noColor;
        this.marketStateService = marketStateService;
        this.stockDataService = stockDataService;
    }

    public void printHeader(StringBuilder buffer, String symbol) {
        String separator = "=".repeat(terminalWidth);
        String title = formatTitle(symbol);
        String timestamp = formatTimestamp();
        String marketState = "Market: " + marketStateService.getMarketStateInfo();
        
        buffer.append(DisplayUtil.color(stockColor, separator, noColor)).append("\n")
              .append(DisplayUtil.color(stockColor, title, noColor)).append("\n")
              .append(DisplayUtil.color(stockColor, timestamp, noColor)).append("  ")
              .append(marketState).append("\n")
              .append(DisplayUtil.color(stockColor, separator, noColor)).append("\n");
    }

    private String formatTitle(String symbol) {
        String title = "=== " + symbol.replace(":IDX", "") + " Live Trading Data ===";
        int leftPadding = (terminalWidth - title.length()) / 2;
        return " ".repeat(Math.max(0, leftPadding)) + title;
    }

    private String formatTimestamp() {
        return "Time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public void getCurrencyDisplay(StringBuilder display, StockData stockData) {
        String priceColor = stockDataService.determinePriceColor(stockData.getPrice());
        display.append(DisplayUtil.CLEAR_LINE)
               .append(DisplayUtil.color(priceColor, 
                   "Price: Rp " + stockDataService.formatCurrency(stockData.getPrice()), noColor))
               .append("\n");
        
        if (!stockData.getChange().isEmpty()) {
            display.append(DisplayUtil.CLEAR_LINE)
                   .append(DisplayUtil.color(stockData.getChange().contains("+") ? 
                       DisplayUtil.GREEN : DisplayUtil.RED, 
                       "Change: " + stockData.getChange(), noColor))
                   .append("\n\n");
        }
    }

    public void updateDisplay(StringBuilder display, String symbol) {
        if (isFirstRun) {
            DisplayUtil.clearScreen();
            isFirstRun = false;
        } else {
            display.append(DisplayUtil.CURSOR_HOME);
        }
        printHeader(display, symbol);
    }

    public void displayDetailedInfo(StringBuilder display) {
        // Implementation for detailed information display
        // This would be implemented based on your needs
    }
} 