# Xaero's Optimal Beacons Add-On

Beacon planning tools for Xaero's World Map on Fabric.

`Xaero's Optimal Beacons Add-On` helps you preview beacon coverage, generate beacon placement plans for selected areas, compare planning modes, and quickly understand how many beacon pyramids you need to build.

## Version

Current release: `v1.0.0`

## Features

- Add a `Place Beacon` action directly to Xaero's World Map right-click menu.
- Preview beacon coverage instantly on the map before building.
- Change manual beacon preview tier from `1` to `4`.
- Generate placement plans from a selected map area.
- Support multiple plan modes:
  - `Full Coverage`
  - `Minimize Beacons`
  - `Hex-Optimal`
- Toggle an existing plan between supported planning modes.
- Shift plan gaps with directional controls:
  - `Move Gaps Up`
  - `Move Gaps Down`
  - `Move Gaps Left`
  - `Move Gaps Right`
- Display a live overlap heatmap on the world map.
- Use clear coverage colors for:
  - covered area
  - overlap
  - heavy overlap
  - uncovered gaps
- Render explicit beacon placement previews on the map so planned beacon locations are easy to identify.
- Show plan statistics directly on the map:
  - beacon count
  - total pyramid blocks required
  - stack breakdown
  - plan coverage percent
- Copy plan coordinates to clipboard.
- Send plan coordinates to chat.
- Create temporary Xaero Minimap waypoints for planned beacon locations when Xaero Minimap is installed.
- Automatically clear generated plan waypoints when a plan is replaced or removed.
- Persist planner defaults and overlay color settings in config.
- Configure defaults and colors through Mod Menu / Cloth Config.

## Screenshots

Replace the placeholder image paths below with your real screenshots before release.

### Beacon Preview

![Beacon preview screenshot placeholder](https://cdn.modrinth.com/data/3pNfuOS8/images/0eedce8fe164dd53707ed0d5c8595bd42bcef0e7.png)

### Placement Plan

![Placement plan screenshot placeholder](https://cdn.modrinth.com/data/3pNfuOS8/images/e93c6b2ae4e3ae95fcc426af4b3960cde4de0cba.png)

### Heatmap And Gap Controls

![Heatmap screenshot placeholder](https://cdn.modrinth.com/data/3pNfuOS8/images/aa4014b40c280611df700aecd54a9f0f15b5b9a0.png)

### Mod Menu Configuration

![Configuration screenshot placeholder](https://cdn.modrinth.com/data/3pNfuOS8/images/5f98ef51371da9a74f0c66dbffdd6a31525bb414.png)

## Requirements

- Minecraft `1.21.11`
- Fabric Loader `0.18.4+`
- Fabric API
- Xaero's World Map `1.40.11+`
- Java `21+`

Optional:

- Xaero's Minimap `25.3.10+`
- Mod Menu `15.0.0-beta.3+`

## Installation

1. Install Fabric Loader for Minecraft `1.21.11`.
2. Install Fabric API.
3. Install Xaero's World Map.
4. Drop `Xaero's Optimal Beacons Add-On` into your `mods` folder.
5. Optionally install Xaero's Minimap for temporary waypoint support.
6. Optionally install Mod Menu for in-game configuration.

## How To Use

### Manual Beacon Preview

1. Open Xaero's World Map.
2. Right-click the map.
3. Click `Place Beacon`.
4. Optional: right-click the preview beacon to change its tier.

### Create A Plan

1. Drag-select an area on Xaero's World Map.
2. Right-click the selected area.
3. Choose your planning mode and tier.
4. Click `Create Plan`.

### Move Coverage Gaps

1. Right-click an existing plan.
2. Choose one of the gap movement options.
3. Re-run the move until the uncovered area sits where you want it.

### Temporary Waypoints

1. Right-click an existing plan.
2. Click `Set Temporary Waypoints`.
3. Xaero Minimap will show temporary markers for planned beacon locations.

## Configuration

The mod stores settings in:

`config/xaeros-optimal-beacons.json`

If Mod Menu is installed, you can configure:

- default manual beacon tier
- default planning tier
- default planning mode
- default planning grid mode
- overlay colors
- heatmap visibility
- plan label visibility
- directional gap controls

## Notes

- Xaero Minimap support is optional.
- World Map is required.
- Heatmap and beacon preview rendering are client-side visual helpers only.
- This mod does not place blocks automatically.

## License

`MIT`

