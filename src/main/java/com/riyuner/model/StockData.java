package com.riyuner.model;

public class StockData {
    private final double price;
    private final String change;

    public StockData(double price, String change) {
        this.price = price;
        this.change = change;
    }

    public double getPrice() {
        return price;
    }

    public String getChange() {
        return change;
    }
} 