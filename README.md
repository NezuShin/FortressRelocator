# FortressRelocator

Nether fortresses are a hardcoded structure. World-generation datapack can't change their Y spawn point.

This Folia plugin adds `y-offset` to new fortresses. The biome check and the nether-brick pieces both move by that amount. Chunks that already contain a fortress stay where they are.

Target server: Folia 1.21.11. Java 21.

## Config

`plugins/FortressRelocator/config.yml`

```yaml
y-offset: -192
```

`0` leaves fortresses at the vanilla height.

## Build

```bat
gradlew.bat build
```

The plugin jar is `build/libs/FortressRelocator-1.0-SNAPSHOT.jar`. Copy that file into the server `plugins` folder.
