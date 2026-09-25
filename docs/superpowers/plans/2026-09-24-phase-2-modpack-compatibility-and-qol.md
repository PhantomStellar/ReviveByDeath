# Phase 2: Modpack Compatibility, Performance & QoL Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Optimize modpack performance, prevent entity lag from Player Shadows, fix sleep-reset exploit, ensure highest event priority over grave mods, and add localization support.

**Architecture:**
- Set `EventPriority.HIGHEST` on `onLivingDeath` so `ReviveByDeath` cancels death before grave/tombstone mods drop items or spawn gravestones.
- Prevent Player Shadows from accumulating and lagging servers: disable loot pickup (`shadow.setCanPickUpLoot(false)`), prevent persistence accumulation, and add a configurable lifetime with dissolve particle effect.
- Fix sleep cost reset: only reset cost when player actually sleeps through the night (`player.isSleepingLongEnough()`), preventing instant-click bed resets.
- Add i18n support in `lang/en_us.json` and `lang/vi_vn.json` for all chat and status messages.
- Add administrative command `/revivebydeath revert_all_rifts` for server admins.

**Tech Stack:** NeoForge 1.21.1, Java 21, Minecraft 1.21.1 Event & Entity API.

---

### Task 1: Intercept Death Before Grave Mods (EventPriority.HIGHEST)

**Files:**
- Modify: `neoforge/src/main/java/com/khanghy/revivebydeath/ReviveByDeath.java:64-75`

**Interfaces:**
- Consumes: `net.neoforged.bus.api.EventPriority`
- Produces: Priority-ordered death cancellation

- [x] **Step 1: Update onLivingDeath annotation**
Set `@SubscribeEvent(priority = EventPriority.HIGHEST)` on `onLivingDeath`.
This ensures `event.setCanceled(true)` executes before other mods (Corpse, Gravestone, Tombstone, KeepInventory) process the player death.

- [x] **Step 2: Verify and compile**
Run `./gradlew compileJava` in `neoforge/`.

---

### Task 2: Player Shadow Entity Lag & Item Duplication Prevention

**Files:**
- Modify: `neoforge/src/main/java/com/khanghy/revivebydeath/server/CheckpointManager.java` (spawnPastShadow)

**Interfaces:**
- Consumes: `Zombie`, `MobEffectInstance`
- Produces: Safe temporary shadow entity

- [x] **Step 1: Prevent loot pickup and entity accumulation**
In `spawnPastShadow`:
1. `shadow.setCanPickUpLoot(false)`: Prevents shadow from picking up items on the ground and voiding/duplicating them.
2. Give shadow a finite lifetime tag or apply a wither/decay effect or track shadow so it dissolves after 60 seconds (1200 ticks) instead of lingering forever in loaded chunks.
3. Drop chance for all slots already set to `0.0F`.

- [x] **Step 2: Verify and compile**
Run `./gradlew compileJava` in `neoforge/`.

---

### Task 3: Sleep Reset Exploit Fix (Only Reset on Full Night Sleep)

**Files:**
- Modify: `neoforge/src/main/java/com/khanghy/revivebydeath/server/RewindCostManager.java`

**Interfaces:**
- Consumes: `PlayerWakeUpEvent`, `ServerPlayer`
- Produces: Accurate night-sleep verification

- [x] **Step 1: Check sleeping state before reset**
In `RewindCostManager.onPlayerWakeUp`:
Only reset cost if the player has slept long enough to skip the night or `!event.wakeImmediately()`:
```java
if (event.getEntity() instanceof ServerPlayer player && ReviveByDeathConfig.get().resetCostOnSleep) {
    if (player.isSleepingLongEnough()) {
        reset(player);
    }
}
```
This ensures merely right-clicking a bed or instantly exiting bed doesn't wipe the rewind cost.

- [x] **Step 2: Verify and compile**
Run `./gradlew compileJava` in `neoforge/`.

---

### Task 4: I18n Localization & Admin Command

**Files:**
- Modify: `neoforge/src/main/resources/assets/revivebydeath/lang/en_us.json`
- Create: `neoforge/src/main/resources/assets/revivebydeath/lang/vi_vn.json`
- Modify: `neoforge/src/main/java/com/khanghy/revivebydeath/command/ModCommands.java`
- Modify: `neoforge/src/main/java/com/khanghy/revivebydeath/server/CheckpointManager.java`
- Modify: `neoforge/src/main/java/com/khanghy/revivebydeath/server/BedCheckpointEvents.java`

**Interfaces:**
- Consumes: `Component.translatable`
- Produces: Multilingual messages and `/revivebydeath revert_all_rifts`

- [x] **Step 1: Add translation keys in en_us.json and vi_vn.json**
Add keys for checkpoint set, checkpoint unsafe, fallback notifications, and command feedback.

- [x] **Step 2: Update message calls to use Component.translatable with fallback text**
Replace hardcoded Component.literal in player-facing messages with Component.translatable.

- [x] **Step 3: Add revert_all_rifts command in ModCommands.java**
Allows server operators to clear all active rift block conversions in emergency maintenance.

- [x] **Step 4: Compile and test jar build**
Run `./gradlew jar` in `neoforge/`.
