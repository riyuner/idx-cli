package com.riyuner.util;

import java.util.Random;

public class DisplayUtil {
    // ANSI Color Constants
    public static final String RESET = "\u001B[0m";
    public static final String GREEN = "\u001B[32m";
    public static final String RED = "\u001B[31m";
    public static final String BLUE = "\u001B[34m";
    public static final String YELLOW = "\u001B[33m";

    // Terminal Control Constants
    public static final String CLEAR_LINE = "\u001B[2K";
    public static final String CURSOR_HOME = "\u001B[H";

    private static final Random random = new Random();

    public static String color(String color, String text, boolean noColor) {
        return noColor ? text : color + text + RESET;
    }

    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public static String generateRandomColor() {
        int r = random.nextInt(156) + 100;
        int g = random.nextInt(156) + 100;
        int b = random.nextInt(156) + 100;
        return String.format("\u001B[38;2;%d;%d;%dm", r, g, b);
    }

    public static int detectTerminalWidth() {
        int defaultWidth = 80;
        try {
            Process process = new ProcessBuilder("stty", "size")
                    .inheritIO()
                    .redirectInput(ProcessBuilder.Redirect.INHERIT)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .start();
            String[] output = new String(process.getInputStream().readAllBytes()).trim().split(" ");
            if (output.length >= 2) {
                return Integer.parseInt(output[1]);
            }
        } catch (Exception e) {
            // Fallback to default width if detection fails
        }
        return defaultWidth;
    }
} 