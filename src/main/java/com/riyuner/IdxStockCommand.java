package com.riyuner;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@TopCommand
@Command(name = "idx", mixinStandardHelpOptions = true, version = "1.0",
        description = "CLI application for IDX stock information")
public class IdxStockCommand implements Callable<Integer> {

    // Remove unused color constants
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String BLUE = "\u001B[34m";
    private static final String YELLOW = "\u001B[33m";

    // Add ANSI escape codes for cursor movement
    private static final String CLEAR_LINE = "\u001B[2K";    // Clear entire line
    private static final String CURSOR_HOME = "\u001B[H";    // Move cursor to top-left

    private final Random random;
    private final String stockColor; // Store single stock color

    private static final String GOOGLE_FINANCE_URL = "https://www.google.com/finance/quote/";
    private static final int MAX_HISTORY_SIZE = 30; // For chart display
    private static final int CHART_HEIGHT = 10;
    
    // Add market hours constants
    private static final int MARKET_OPEN_HOUR = 9;
    private static final int MARKET_CLOSE_HOUR = 15;
    private static final int MARKET_CLOSE_MINUTE = 30;

    // Add holiday constants and cache
    private static final String HOLIDAY_API_URL = "https://date.nager.at/api/v3/PublicHolidays/%d/ID";
    private static final long HOLIDAY_CACHE_DURATION = TimeUnit.HOURS.toMillis(12);
    private Set<LocalDate> holidayCache = new HashSet<>();
    private long lastHolidayFetch = 0;

    @Option(names = {"-s", "--symbol"}, description = "Stock symbol (e.g., BBCA)", required = true)
    String symbol;

    @Option(names = {"-d", "--detailed"}, description = "Show detailed information")
    boolean detailed;

    @Option(names = {"-i", "--interval"}, description = "Refresh interval in seconds", defaultValue = "5")
    int interval;

    @Option(names = {"-n", "--no-color"}, description = "Disable colored output")
    boolean noColor;

    private boolean isFirstRun = true;
    private int terminalWidth = 80; // Default terminal width

    private boolean hasRealData = false; // Track first price chart data
    private boolean hasDetailedInfo = false; // Track first detailed info

    public IdxStockCommand() {
        this.random = new Random();
        this.stockColor = generateRandomColor();
        // Get terminal width using stty command
        try {
            Process process = new ProcessBuilder("stty", "size").inheritIO().redirectInput(ProcessBuilder.Redirect.INHERIT).redirectOutput(ProcessBuilder.Redirect.PIPE).start();
            String[] output = new String(process.getInputStream().readAllBytes()).trim().split(" ");
            if (output.length >= 2) {
                terminalWidth = Integer.parseInt(output[1]);
            }
        } catch (Exception e) {
            // Fallback to default width if detection fails
        }
    }

