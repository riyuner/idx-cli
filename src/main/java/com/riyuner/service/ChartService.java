package com.riyuner.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.riyuner.util.DisplayUtil;

public class ChartService {
    private static final int CHART_HEIGHT = 10;
    
    private final int terminalWidth;
    private final String stockColor;
    private final boolean noColor;

    public ChartService(int terminalWidth, String stockColor, boolean noColor) {
        this.terminalWidth = terminalWidth;
        this.stockColor = stockColor;
        this.noColor = noColor;
    }

    public String generateChart(List<Double> priceHistory, List<LocalDateTime> timeHistory) {
        if (priceHistory.size() < 2) return "Collecting data...";

        double min = priceHistory.stream().mapToDouble(v -> v).min().getAsDouble();
        double max = priceHistory.stream().mapToDouble(v -> v).max().getAsDouble();
        double range = max - min;
        
        int chartWidth = terminalWidth - 10;
        int dataPoints = Math.min(priceHistory.size(), chartWidth);
        
        char[][] grid = initializeChartGrid(dataPoints);
        plotChartPoints(grid, dataPoints, priceHistory, min, range);
        
        return buildChartString(grid, dataPoints, min, range, timeHistory);
    }

    private char[][] initializeChartGrid(int dataPoints) {
        char[][] grid = new char[CHART_HEIGHT][dataPoints];
        for (int i = 0; i < CHART_HEIGHT; i++) {
            for (int j = 0; j < dataPoints; j++) {
                grid[i][j] = ' ';
            }
        }
        return grid;
    }

    private void plotChartPoints(char[][] grid, int dataPoints, List<Double> priceHistory, double min, double range) {
        for (int i = 0; i < dataPoints; i++) {
            int historyIndex = priceHistory.size() - dataPoints + i;
            double price = priceHistory.get(historyIndex);
            int y = (int) ((CHART_HEIGHT - 1) * (price - min) / range);
            grid[y][i] = '•';
            
            if (i > 0) {
                plotTrendLine(grid, i, historyIndex, y, priceHistory, min, range);
            }
        }
    }

    private void plotTrendLine(char[][] grid, int i, int historyIndex, int y, List<Double> priceHistory, double min, double range) {
        double prevPrice = priceHistory.get(historyIndex - 1);
        int prevY = (int) ((CHART_HEIGHT - 1) * (prevPrice - min) / range);
        int startY = Math.min(y, prevY);
        int endY = Math.max(y, prevY);
        for (int j = startY; j <= endY; j++) {
            if (grid[j][i] == ' ') grid[j][i] = '│';
        }
    }

    private String buildChartString(char[][] grid, int dataPoints, double min, double range, List<LocalDateTime> timeHistory) {
        StringBuilder chart = new StringBuilder();
        buildChartBody(chart, grid, dataPoints, min, range);
        buildChartAxis(chart, dataPoints);
        buildTimeMarkers(chart, dataPoints, timeHistory);
        return chart.toString();
    }

    private void buildChartBody(StringBuilder chart, char[][] grid, int dataPoints, double min, double range) {
        for (int i = CHART_HEIGHT - 1; i >= 0; i--) {
            String priceString = String.format("%.0f", min + (range * i / (CHART_HEIGHT - 1)));
            chart.append(DisplayUtil.color(DisplayUtil.BLUE, String.format("%8s |", priceString), noColor));
            for (int j = 0; j < dataPoints; j++) {
                chart.append(formatChartSymbol(grid[i][j]));
            }
            chart.append("\n");
        }
    }

    private String formatChartSymbol(char c) {
        if (c == '•' || c == '│') {
            return DisplayUtil.color(stockColor, String.valueOf(c), noColor);
        }
        return String.valueOf(c);
    }

    private void buildChartAxis(StringBuilder chart, int dataPoints) {
        chart.append("         ");
        chart.append(DisplayUtil.color(DisplayUtil.BLUE, String.join("", "-".repeat(dataPoints)), noColor));
        chart.append("\n         ");
    }

    private void buildTimeMarkers(StringBuilder chart, int dataPoints, List<LocalDateTime> timeHistory) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        int timeMarkInterval = Math.max(1, dataPoints / 5);
        
        for (int i = 0; i < dataPoints; i++) {
            if (i % timeMarkInterval == 0) {
                int historyIndex = timeHistory.size() - dataPoints + i;
                String time = timeHistory.get(historyIndex).format(formatter);
                chart.append(DisplayUtil.color(DisplayUtil.YELLOW, "|", noColor));
                chart.append(DisplayUtil.color(DisplayUtil.BLUE, time, noColor));
                i += time.length() - 1;
            } else {
                chart.append(" ");
            }
        }
    }
} 