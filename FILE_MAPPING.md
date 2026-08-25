# File mapping: old repo name → true identity

Every file in `Village-life344` was stored under the wrong name. The
content was intact; only the filenames were scrambled. Each file was
re-identified from its own `package` + type declaration (Java) or its
JSON body, then placed at the correct path.

| Old filename | Actual content | New path |
|---|---|---|
| `BlueprintItem.java` | class `GatherMaterialsGoal` | `src/main/java/com/villageevolution/mod/ai/GatherMaterialsGoal.java` |
| `BlueprintLibrary.java` | class `VillagerEvolutionMod` | `src/main/java/com/villageevolution/mod/VillagerEvolutionMod.java` |
| `DeliverMaterialsGoal.java` | **build.gradle** | `build.gradle` |
| `GoalAccessHelper.java` | class `DeliverMaterialsGoal` | `src/main/java/com/villageevolution/mod/ai/DeliverMaterialsGoal.java` |
| `ModItems.java` | class `ConstructionWorkGoal` | `src/main/java/com/villageevolution/mod/ai/ConstructionWorkGoal.java` |
| `README.md` | item model (storage) | `src/main/resources/assets/villageevolution/models/item/blueprint_storage.json` |
| `ResourceType.java` | class `VillagerInjuryHelper` | `src/main/java/com/villageevolution/mod/util/VillagerInjuryHelper.java` |
| `VillageBuilding.java` | class `VillagerJoinHandler` | `src/main/java/com/villageevolution/mod/event/VillagerJoinHandler.java` |
| `VillageInstance.java` | class `VillageTickHandler` | `src/main/java/com/villageevolution/mod/event/VillageTickHandler.java` |
| `VillageSavedData.java` | class `VillagerTaskData` | `src/main/java/com/villageevolution/mod/util/VillagerTaskData.java` |
| `VillageStage.java` | class `BlueprintItem` | `src/main/java/com/villageevolution/mod/item/BlueprintItem.java` |
| `VillageStatistics.java` | class `GoalAccessHelper` | `src/main/java/com/villageevolution/mod/util/GoalAccessHelper.java` |
| `VillageTickHandler.java` | class `HealWoundedVillagerGoal` | `src/main/java/com/villageevolution/mod/ai/HealWoundedVillagerGoal.java` |
| `VillagerEvolutionMod.java` | class `RepairIronGolemGoal` | `src/main/java/com/villageevolution/mod/ai/RepairIronGolemGoal.java` |
| `VillagerJoinHandler.java` | class `FarmerContributeFoodGoal` | `src/main/java/com/villageevolution/mod/ai/FarmerContributeFoodGoal.java` |
| `blueprint_blacksmith.json` | class `VillageSavedData` | `src/main/java/com/villageevolution/mod/village/VillageSavedData.java` |
| `blueprint_blacksmith.png` | enum `BuildingType` | `src/main/java/com/villageevolution/mod/village/BuildingType.java` |
| `blueprint_clinic.json` | class `ModItems` | `src/main/java/com/villageevolution/mod/registry/ModItems.java` |
| `blueprint_hospital.json` | class `BlueprintLibrary` | `src/main/java/com/villageevolution/mod/village/BlueprintLibrary.java` |
| `blueprint_house.json` | class `VillageManager` | `src/main/java/com/villageevolution/mod/village/VillageManager.java` |
| `blueprint_house.png` | item model (clinic) | `.../models/item/blueprint_clinic.json` |
| `blueprint_market.json` | class `VillageInstance` | `src/main/java/com/villageevolution/mod/village/VillageInstance.java` |
| `blueprint_storage.json` | enum `ResourceType` | `src/main/java/com/villageevolution/mod/village/ResourceType.java` |
| `blueprint_town_hall.json` | record `BlockPlacement` | `src/main/java/com/villageevolution/mod/village/BlockPlacement.java` |
| `blueprint_town_hall.png` | enum `VillageStage` | `src/main/java/com/villageevolution/mod/village/VillageStage.java` |
| `blueprint_wall_gate.json` | class `VillageStatistics` | `src/main/java/com/villageevolution/mod/village/VillageStatistics.java` |
| `blueprint_watchtower.json` | class `ModCreativeTab` | `src/main/java/com/villageevolution/mod/registry/ModCreativeTab.java` |
| `blueprint_watchtower.png` | class `ConstructionProject` | `src/main/java/com/villageevolution/mod/village/ConstructionProject.java` |
| `en_us.json` | class `VillageBuilding` | `src/main/java/com/villageevolution/mod/village/VillageBuilding.java` |
| `gradle.properties` | item model (blacksmith) | `.../models/item/blueprint_blacksmith.json` |
| `mods.toml` | item model (watchtower) | `.../models/item/blueprint_watchtower.json` |
| `pack.mcmeta` | item model (farm) | `.../models/item/blueprint_farm.json` |
| `settings.gradle` | item model (wall_gate) **+** real `settings.gradle` | split into `.../models/item/blueprint_wall_gate.json` and `settings.gradle` |

## Files that did not exist and were written from scratch

| File | Why |
|---|---|
| `gradle.properties` | The name was taken by an item model. `build.gradle` references 11 properties from it; without it the build fails immediately. |
| `src/main/resources/META-INF/mods.toml` | Same — name was taken by an item model. Forge will not load the mod without it. |
| `src/main/resources/pack.mcmeta` | Same. `pack_format` 34 = MC 1.21.1. |
| `src/main/resources/assets/villageevolution/lang/en_us.json` | Same. Generated from `BuildingType` — 10 item keys plus the creative tab key. |
| `gradlew`, `gradlew.bat`, `gradle/wrapper/*` | Never in the repo. This is what caused exit code 127. Gradle 8.8. |
| `models/item/blueprint_{town_hall,house,market,hospital}.json` | `BuildingType` has 10 constants but only 6 models survived. |
| `textures/item/blueprint_*.png` (10) | **Placeholders.** No real texture ever existed in the repo — all four `.png` files held Java or JSON. Replace these. |
| `README.md` | Name was taken by an item model. |
| `.gitattributes` | Forces LF on `gradlew`; CRLF here is the other cause of exit 127. |
| `.gitignore` | Was absent; keeps `build/`, `.gradle/`, `run/` out of the repo. |

## Corrections to existing files

- **`settings.gradle`** — had a JSON object prepended before the Groovy, and
  a bogus `maven { url = "https://neoforged.net" }` entry (not a Maven repo,
  and unnecessary: the code imports `net.minecraftforge.*`).
- **`.github/workflows/build.yml`** — JDK raised from 17 to 21. `build.gradle`
  declares `JavaLanguageVersion.of(21)`, and `VillageSavedData` uses the
  1.20.5+ `SavedData.Factory` / `HolderLookup.Provider` API. Also added a jar
  upload step and pinned `actions/setup-java@v4`.

## Verification performed

- All 26 classes compile-parsed with `javac 21`. 606 errors, all of them
  `cannot find symbol` / `package does not exist` against `net.minecraft.*`
  and `net.minecraftforge.*` — expected without the Minecraft classpath.
  **Zero syntax or structural errors.**
- All internal `com.villageevolution.*` imports resolve. Nothing missing.
- Every JSON file parses.
- Every `project.*` property referenced by `build.gradle` exists in
  `gradle.properties`.
