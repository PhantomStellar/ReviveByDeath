# ReviveByDeath

ReviveByDeath is a Minecraft 1.20.1 and 1.21.1 mod for **Fabric**, **Forge**, and **NeoForge** that replaces fatal moments with a short, cinematic rewind sequence, restoring the player to their latest safe checkpoint.

All visual and audio assets in this project are original placeholders/custom assets. Do not add copyrighted anime, game, movie, logo, quote, or sound content unless you have explicit rights to use and monetize it.

---

## Requirements

- **Minecraft**: `1.20.1` or `1.21.1`; use a JAR built for your exact version and loader.
- **Fabric 1.21.1**: Requires Fabric Loader `0.19.3` or newer and Fabric API.
- **Forge 1.21.1**: Requires Forge `52.1.0` or newer.
- **NeoForge 1.21.1**: Requires NeoForge `21.1` or newer.
- **Java**: `17` for Minecraft 1.20.1, `21` for Minecraft 1.21.1.

ReviveByDeath is required on both client and server.

---

## Features

### 1. Cinematic death rewind cutscene
If the player meets the revive requirements, death is interrupted, a short cutscene plays, and the player returns to their latest safe checkpoint.

### 2. Player Shadow
When the rewind sequence starts, a custom Zombie representing the player's shadow spawns at the death location:
- Equipped with the exact armor and weapons the player was holding/wearing.
- Custom-named `<Player>'s Shadow` with permanent Speed II, Strength I, and Glowing for 10 seconds.
- Drop rate set to `0.0F` to prevent duplication of gear.
- Can be toggled on/off in the configuration (`spawnPastShadow`).

### 3. Dimensional Rift
When returning to the checkpoint, a cosmic rift/timeline fracture cracks the floor blocks around it:
- **Low Survival Utility**: Avoids high-value blocks like Crying Obsidian or Sculk. Replaces common floor blocks with low-utility equivalents:
  - `Stone Bricks` -> `Cracked Stone Bricks`
  - `Deepslate Bricks` -> `Cracked Deepslate Bricks`
  - `Deepslate Tiles` -> `Cracked Deepslate Tiles`
  - `Nether Bricks` -> `Cracked Nether Bricks`
  - `Polished Blackstone Bricks` -> `Cracked Polished Blackstone Bricks`
  - `Logs / Planks` (Wood) -> `Basalt` (charred log appearance)
  - `Stone / Cobblestone` -> `Tuff`
  - `Grass / Dirt` -> `Coarse Dirt`
  - `Sand` -> `Gravel`
- Blocks outside the supported vanilla set, including blocks from other mods and blocks with stored data, are left untouched.
- **Gradual Expansion**: The rift grows in radius and density if the player repeatedly dies and returns to the same active checkpoint.
- **Smart Reversion & No Conflict**: When the checkpoint is reset (e.g. setting a new checkpoint, sleeping in a bed, true vanilla death, or admin command), all rift blocks automatically revert back to their original states. If a player has broken or modified a block in the meantime, the mod is smart enough to leave it alone.
- Includes visual Portal particles/crumbling sound on crack placement, and Witch particles/clock chime sound right before reversion.
- Can be toggled on/off in the configuration (`createDimensionalRift`).

---

## Activation Modes

`activationMode` controls what is required to trigger a rewind.

- `enchantment`: Requires at least one equipped armor piece with Death Rewind. This is the default survival-friendly mode.
- `totem`: Requires a Totem of Undying in either hand. If `replaceVanillaTotem` is true, the mod replaces the vanilla totem behavior with the cinematic rewind.
- `both`: Allows either Death Rewind armor or a totem. Armor enchantment is checked first, so a player wearing Death Rewind will not consume a totem.
- `always`: No item requirement. Useful for testing or showcases, not recommended for balanced survival.

---

## Death Rewind Enchantment

Death Rewind is a level 1 armor enchantment available through vanilla-style progression:
- Enchanting table, because it is tagged as `minecraft:non_treasure`.
- Villager trading, because it is tagged as `minecraft:tradeable`.
- OP test command: `/revivebydeath give_book`.

