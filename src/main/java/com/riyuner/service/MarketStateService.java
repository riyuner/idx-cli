package com.riyuner.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.riyuner.util.DisplayUtil;

public class MarketStateService {
    // Market Hours Constants
    private static final int MARKET_OPEN_HOUR = 9;
    private static final int MARKET_CLOSE_HOUR = 15;
    private static final int MARKET_CLOSE_MINUTE = 30;

    // Holiday API Constants
    private static final String HOLIDAY_API_URL = "https://date.nager.at/api/v3/PublicHolidays/%d/ID";
    private static final long HOLIDAY_CACHE_DURATION = TimeUnit.HOURS.toMillis(12);

    private final Set<LocalDate> holidayCache = new HashSet<>();
    private long lastHolidayFetch = 0;
    private final boolean noColor;

    public MarketStateService(boolean noColor) {
        this.noColor = noColor;
    }

    public String getMarketStateInfo() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        
        if (isHoliday(today)) {
            return getHolidayMessage(now);
        }

        if (isWeekend(now)) {
            return getWeekendMessage(now);
        }

        if (isMarketOpen(now)) {
            return DisplayUtil.color(DisplayUtil.GREEN, "OPEN", noColor);
        }

        return getClosedMessage(now);
    }

    private void updateHolidayCache() {
        if (isCacheValid()) return;

        try {
            int currentYear = LocalDate.now().getYear();
            fetchHolidaysForYear(currentYear);
            fetchHolidaysForYear(currentYear + 1);
            lastHolidayFetch = System.currentTimeMillis();
        } catch (Exception e) {
            System.err.println(DisplayUtil.color(DisplayUtil.RED, 
                "Warning: Failed to fetch holiday data: " + e.getMessage(), noColor));
        }
    }

    private boolean isCacheValid() {
        long now = System.currentTimeMillis();
        return now - lastHolidayFetch < HOLIDAY_CACHE_DURATION && !holidayCache.isEmpty();
    }

    private void fetchHolidaysForYear(int year) {
        try {
            String url = String.format(HOLIDAY_API_URL, year);
            Document doc = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0")
                    .get();

            parseAndAddHolidays(doc.text());
        } catch (Exception e) {
            System.err.println(DisplayUtil.color(DisplayUtil.RED, 
                "Warning: Failed to fetch holidays for year " + year + ": " + e.getMessage(), noColor));
        }
    }

    private void parseAndAddHolidays(String jsonResponse) {
        String[] holidays = jsonResponse.split("\"date\":\"");
        for (int i = 1; i < holidays.length; i++) {
            try {
                String dateStr = holidays[i].substring(0, 10);
                holidayCache.add(LocalDate.parse(dateStr));
            } catch (Exception e) {
                // Skip invalid dates
            }
        }
    }

    private boolean isHoliday(LocalDate date) {
        updateHolidayCache();
        return holidayCache.contains(date);
    }

    private boolean isWeekend(LocalDateTime dateTime) {
        return dateTime.getDayOfWeek().getValue() >= 6;
    }

    private boolean isMarketOpen(LocalDateTime time) {
        int hour = time.getHour();
        int minute = time.getMinute();
        return (hour > MARKET_OPEN_HOUR || hour == MARKET_OPEN_HOUR) && 
               (hour < MARKET_CLOSE_HOUR || hour == MARKET_CLOSE_HOUR && minute <= MARKET_CLOSE_MINUTE);
    }

    private String getHolidayMessage(LocalDateTime now) {
        LocalDateTime nextWorkingDay = findNextWorkingDay(now);
        long hoursUntilOpen = java.time.Duration.between(now, nextWorkingDay).toHours();
        return DisplayUtil.color(DisplayUtil.RED, 
            "CLOSED (Holiday) - Opens in " + hoursUntilOpen + " hours", noColor);
    }

    private String getWeekendMessage(LocalDateTime now) {
        LocalDateTime nextWorkingDay = findNextWorkingDay(now);
        long hoursUntilOpen = java.time.Duration.between(now, nextWorkingDay).toHours();
        return DisplayUtil.color(DisplayUtil.RED, 
            "CLOSED (Weekend) - Opens in " + hoursUntilOpen + " hours", noColor);
    }

    private String getClosedMessage(LocalDateTime now) {
        LocalDateTime nextOpen = findNextWorkingDay(now);
        long hoursUntilOpen = java.time.Duration.between(now, nextOpen).toHours();
        long minutesUntilOpen = java.time.Duration.between(now, nextOpen).toMinutesPart();
        return DisplayUtil.color(DisplayUtil.RED, 
            "CLOSED - Opens in " + hoursUntilOpen + " hours " + minutesUntilOpen + " minutes", noColor);
    }

    private LocalDateTime findNextWorkingDay(LocalDateTime from) {
        LocalDateTime nextOpen = from;
        
        if (isAfterMarketClose(from)) {
            nextOpen = nextOpen.plusDays(1);
        }
        
        nextOpen = nextOpen.withHour(MARKET_OPEN_HOUR).withMinute(0).withSecond(0);
        
        while (isWeekend(nextOpen) || isHoliday(nextOpen.toLocalDate())) {
            nextOpen = nextOpen.plusDays(1);
        }
        
        return nextOpen;
    }

    private boolean isAfterMarketClose(LocalDateTime time) {
        return time.getHour() >= MARKET_CLOSE_HOUR && time.getMinute() > MARKET_CLOSE_MINUTE;
    }
} 