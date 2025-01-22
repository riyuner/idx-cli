# IDX Stock CLI

A command-line interface (CLI) application for real-time monitoring of Indonesia Stock Exchange (IDX) stock prices with interactive charts and detailed information.

## Features

- Real-time stock price monitoring
- Live market state information (OPEN/CLOSED with countdown)
- Interactive price chart with history
- Detailed stock information
- Colorful and user-friendly CLI interface
- Configurable refresh intervals
- Support for all IDX listed stocks
- Terminal width-aware display
- Automatic market hours detection (09:00-15:30 WIB)

## Requirements

- Java 21 or higher
- Maven 3.8.1 or higher
- Internet connection to fetch stock data
- GraalVM 21 (for native image build)
- Terminal with ANSI support

## Installation

### Native Image Build

Building a native executable creates a platform-specific binary that starts up faster and uses less memory.

#### Prerequisites
- GraalVM 21 installed
- Native Image tool installed (`gu install native-image`)

1. Build the native executable:
```bash
mvn package -Pnative
```

2. Run the native executable:
```bash
./target/idx-runner -s SYMBOL
```

#### Building Native Image with Docker

If you don't want to install GraalVM locally, you can build using Docker:

```bash
mvn package -Pnative -Dquarkus.native.container-build=true
```

The native executable will be available in the `target` directory.

## Usage

Basic command structure:
```bash
./target/idx-runner -s <stock-symbol> [options]
```

### Command Options

| Option | Description | Default | Required |
|--------|-------------|---------|----------|
| `-s, --symbol` | Stock symbol (e.g., BBCA) | - | Yes |
| `-d, --detailed` | Show detailed information | false | No |
| `-i, --interval` | Refresh interval in seconds | 5 | No |
| `-n, --no-color` | Disable colored output | false | No |
| `-h, --help` | Show help message | - | No |

### Examples

1. Monitor BBCA stock with default settings:
```bash
./target/idx-runner -s BBCA
```

2. Monitor BBCA stock with detailed information and 10-second refresh interval:
```bash
./target/idx-runner -s BBCA -d -i 1
```

3. Monitor BBCA stock without colors:
```bash
./target/idx-runner -s BBCA -n
```

## Display Features

### Market State Information
- Shows if the market is currently OPEN or CLOSED
- Displays countdown to market opening when closed
- Special handling for weekends with countdown to Monday opening
- Market hours: 09:00-15:30 WIB, Monday-Friday

### Real-time Display
- Adaptive display based on terminal width
- Live price updates with color-coded changes
- Historical price chart with trend lines
- Detailed company information (when enabled)
- Time-stamped updates

## Dependencies

The application is built using the following main dependencies:

- Quarkus Framework 3.17.7
- Picocli - Command line interface framework
- Jackson - JSON processing
- JSoup 1.17.2 - HTML parsing
- Quarkus REST Client - API communication

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Setup

1. Ensure you have JDK 21 and Maven installed
2. Clone the repository
3. Import the project into your favorite IDE
4. Run `mvn quarkus:dev` for development mode

## License

This project is licensed under the [LICENSE NAME] - see the LICENSE file for details.

## Acknowledgments

- Data provided by Google Finance
- Built with Quarkus Framework
- Special thanks to all contributors