    private String color(String color, String text) {
        return noColor ? text : color + text + RESET;
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private String generateRandomColor() {
        int r = this.random.nextInt(156) + 100;
        int g = this.random.nextInt(156) + 100;
        int b = this.random.nextInt(156) + 100;
        return String.format("\u001B[38;2;%d;%d;%dm", r, g, b);
    }

    private String generateChart() {
        if (priceHistory.size() < 2) return "Collecting data...";

        double min = priceHistory.stream().mapToDouble(v -> v).min().getAsDouble();
        double max = priceHistory.stream().mapToDouble(v -> v).max().getAsDouble();
        double range = max - min;
        
        // Calculate available width for the chart (accounting for price labels)
        int chartWidth = terminalWidth - 10; // Leave space for price labels
        int dataPoints = Math.min(priceHistory.size(), chartWidth);
        
        StringBuilder chart = new StringBuilder();
        char[][] grid = new char[CHART_HEIGHT][dataPoints];
        
        // Initialize grid with spaces
        for (int i = 0; i < CHART_HEIGHT; i++) {
            for (int j = 0; j < dataPoints; j++) {
                grid[i][j] = ' ';
            }
        }

        // Plot points and trend lines with adjusted width
        for (int i = 0; i < dataPoints; i++) {
            int historyIndex = priceHistory.size() - dataPoints + i;
            double price = priceHistory.get(historyIndex);
            int y = (int) ((CHART_HEIGHT - 1) * (price - min) / range);
            grid[y][i] = '•';
            
            if (i > 0) {
                double prevPrice = priceHistory.get(historyIndex - 1);
                int prevY = (int) ((CHART_HEIGHT - 1) * (prevPrice - min) / range);
                int startY = Math.min(y, prevY);
                int endY = Math.max(y, prevY);
                for (int j = startY; j <= endY; j++) {
                    if (grid[j][i] == ' ') grid[j][i] = '│';
                }
            }
        }

        // Build the chart string with colors
        for (int i = CHART_HEIGHT - 1; i >= 0; i--) {
            String priceString = String.format("%.0f", min + (range * i / (CHART_HEIGHT - 1)));
            chart.append(color(BLUE, String.format("%8s |", priceString)));
            for (int j = 0; j < dataPoints; j++) {
                char c = grid[i][j];
                String chartSymbol = String.valueOf(c);
                if (c == '•') {
                    chartSymbol = color(stockColor, "•");
                } else if (c == '│') {
                    chartSymbol = color(stockColor, "│");
                }
                chart.append(chartSymbol);
            }
            chart.append("\n");
        }

        // Add time axis with adjusted width
        chart.append("         ");
        chart.append(color(BLUE, String.join("", "-".repeat(dataPoints))));
        chart.append("\n         ");
        
        // Add time markers with adjusted spacing
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        int timeMarkInterval = Math.max(1, dataPoints / 5); // Show 5 time markers
        for (int i = 0; i < dataPoints; i++) {
            if (i % timeMarkInterval == 0) {
                int historyIndex = timeHistory.size() - dataPoints + i;
                String time = timeHistory.get(historyIndex).format(formatter);
                chart.append(color(YELLOW, "|"));
                chart.append(color(BLUE, time));
                i += time.length() - 1;
            } else {
                chart.append(" ");
            }
        }

        return chart.toString();
    }

    private final List<Double> priceHistory = new ArrayList<>();
    private final List<LocalDateTime> timeHistory = new ArrayList<>();
    private double previousPrice = 0;

    // Add new method to fetch and parse holidays
    private void updateHolidayCache() {
        long now = System.currentTimeMillis();
        if (now - lastHolidayFetch < HOLIDAY_CACHE_DURATION && !holidayCache.isEmpty()) {
            return; // Use cached data if it's fresh
        }

        try {
            // Get holidays for current year and next year to handle year transitions. The next year is specifically
            // to handle end of year actually, but i was to lazy for doing it the proper way
            int currentYear = LocalDate.now().getYear();
            fetchHolidaysForYear(currentYear);
            fetchHolidaysForYear(currentYear + 1);
            
            lastHolidayFetch = now;
        } catch (Exception e) {
            System.err.println(color(RED, "Warning: Failed to fetch holiday data: " + e.getMessage()));
        }
    }

    private void fetchHolidaysForYear(int year) {
        try {
            String url = String.format(HOLIDAY_API_URL, year);
            Document doc = Jsoup.connect(url)
                    .ignoreContentType(true) // Required for JSON response
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .get();

            // Parse JSON response
            String jsonResponse = doc.text();
            // Split the JSON array by date field
            String[] holidays = jsonResponse.split("\"date\":\"");
            
            for (int i = 1; i < holidays.length; i++) {
                try {
                    String dateStr = holidays[i].substring(0, 10); // Extract YYYY-MM-DD
                    LocalDate holidayDate = LocalDate.parse(dateStr);
                    holidayCache.add(holidayDate);
                } catch (Exception e) {
                    // Skip invalid dates
                }
            }
        } catch (Exception e) {
            System.err.println(color(RED, "Warning: Failed to fetch holidays for year " + year + ": " + e.getMessage()));
        }
    }

    // Add method to check if date is a holiday
    private boolean isHoliday(LocalDate date) {
        updateHolidayCache();
        return holidayCache.contains(date);
    }

    private String getMarketStateInfo() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        
        // Check for holiday first
        if (isHoliday(today)) {
            LocalDateTime nextWorkingDay = findNextWorkingDay(now);
            long hoursUntilOpen = java.time.Duration.between(now, nextWorkingDay).toHours();
            return color(RED, "CLOSED (Holiday) - Opens in " + hoursUntilOpen + " hours");
        }

        boolean isWeekend = now.getDayOfWeek().getValue() >= 6; // Saturday or Sunday
        
        if (isWeekend) {
            LocalDateTime nextWorkingDay = findNextWorkingDay(now);
            long hoursUntilOpen = java.time.Duration.between(now, nextWorkingDay).toHours();
            return color(RED, "CLOSED (Weekend) - Opens in " + hoursUntilOpen + " hours");
        }

        int hour = now.getHour();
        int minute = now.getMinute();
        boolean isMarketOpen = (hour > MARKET_OPEN_HOUR || hour == MARKET_OPEN_HOUR) && 
                             (hour < MARKET_CLOSE_HOUR || hour == MARKET_CLOSE_HOUR && minute <= MARKET_CLOSE_MINUTE);

        if (isMarketOpen) {
            return color(GREEN, "OPEN");
        } else {
            LocalDateTime nextOpen = findNextWorkingDay(now);
            long hoursUntilOpen = java.time.Duration.between(now, nextOpen).toHours();
            long minutesUntilOpen = java.time.Duration.between(now, nextOpen).toMinutesPart();
            return color(RED, "CLOSED - Opens in " + hoursUntilOpen + " hours " + minutesUntilOpen + " minutes");
        }
    }

