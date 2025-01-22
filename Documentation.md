# IDX Stock CLI Documentation

This document provides detailed information about the codebase structure, functionality, and implementation details of the IDX Stock CLI application.

## Table of Contents
- [Code Structure](#code-structure)
- [Main Components](#main-components)
- [Core Functionality](#core-functionality)
- [UI Components](#ui-components)
- [Data Management](#data-management)

## Code Structure

The application is built using Quarkus and follows a command-line interface pattern using Picocli. The main class `IdxStockCommand` handles all the core functionality.

### Main Class: IdxStockCommand

```java
@TopCommand
@Command(name = "idx", mixinStandardHelpOptions = true, version = "1.0")
public class IdxStockCommand implements Callable<Integer>
```

This is the entry point of the application, implementing Picocli's `Callable` interface for command-line processing.

## Main Components

### Command Options

```java
@Option(names = {"-s", "--symbol"}, description = "Stock symbol (e.g., BBCA)", required = true)
String symbol;

@Option(names = {"-d", "--detailed"}, description = "Show detailed information")
boolean detailed;

@Option(names = {"-i", "--interval"}, description = "Refresh interval in seconds", defaultValue = "5")
int interval;

@Option(names = {"-n", "--no-color"}, description = "Disable colored output")
boolean noColor;
```

These fields define the command-line options available to users.

### Market Hours Configuration

```java
private static final int MARKET_OPEN_HOUR = 9;
private static final int MARKET_CLOSE_HOUR = 15;
private static final int MARKET_CLOSE_MINUTE = 30;
```

Constants defining IDX market hours (09:00-15:30 WIB, Monday-Friday).

## Core Functionality

### Market State Management

#### getMarketStateInfo()
```java
private String getMarketStateInfo()
```
Determines and formats the current market state:
- Checks if market is open/closed
- Handles weekend special cases
- Calculates time until next market opening
- Returns colored status string

### Display Management

#### Terminal Width Detection
```java
private int terminalWidth = 80; // Default terminal width
```
Automatically detects and adapts to terminal width:
- Uses `stty` command for detection
- Falls back to 80 columns if detection fails
- Adjusts display components accordingly

#### printHeader()
```java
private void printHeader(StringBuilder buffer)
```
Prints the application header with:
- Full-width separator lines
- Centered stock symbol
- Current timestamp
- Market state information
- Dynamic width adjustment

### Chart Generation

#### generateChart()
```java
private String generateChart()
```
Generates an ASCII chart visualization:
- Adapts to terminal width
- Uses price history data
- Creates a grid-based visualization
- Adds trend lines between points
- Includes time axis and price scale
- Returns formatted string with colors

Key features:
1. Dynamic width scaling
2. Automatic data point adjustment
3. Trend line visualization
4. Time axis with proportional markers
5. Color coding for price movements

### Data Fetching

#### fetchAndDisplayData()
```java
private void fetchAndDisplayData() throws IOException
```
Main function responsible for:
- Fetching stock data from Google Finance
- Parsing HTML response using JSoup
- Managing display state transitions
- Updating price history
- Rendering the display
- Handling errors

Display order:
1. Header with market state
2. Price and change information
3. Detailed stock information (if enabled)
4. Price chart visualization

### Color Management

#### color()
```java
private String color(String color, String text)
```
Applies ANSI color codes to text:
- Parameters:
  - `color`: ANSI color code
  - `text`: Text to colorize
- Returns colorized text string
- Respects `noColor` setting

## Data Management

### Price History
```java
private List<Double> priceHistory = new ArrayList<>();
private List<LocalDateTime> timeHistory = new ArrayList<>();
```
Maintains historical data for:
- Price values
- Corresponding timestamps
- Limited to last 30 updates (MAX_HISTORY_SIZE)

### State Tracking
```java
private boolean hasRealData = false;
private boolean hasDetailedInfo = false;
```
Tracks display state transitions:
- First price chart data
- First detailed information
- Manages clean display updates

## UI Components

### ANSI Escape Codes
```java
private static final String RESET = "\u001B[0m";
private static final String GREEN = "\u001B[32m";
private static final String RED = "\u001B[31m";
private static final String BLUE = "\u001B[34m";
private static final String YELLOW = "\u001B[33m";
```
Used for:
- Text coloring
- Price change indication
- Chart elements
- Market state indication

### Cursor Control
```java
private static final String CURSOR_UP = "\u001B[1A";
private static final String CURSOR_DOWN = "\u001B[1B";
private static final String CLEAR_LINE = "\u001B[2K";
private static final String CURSOR_HOME = "\u001B[H";
private static final String SAVE_CURSOR = "\u001B[s";
private static final String RESTORE_CURSOR = "\u001B[u";
```
Manages terminal cursor for smooth updates:
- Cursor positioning
- Line clearing
- Position saving/restoring
- Dynamic display refresh

## Contributing Guidelines

When contributing to this project:

1. **Code Style**
   - Follow existing code formatting
   - Use meaningful variable names
   - Add comments for complex logic
   - Keep methods focused and single-purpose

2. **New Features**
   - Add appropriate command options
   - Implement proper error handling
   - Update documentation
   - Add tests if applicable

3. **UI Changes**
   - Maintain consistent color scheme
   - Consider terminal compatibility
   - Test in different terminal sizes
   - Handle edge cases (no color, small terminals)
   - Respect terminal width constraints

4. **Performance**
   - Keep refresh rate reasonable
   - Optimize data fetching
   - Minimize memory usage
   - Handle network issues gracefully
   - Consider display state management

## Error Handling

The application implements robust error handling for:
- Network issues
- Invalid stock symbols
- Parse errors
- Display issues
- Terminal size detection
- Market hours calculation 