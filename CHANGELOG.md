# Changelog

## 1.1.0

- Players can now be downed by another revival mod before Death Rewind steps in. Server owners can change which gets priority.
- Dimensional rifts are safer for modpacks: they avoid blocks from other mods and restore changed blocks more reliably, even after a restart.
- Revives recover more safely when a checkpoint is unavailable or a player reconnects during the cutscene.
- Fixed cases where the cutscene could leave audio muted or the HUD hidden. Added Vietnamese translations.

## 1.0.0-release

### Added
- Released 1.20.1 Forge version of the mod
- Released 1.21.1 Neoforge version of the mod

### Changed
- Removed the enchant required message

## 0.4.0-beta.1

### Changed

- **Cutscene Parameters**: Updated Cutscene and default configurations for the cinematic death rewind cutscene (decreased cutscene ticks to 30 and increased frames to 52).

## 0.3.0-beta.1

### Fixed

- **Rift Block Double-Conversion**: Fixed a bug where blocks under the player could be converted twice on subsequent deaths at the same checkpoint, which prevented them from being reverted properly back to their original states upon a cost reset.
- **Forge Sleep Cost-Reset Alignment**: Realigned Forge's sleep reset mechanic with Fabric's behavior. Waking up from a successful sleep in a bed now properly resets the rewind cost and reverts rift blocks, whereas simply clicking/interacting with a bed to set a checkpoint (during the day/manually) will no longer reset the cost.

## 0.2.0-beta.1

### Added

- Released Forge version of the mod, matching all features of the Fabric version.
- Configurable **Player Shadow**: Spawns a Zombie at the player's death location, wearing the player's armor and weapons (safe from duplication with 0% drop rate).
- Configurable **Dimensional Rift**:
  - Generates cosmetic, low-survival-utility cracks (Cracked stone bricks, Cracked deepslate bricks/tiles, Cracked nether bricks, Cracked polished blackstone bricks, Basalt, Tuff, Coarse Dirt, Gravel) on return around the checkpoint.
  - Rift size and density expands gradually with subsequent deaths at the same checkpoint.
  - Automatically and smartly reverts rift blocks back to their original states upon checkpoint reset/update, ignoring any coordinates broken/changed by the player.

## 0.1.0-beta.1

Initial public beta for Minecraft 1.21.1 Fabric.

### Added

- Cinematic death rewind sequence with custom PNG cutscene and SFX.
- Safe checkpoint restore system.
- Bed click checkpoint setting during day or night.
- Death Rewind armor enchantment.
- Activation modes: `enchantment`, `totem`, `both`, and `always`.
- XP level cost system for enchantment and always modes.
- Totem fallback in `both` mode when enchantment activation is blocked by XP.
- Configurable cutscene timing and cutscene on/off option.
- White return overlay and white revive particles.
- Post-revive invulnerability window to reduce death loops.
- Per-player debug log command: `/revivebydeath log on/off`.
- Player status command: `/revivebydeath status`.
- OP commands for mode switching, cutscene toggle, test book, and XP cost reset.

### Notes

- Requires Fabric API.
- Required on both client and server.
- No copyrighted anime/game/movie assets are included.