    // Add helper method to find next working day
    private LocalDateTime findNextWorkingDay(LocalDateTime from) {
        LocalDateTime nextOpen = from;
        
        // If we're after market close, start checking from next day
        if (from.getHour() >= MARKET_CLOSE_HOUR && from.getMinute() > MARKET_CLOSE_MINUTE) {
            nextOpen = nextOpen.plusDays(1);
        }
        
        // Set to market opening time
        nextOpen = nextOpen.withHour(MARKET_OPEN_HOUR).withMinute(0).withSecond(0);
        
        // Keep checking until we find a working day
        while (nextOpen.getDayOfWeek().getValue() >= 6 || isHoliday(nextOpen.toLocalDate())) {
            nextOpen = nextOpen.plusDays(1);
        }
        
        return nextOpen;
    }

    private void printHeader(StringBuilder buffer) {
        String separator = "=".repeat(terminalWidth);
        String title = "=== " + symbol.replace(":IDX", "") + " Live Trading Data ===";
        String timestamp = "Time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String marketState = "Market: " + getMarketStateInfo();
        
        // Center the title
        int leftPadding = (terminalWidth - title.length()) / 2;
        String centeredTitle = " ".repeat(Math.max(0, leftPadding)) + title;
        
        buffer.append(color(stockColor, separator)).append("\n")
              .append(color(stockColor, centeredTitle)).append("\n")
              .append(color(stockColor, timestamp)).append("  ")
              .append(marketState).append("\n")
              .append(color(stockColor, separator)).append("\n");
    }

