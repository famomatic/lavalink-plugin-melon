# Melon Source Plugin

This project provides a [Lavalink](https://github.com/lavalink-devs/Lavalink) plugin that adds support for playing tracks from [Melon](https://www.melon.com/).

## Features

- Search Melon using the `melonsc:` prefix (e.g. `melonsc:아이유`)
- Load tracks directly from Melon song URLs

## Building

This plugin targets **Lavalink 4.x** and requires **Java 17** or newer.

```bash
./gradlew shadowJar
```
The shaded jar will be located in `build/libs`.

## Usage

Copy the generated jar to Lavalink's `plugins` directory and enable the plugin in your Lavalink configuration.

## Configuration

Enable or disable the plugin via `application.yml`:

```yaml
plugins:
  melon:
    enabled: true
```

## License

MIT