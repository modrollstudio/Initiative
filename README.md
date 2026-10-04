# Critfall: Initiative

Turn-based, initiative-driven combat for Minecraft, in the style of Baldur's Gate 3: the world
runs in real time until combat starts, then participants drop into an initiative order and take
structured turns while the rest of the world carries on.

Built on [Critfall](https://github.com/modrollstudio/Critfall)'s d20 engine — every attack roll,
save, crit, and fumble is resolved by Critfall through its public API; Initiative orchestrates
*when* and *in what order* those rolls happen, and presents them dramatically.

**Requires Critfall.** Fully optional the other way around: Critfall works standalone.

**Works with Critfall: Checks** (optional). With it installed, Shove, Grapple, Escape and Hide roll
the participants' skills instead of flat bonuses; see [actions](docs/actions.md#with-checks-installed).

## Install

- Minecraft 1.21.1 with NeoForge 21.1+, or Fabric Loader 0.16.9+ with Fabric API
- [Critfall](https://github.com/modrollstudio/Critfall) 0.2.6 or newer
- The Initiative jar for your loader (`+neoforge` or `+fabric`), on the server and every client

## Scope

Initiative is a pure engine: it provides the turn and action mechanics — encounters, initiative
order, the action economy, reactions, roll presentation and the turn UI — plus the standard actions
built on them. Content such as creatures, classes and new actions comes from datapacks and other
mods, which register their actions through the [action API](docs/api.md).

## Config

Settings live in `config/initiative.json`, created with the defaults on first run and read at
startup. Every mechanic can be tuned or switched off there; [docs/config.md](docs/config.md)
covers each key, and the other pages in [docs](docs) cover each system.

## Platform

- Java 21, Minecraft 1.21.1
- NeoForge (primary) and Fabric, from a shared loader-agnostic `common` module

## Building

Critfall is consumed from the [Modrinth maven](https://api.modrinth.com/maven) under the
`maven.modrinth:critfall` coordinates pinned in `gradle.properties`. Checks is not on Modrinth yet,
so its jars (`studio.modroll.checks:checks-*`, `checks_version` in `gradle.properties`) come from
your local Maven repo: publish them from the Checks repo before building.

```
./gradlew check    # unit tests + spotless
./gradlew build    # initiative-neoforge-*.jar and initiative-fabric-*.jar
```

GameTests: `./gradlew :neoforge:runGameTestServer` and `./gradlew :fabric:runGametest`.

## License

MIT © 2026 Modroll Studio


## Public links to the mod
[Curseforge](www.curseforge.com/minecraft/mc-mods/critfall-initiative)