    private void fetchAndDisplayData() {
        final StringBuilder display = new StringBuilder();
        
        if (isFirstRun) {
            clearScreen();
            isFirstRun = false;
        } else {
            // Move cursor to top
            display.append(CURSOR_HOME);
        }

        // Build the complete display buffer
        printHeader(display);
        
        String currentSymbol = symbol.endsWith(":IDX") ? symbol : symbol + ":IDX";
        String url = GOOGLE_FINANCE_URL + currentSymbol;
        
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .get();

            Elements priceElement = doc.select("div[data-last-price]");
            Elements changeElements = doc.select("div.YMlKec.vpf-qc");
            Elements percentElements = doc.select("div.JwB6zf.vpf-qc");

            if (priceElement.isEmpty()) {
                throw new IOException("Stock not found: " + symbol);
            }

            double price = Double.parseDouble(priceElement.attr("data-last-price"));
            String change = "";
            if (!changeElements.isEmpty() && !percentElements.isEmpty()) {
                change = Objects.requireNonNull(changeElements.first()).text() +
                        " (" + Objects.requireNonNull(percentElements.first()).text() + ")";
            }

            // Add price and change to display buffer
            String priceColor = previousPrice == 0 ? RESET : (price >= previousPrice ? GREEN : RED);
            getCurrencyDisplay(display, price, change, priceColor);
            previousPrice = price;

            // Handle price chart data
            if (!hasRealData && priceHistory.size() >= 2) {
                priceHistory.clear();
                timeHistory.clear();
                hasRealData = true;
                clearScreen();
                display.setLength(0);
                printHeader(display);
                // Re-add price and change
                getCurrencyDisplay(display, price, change, priceColor);
            }

            // Update price history
            priceHistory.add(price);
            timeHistory.add(LocalDateTime.now());
            if (priceHistory.size() > MAX_HISTORY_SIZE) {
                priceHistory.removeFirst();
                timeHistory.removeFirst();
            }

            // Handle detailed information if enabled
            if (detailed) {
                Elements detailRows = doc.select("div.gyFHrc");
                if (!detailRows.isEmpty()) {
                    if (!hasDetailedInfo) {
                        hasDetailedInfo = true;
                        clearScreen();
                        display.setLength(0);
                        printHeader(display);
                        // Re-add price and change
                        getCurrencyDisplay(display, price, change, priceColor);
                    }
                    display.append(color(YELLOW, "Detailed Information:\n"));
                    detailRows.forEach(row -> {
                        Elements label = row.select("div.mfs7Fc");
                        Elements value = row.select("div.P6K39c");
                        if (!label.isEmpty() && !value.isEmpty()) {
                            display.append(String.format("%s%-20s:%s %s%n", 
                                BLUE, label.text(), RESET,
                                value.text()));
                        }
                    });
                    display.append("\n");
                }
            }

            // Show chart if we have data points
            if (priceHistory.size() >= 2) {
                display.append("Price Chart (Last " + MAX_HISTORY_SIZE + " updates):\n");
                display.append(generateChart());
                display.append("\n");
            }

            // Print the complete buffer
            System.out.print(display);

        } catch (Exception e) {
            System.err.println(color(RED, "Error updating data: " + e.getMessage()));
        }
    }

    private void getCurrencyDisplay(StringBuilder display, double price, String change, String priceColor) {
        display.append(CLEAR_LINE).append(color(priceColor, "Price: Rp " +
                NumberFormat.getInstance(Locale.of("id", "ID")).format(price))).append("\n");
        if (!change.isEmpty()) {
            display.append(CLEAR_LINE).append(color(change.contains("+") ? GREEN :
                    RED, "Change: " + change)).append("\n\n");
        }
    }

    @Override
    public Integer call() {
        try {
            if (!symbol.endsWith(":IDX")) {
                symbol = symbol + ":IDX";
            }

            // Enable alternative buffer to prevent scrolling
            System.out.print("\u001B[?1049h");
            System.out.println(color(YELLOW, "Starting live data feed... Press Ctrl+C to exit"));
            TimeUnit.SECONDS.sleep(1);

            // Looping the main code
            while (true) {
                try {
                    fetchAndDisplayData();
                } catch (Exception e) {
                    System.err.println(color(RED, "Error updating data: " + e.getMessage()));
                }
                TimeUnit.SECONDS.sleep(interval);
            }

        } catch (Exception e) {
            System.err.println(color(RED, "Fatal error: " + e.getMessage()));
            return 1;
        } finally {
            // Restore main buffer
            System.out.print("\u001B[?1049l");
        }
    }
} 