Only one equipped armor piece needs the enchantment to activate rewind.

---

## Checkpoints

The server automatically tracks each player's latest safe checkpoint. A checkpoint is saved only when the player is:
- alive;
- on the ground;
- not in lava, fire, or the void;
- standing on a sturdy non-liquid block;
- in enough open space for feet and head.

Players can also right-click a bed during day or night to manually save a checkpoint.

---

## XP Cost

When `useXpCost` is true, enchantment and `always` rewinds cost XP levels. Totem rewinds consume the totem instead, so they can still work when the player does not have enough XP.

The cost resets when the player sleeps if `resetCostOnSleep` is true. It also resets after a normal death if `resetCostOnDeath` is true.

---

## Configuration

Server config path: `config/revivebydeath.json`

Recommended default:

```json
{
  "activationMode": "enchantment",
  "revivePriority": "downed_first",
  "replaceVanillaTotem": true,
  "consumeTotem": true,
  "enchantmentArmorDamage": 35,
  "useXpCost": true,
  "minimumXpLevelCost": 6,
  "xpLevelCostIncrease": 2,
  "xpLevelCostMultiplier": 1.18,
  "resetCostOnSleep": true,
  "resetCostOnDeath": true,
  "enableCutscene": true,
  "cutsceneIntroFadeTicks": 4,
  "cutsceneTicks": 30,
  "cutsceneFrames": 52,
  "returnEffectTicks": 14,
  "postReviveInvulnerabilityTicks": 60,
  "checkpointInitialCheckTicks": 10,
  "checkpointRecheckTicks": 6000,
  "spawnPastShadow": true,
  "createDimensionalRift": true
}
```

`revivePriority` is a server-wide setting on all six supported builds (Fabric, Forge and NeoForge for Minecraft 1.20.1 and 1.21.1). It controls the order in which ReviveByDeath tries to rewind relative to downed/revival mods:

- `downed_first` (default): Give a downed/revival mod the first chance to save the player. If it allows a later fatal death, ReviveByDeath tries to rewind. If neither saves the player, normal death proceeds and grave mods follow their own rules.
- `rewind_first`: ReviveByDeath tries to rewind first. If its checkpoint, activation, or cost requirements are not met, the other revival mod can still put the player in a downed state.

Edit `config/revivebydeath.json` on the server. Existing config files receive `downed_first` when upgraded. Restart the server after changing this setting.

For Revive Me, leave its `Run_Death_Event_First` option disabled when using `downed_first`. Enabling that option makes Revive Me run other mods' death listeners before it tries to down the player.

Fabric has downed/revival mods too, including Not Enough Revives (1.20.1 and 1.21.1) and PlayerReviveFabric (1.20.1). The exact order can depend on each mod's death hook, so test the full downed, bleedout, rewind, and normal-death sequence in your modpack before updating a live server.

---

## Commands

Player commands:
```mcfunction
/revivebydeath status
/revivebydeath log on
/revivebydeath log off
```

OP-only commands:
```mcfunction
/revivebydeath mode always
/revivebydeath mode totem
/revivebydeath mode enchantment
/revivebydeath mode both
/revivebydeath cutscene on
/revivebydeath cutscene off
/revivebydeath give_book
/revivebydeath reset_cost
```

---

## Build & Project Structure

The project has one module per loader and Minecraft version:
- `/fabric`, `/forge`, `/neoforge`: Minecraft 1.21.1.
- `/fabric-1.20.1`, `/forge-1.20.1`, `/neoforge-1.20.1`: Minecraft 1.20.1.

To compile each version, go into its directory and run the gradle wrapper:

For Fabric 1.21.1:
```powershell
cd fabric
.\gradlew.bat clean build
```

For Forge 1.21.1:
```powershell
cd forge
.\gradlew.bat clean build
```

The compiled jars are located under `<module>/build/libs/`.

---

## License

This project is licensed under the MIT License.
