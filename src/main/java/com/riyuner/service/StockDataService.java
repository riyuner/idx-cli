package com.riyuner.service;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.riyuner.model.StockData;
import com.riyuner.util.DisplayUtil;

public class StockDataService {
    private static final String GOOGLE_FINANCE_URL = "https://www.google.com/finance/quote/";
    private static final int MAX_HISTORY_SIZE = 30;

    private final List<Double> priceHistory = new ArrayList<>();
    private final List<LocalDateTime> timeHistory = new ArrayList<>();
    private double previousPrice = 0;
    private boolean hasRealData = false;
    private final boolean noColor;

    public StockDataService(boolean noColor) {
        this.noColor = noColor;
    }

    public StockData fetchStockData(String symbol) throws IOException {
        String currentSymbol = symbol.endsWith(":IDX") ? symbol : symbol + ":IDX";
        String url = GOOGLE_FINANCE_URL + currentSymbol;
        
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .get();

        Elements priceElement = doc.select("div[data-last-price]");
        if (priceElement.isEmpty()) {
            throw new IOException("Stock not found: " + symbol);
        }

        double price = Double.parseDouble(priceElement.attr("data-last-price"));
        String change = extractPriceChange(doc);
        
        updatePriceHistory(price);
        
        return new StockData(price, change);
    }

    private String extractPriceChange(Document doc) {
        Elements changeElements = doc.select("div.YMlKec.vpf-qc");
        Elements percentElements = doc.select("div.JwB6zf.vpf-qc");
        
        if (!changeElements.isEmpty() && !percentElements.isEmpty()) {
            return Objects.requireNonNull(changeElements.first()).text() +
                   " (" + Objects.requireNonNull(percentElements.first()).text() + ")";
        }
        return "";
    }

    public void updatePriceHistory(double price) {
        if (!hasRealData && priceHistory.size() >= 2) {
            priceHistory.clear();
            timeHistory.clear();
            hasRealData = true;
        }

        priceHistory.add(price);
        timeHistory.add(LocalDateTime.now());
        if (priceHistory.size() > MAX_HISTORY_SIZE) {
            priceHistory.removeFirst();
            timeHistory.removeFirst();
        }
        previousPrice = price;
    }

    public String determinePriceColor(double currentPrice) {
        return previousPrice == 0 ? DisplayUtil.RESET : 
               (currentPrice >= previousPrice ? DisplayUtil.GREEN : DisplayUtil.RED);
    }

    public String formatCurrency(double amount) {
        return NumberFormat.getInstance(Locale.of("id", "ID")).format(amount);
    }

    public List<Double> getPriceHistory() {
        return priceHistory;
    }

    public List<LocalDateTime> getTimeHistory() {
        return timeHistory;
    }

    public boolean hasEnoughData() {
        return priceHistory.size() >= 2;
    }
} 