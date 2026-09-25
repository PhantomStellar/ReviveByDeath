# Phase 1: Critical Stability, Zero World Corruption & Client Protection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix critical data loss (world rift blocks not reverting after restart), client permanent mute bug, and player limbo 1 HP softlock in NeoForge 1.21.1 without breaking any existing configs, saves, or IDs.

**Architecture:** 
- Implement `CheckpointSavedData` extending `net.minecraft.world.level.saveddata.SavedData` attached to Overworld data storage to persist `CHECKPOINTS`, `CHECKPOINT_RIFTS`, and `PENDING_REVIVES_ON_DISCONNECT` to disk using NBT (`NbtUtils.writeBlockState` and `NbtUtils.readBlockState`).
- Hook `ClientPlayerNetworkEvent.LoggingOut` on client NeoForge game bus to always restore audio sliders and `hideGui` state if the player disconnects during a cutscene.
- Implement intelligent fallback teleportation in `CheckpointManager.completeRevive` (Bed respawn -> World spawn) so an unsafe checkpoint never leaves a player stuck at death location with 1 HP.
- Hook `PlayerEvent.PlayerLoggedInEvent` to resume pending revives for players who disconnected mid-cutscene.

**Tech Stack:** NeoForge 1.21.1, Java 21, Minecraft 1.21.1 SavedData & NBT API.

**Spec / Requirements:**
- Zero breaking changes to existing config (`revivebydeath.json`).
- Zero breaking changes to existing `revivebydeath_rewind_costs.dat`.
- 100% backwards compatibility for existing player worlds.
- Rift blocks (`CHECKPOINT_RIFTS`) must be saved to disk so after a server restart, reverting blocks (on sleep/death/new checkpoint) restores the original blocks cleanly.

---

### Task 1: CheckpointSavedData (SavedData Persistence for Checkpoints & Rifts)

**Files:**
- Create: `neoforge/src/main/java/com/khanghy/revivebydeath/server/CheckpointSavedData.java`
- Modify: `neoforge/src/main/java/com/khanghy/revivebydeath/server/CheckpointManager.java`

**Interfaces:**
- Consumes: `MinecraftServer`, `SavedData`, `HolderLookup.Provider`, `NbtUtils`
- Produces: `CheckpointSavedData.get(MinecraftServer server)`
  - Methods:
    - `getCheckpoint(UUID playerId)` -> `Checkpoint`
    - `setCheckpoint(UUID playerId, Checkpoint checkpoint)`
    - `removeCheckpoint(UUID playerId)`
    - `hasCheckpoint(UUID playerId)` -> `boolean`
    - `getRiftData(UUID playerId)` -> `CheckpointRiftData`
    - `getOrCreateRiftData(UUID playerId)` -> `CheckpointRiftData`
    - `removeRiftData(UUID playerId)` -> `CheckpointRiftData`
    - `addPendingReviveOnLogin(UUID playerId, Checkpoint checkpoint)`
    - `popPendingReviveOnLogin(UUID playerId)` -> `Checkpoint`

- [x] **Step 1: Create CheckpointSavedData.java**
Implement `SavedData` with NBT serialization:
- Serialize `CHECKPOINTS`: `UUID -> (dimension, x, y, z, yaw, pitch, savedAtTick)`
- Serialize `CHECKPOINT_RIFTS`: `UUID -> { reviveCount: int, blocks: [ { pos: long, original: BlockState, placed: BlockState } ] }` using `NbtUtils.writeBlockState` and `NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag)`.
- Serialize `PENDING_REVIVES_ON_LOGIN`: `UUID -> Checkpoint`

- [x] **Step 2: Update CheckpointManager.java to use CheckpointSavedData**
Refactor `CheckpointManager` so that:
- It gets data from `CheckpointSavedData.get(server)` instead of memory-only static maps for checkpoints, rift blocks, and pending revives across disconnects.
- `createDimensionalRift` marks `savedData.setDirty()`.
- `revertRiftBlocks` retrieves data from `savedData`, restores the blocks in the world, removes the record, and calls `savedData.setDirty()`.

- [x] **Step 3: Compile and verify CheckpointSavedData compiles cleanly**
Run `./gradlew compileJava` in `neoforge/`.

