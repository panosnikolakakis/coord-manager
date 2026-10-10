# Coord Manager

Minecraft fabric mod to manage locations (client/server)

for singleplayer use: put the mod jar in the /mods folder
for server use: put the mod jar in the server /mods folder, no client mod needed

## Commands

- `/loc save <name>` Saves where you're standing. If using a name that already exists, the previous location will be overwritten.
- `/loc list [page]` Lists every saved location, with clickable names, coordinates and teleport buttons.
- `/loc find <name>` Shows one location's details, its distance from you and who saved it.
- `/loc tp <name>` Teleports you to a location, including in another dimension. Available only to server operators (configurable).
- `/loc delete <name>` Deletes a location. Only whoever saved it, or an operator, can delete it.
- `/loc waypoints on|off` Shows or hides saved locations on your own locator bar. Locations use the "bowtie" waypoint style by default (configurable).

Names can contain spaces and are case-insensitive (`Home` and `home` are the same location).
You don't have to type the whole name: tab completion and `/loc find` and `/loc tp` match any part of it, so `/loc find th` finds `over there`. If several locations match, you get a clickable list to pick from. `/loc delete` always needs the full name.
The 1.x commands (`/locationsave` and so on) have been replaced by `/loc`.

In the list and find outputs you can click:
- a name to show its details
- coordinates to copy them as `x y z`
- [TP] to teleport (only shown if you're allowed to)
- [Delete] to put the delete command in chat for you to confirm

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
  "waypointStyle": "minecraft:bowtie",
  "waypointColor": "EFBF04",
  "listPageSize": 10
}
```

- `teleportRequiresOp`: only operators (permission level 2, the same level as vanilla `/tp`) can use `/loc tp`. In singleplayer, that means commands must be on.
- `protectOthersLocations`: players can only change locations they saved.
- `waypoints`: show saved locations on the locator bar.
- `waypointStyle`: the locator-bar icon for locations. The default, `minecraft:bowtie`, is built into vanilla, so every player sees it, and it looks different from the dots used for players. A server resource pack can add other styles.
- `waypointColor`: a hex colour (`EFBF04` or `#EFBF04`) for every location's icon, or leave it empty so each location gets its own random colour.
- `listPageSize`: how many locations `/loc list` shows per page.

## Where data is stored

Each world keeps its own list in `<world>/coordmanager/locations.json`, on dedicated servers as well as in singleplayer.

### Upgrading from 1.x

On first start, 2.0 imports the old `locations.txt` from the world folder (singleplayer) or
from `config/coordmanager/` (dedicated server), then renames it to `locations.txt.migrated`.
It also removes the invisible armor stands that 1.2 used as waypoint markers, one chunk at a
time as chunks are loaded.