# Coord Manager

A Fabric mod for saving, sharing and teleporting to named locations. Saved locations
show up on the vanilla **locator bar**.

It runs on the server only. On a dedicated server, players don't need to install
anything; in singleplayer it works just by being in your mods folder.

## Commands

| Command | What it does | Who can use it |
| --- | --- | --- |
| `/loc save <name>` | Saves where you're standing. Saving an existing name updates it. | Everyone |
| `/loc list [page]` | Lists every saved location, with clickable names, coordinates and teleport buttons. | Everyone |
| `/loc find <name>` | Shows one location's details, its distance from you and who saved it. | Everyone |
| `/loc tp <name>` | Teleports you to a location, including in another dimension. | Operators (configurable) |
| `/loc delete <name>` | Deletes a location. | Whoever saved it, or an operator |
| `/loc waypoints on\|off` | Shows or hides saved locations on your own locator bar. | Everyone |

Names can contain spaces and are case-insensitive (`Base` and `base` are the same location).
The old 1.x commands (`/locationsave`, `/locationlist`, `/locationfind`, `/locationtp`,
`/locationdelete`) still work.

In the list and the find output you can click:
- a **name** to show its details
- **coordinates** to copy them as `x y z`
- **[TP]** to teleport (only shown if you're allowed to)
- **[Delete]** to put the delete command in chat for you to confirm

## Locations are shared

Everyone on a server sees the same list. Each location records who saved it. With
`protectOthersLocations` on (the default), players can only update or delete their own
locations, and operators can change any of them.

## Config

`config/coordmanager.json` is created on first start:

```json
{
  "teleportRequiresOp": true,
  "protectOthersLocations": true,
  "waypoints": true,
  "listPageSize": 10
}
```

- `teleportRequiresOp`: only operators (permission level 2, the same level as vanilla `/tp`) can use `/loc tp`. In singleplayer, that means cheats must be on.
- `protectOthersLocations`: players can only change locations they saved.
- `waypoints`: show saved locations on the locator bar.
- `listPageSize`: how many locations `/loc list` shows per page.

## Where data is stored

Each world keeps its own list in `<world>/coordmanager/locations.json`, on dedicated servers
as well as in singleplayer. The file is plain JSON, so you can back it up or edit it by hand
while the server is stopped.

### Upgrading from 1.x

On first start, 2.0 imports the old `locations.txt` from the world folder (singleplayer) or
from `config/coordmanager/` (dedicated server), then renames it to `locations.txt.migrated`.
It also removes the invisible armor stands that 1.2 used as waypoint markers, one chunk at a
time as chunks are loaded.

## Building

Requires JDK 25.

```
./gradlew build
```

The jar ends up in `build/libs/`. GitHub Actions builds every push and uploads the jar as an artifact.

### Updating to a new Minecraft version

1. Get the new values from <https://fabricmc.net/develop> and update `minecraft_version`,
   `loader_version`, `loom_version` and `fabric_api_version` in `gradle.properties`. Change
   the `+26.3` suffix in `mod_version` too.
2. Run `./gradlew build`.
3. If something no longer compiles, it will be in one of the Minecraft-facing classes in
   `src/main/java/com/panosnikolakakis/coordmanager/`. The `core` package has no Minecraft
   imports and shouldn't need changes. `Waypoints.java` is the only file that touches the
   locator-bar network API.

## Code layout

```
core/                     Pure Java with unit tests: Location, the JSON file, the 1.x importer, config
CoordManager.java         Entry point: events and lifecycle
LocationService.java      Connects the core to a running server
LocationCommands.java     All commands
Chat.java                 Chat formatting and click/hover buttons
Waypoints.java            Locator-bar waypoint packets
LegacyWaypointCleanup.java  Removes 1.2's armor stand markers
```
