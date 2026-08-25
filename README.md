# Village Evolution

A Minecraft Forge mod for **1.21.1**. Villages are tracked as persistent
civilizations: they hold a development stage, population, housing, and
resource stockpiles, queue construction projects, and grow over time.
Villagers do the work — gathering materials, delivering them to build
sites, constructing and upgrading buildings, repairing damaged iron
golems, and healing wounded villagers.

## Building

```
./gradlew build
```

The mod jar is written to `build/libs/`.

Requires JDK 21. The first build downloads and decompiles Minecraft,
which takes several minutes.

## Development

```
./gradlew runClient
./gradlew runServer
```

## Project layout

- `src/main/java/com/villageevolution/mod/ai/` — villager goals
- `src/main/java/com/villageevolution/mod/village/` — village state, blueprints, construction
- `src/main/java/com/villageevolution/mod/event/` — Forge event handlers
- `src/main/java/com/villageevolution/mod/registry/` — item and creative-tab registration
- `src/main/java/com/villageevolution/mod/item/` — the blueprint item
- `src/main/resources/` — mods.toml, models, lang, textures

## License

MIT