---

### Task 2: Safe Fallback Teleportation (Prevent Limbo / Softlock)

**Files:**
- Modify: `neoforge/src/main/java/com/khanghy/revivebydeath/server/CheckpointManager.java:250-282`

**Interfaces:**
- Consumes: `ServerPlayer`, `ServerLevel`, `Checkpoint`
- Produces: Safe teleportation target even when initial checkpoint is destroyed/flooded.

- [x] **Step 1: Implement findSafeFallbackPosition in CheckpointManager**
If `level == null || !isCheckpointStillSafe(level, checkpoint)`:
1. Try `player.getRespawnPosition()` and `player.getRespawnDimension()` via `player.findRespawnPositionAndUseSpawnBlock(false, DimensionTransition.DO_NOTHING)`.
2. If invalid or null, get `server.overworld().getSharedSpawnPos()`, finding the highest sturdy safe block.
3. Teleport player there safely.
4. Notify player via system message: `"Your saved checkpoint was no longer safe; you were returned to your spawn point."`
5. Stabilize player health to `REVIVE_HEALTH` (8.0F) and food to `REVIVE_FOOD` (8), apply protection ticks.

- [x] **Step 2: Test edge case logic in code review**
Verify that under no circumstances can `completeRevive` exit without either teleporting the player to safe ground or completing the revive sequence.

- [x] **Step 3: Compile and verify**
Run `./gradlew compileJava` in `neoforge/`.

---

### Task 3: Client Disconnect Audio & GUI Protection

**Files:**
- Modify: `neoforge/src/main/java/com/khanghy/revivebydeath/client/ReviveByDeathClient.java`
- Modify: `neoforge/src/main/java/com/khanghy/revivebydeath/ReviveByDeath.java` (ClientEvents)

**Interfaces:**
- Consumes: `ClientPlayerNetworkEvent.LoggingOut`
- Produces: `ReviveByDeathClient.onDisconnect()`

- [x] **Step 1: Add onDisconnect cleanup method in ReviveByDeathClient.java**
```java
public static void onDisconnect() {
    Minecraft client = Minecraft.getInstance();
    restoreCutsceneState(client);
    introFadeTicks = 0;
    cutsceneTicks = 0;
    returnEffectTicks = 0;
    cutsceneSoundPlayed = false;
}
```
Ensure `restoreCutsceneState(client)` unconditionally restores all muted volumes back to `previousSourceVolumes` (if recorded) and resets `client.options.hideGui = previousHideGui`.

- [x] **Step 2: Register ClientPlayerNetworkEvent.LoggingOut in ClientEvents**
In `ReviveByDeath.ClientEvents`:
```java
@SubscribeEvent
public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
    ReviveByDeathClient.onDisconnect();
}
```

- [x] **Step 3: Compile and verify**
Run `./gradlew compileJava` in `neoforge/`.

---

### Task 4: Mid-Cutscene Disconnect / Combat-Log Recovery

**Files:**
- Modify: `neoforge/src/main/java/com/khanghy/revivebydeath/server/CheckpointManager.java`
- Modify: `neoforge/src/main/java/com/khanghy/revivebydeath/ReviveByDeath.java`

**Interfaces:**
- Consumes: `PlayerEvent.PlayerLoggedInEvent`
- Produces: `CheckpointManager.onPlayerLoggedIn(ServerPlayer player)`

- [x] **Step 1: Handle player disconnect in tickPendingRevives**
When a player disconnects while `PENDING_REVIVES` contains their UUID:
- Store their `checkpoint` in `CheckpointSavedData.get(server).addPendingReviveOnLogin(uuid, pendingRevive.checkpoint())`.
- Remove from `PENDING_REVIVES`.

- [x] **Step 2: Hook PlayerLoggedInEvent in ReviveByDeath.java**
When player logs in:
- Check if `CheckpointSavedData.get(player.getServer()).popPendingReviveOnLogin(player.getUUID())` has a checkpoint.
- If yes, teleport them to the checkpoint (or fallback), restore health/food, and grant post-revive protection.
- Send notification: `"Death rewind completed upon reconnecting."`

- [x] **Step 3: Compile and verify**
Run `./gradlew compileJava` in `neoforge/`.
