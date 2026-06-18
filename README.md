I've launched a website that automates code generation!
https://shb2-dev.github.io/cargo_rocket_generator/

I'm new to modding. This mod may contain many bugs. I would really appreciate it if you could submit a pull request.

This mod adds cargo rockets to *Ad Astra* and, when used in conjunction with *CC:tweaked*, allows for the automation of interplanetary item transport.

# Added Features
## [v1.2.4] Rocket inventory GUI, pipes/hoppers now sync with the grounded rocket, grounded-time wait
**This is a breaking change.** The rocket itself now has its own inventory (9 slots) and its own
fuel/cargo fluid tanks — these are the *only* place the items/fuel/cargo actually live, the
launchpad itself holds nothing. Right-click a grounded rocket to open its inventory screen, where
you can place items directly, and drop a fuel/cargo fluid bucket into the dedicated slot to fill the
tank (or an empty bucket to drain it back out).
<br>The launchpad's pipe connections and hopper-accessible slots are still there exactly as before
— but instead of holding their own contents, they now act as a direct window into whichever rocket
is currently grounded on the pad. Pipe fuel into the BOTTOM face, or pipe cargo fluid into the
TOP/SIDE faces, or push items in via a hopper, and it goes straight into that rocket's own tank or
inventory in real time; the rocket's GUI and the Lua API both see the same numbers immediately. When
no rocket is grounded on the pad (or it's mid-flight), those connections simply have nowhere to
deliver to and accept nothing, the same way an unconnected pipe would.
<br>As a result, the automatic launchpad↔rocket *transfer* APIs that previously moved items or
fluid between two separate inventories have been **removed**, since there's only one inventory now:
`moveItemsFromRocketToLaunchPad`, `moveItemsFromLaunchPadToRocket`, `listRocketInventory`,
`listLaunchPadInventory`, `listLaunchPadInputSlotIndexes`, `listLaunchPadOutputSlotIndexes`.
`loadAllItems`/`unloadAllItems` are removed too, for the same reason — see the entry further down.
`getFuel()`, `getMaxFuel()`, `getFuelType()`, `getCargoFluid()`, `getMaxCargoFluid()`, and
`getCargoFluidType()` are unchanged and still report the rocket's own tank.
<br>This also fixes a long-standing bug where cargo fluid carried by a rocket would silently
disappear: previously the fluid was transferred into the *destination launchpad's* tank on landing,
so if that tank already held something else (or had none of its own), the rocket's cargo looked
like it had vanished. It's now simply wherever the rocket left it.
<br>**Wait timing changed:** instead of a fixed timer counted from the moment of launch, scripts now
use `getSecondsSinceLanded()`, which is the time since the rocket actually came to a stop. This gives
a reliable window to load/unload it (whether by hand or by pipe/hopper) regardless of flight
duration, and avoids accidentally relaunching a rocket that's still mid-landing.
<br>A new `isRocketInventoryEmpty()` function and a "Don't launch until empty" option in the script
generator let you hold a rocket on the pad if last trip's cargo wasn't unloaded yet, instead of
relaunching it with old items still aboard.
<br>Also fixed a bug where rockets descending past another rocket parked nearby (common on the Moon
with multiple shuttles in rotation) could be destroyed by a false collision; the check now only
considers other rockets that are themselves grounded, in a tighter radius directly below.
## [v1.2.3] Rocket Scanner item + name/status API
Added a new item, the **Rocket Scanner**, that lets you view every rocket in the world (position,
dimension, flight state, inventory) from a single GUI without flying out to check on them.
<br>Rockets can now be given a custom name, either from the Scanner GUI or via the new
`setRocketName(name)` Lua function.
<br>Scripts can also report what they're currently waiting for via the new `setRocketStatus(status)`
Lua function, which is shown in the Scanner GUI in place of the mod's automatic guess.
<br>See the [Rocket Scanner](#rocket-scanner) section below for details.
## Added particle and sound, and rework launch/landing animation
Use ad astra's sound and particle to expression
<br>It can now take off and land more smoothly than before.
<br>Also, a bug where the rocket would land on blocks without collision detection has been fixed.
## New rocket type
Added three new tiers of rockets.
<br>Each tier has a different maximum number of planets it can reach.
<br>The same range limitations apply as for AdAstra's rockets
<br>With the addition of rocket tiers, fuel and energy consumption now increases depending on the destination
## Added New useful tag
This mod add itemtag `denied_in_launch_pad` to prevent to use specified item in launch pad (e.g. shulker box)
<br>By using this tag,you can prevent illegal transport technic.
## customizable the difficulty level and fuel efficiency for your destination 
Change `config/ad_astra_cargo_rockets.json` to customize target destination cost and fuel efficient.

---

# Fluid & Energy Connection

**[v1.2.4]** Fuel and cargo fluid live in tanks on the rocket itself, not the launchpad — but the
launchpad's pipe connections work exactly as they did before. While a rocket is grounded on the
pad, the launchpad's faces act as a direct window into that rocket's own tanks/inventory: anything
piped or hoppered in goes straight into the rocket, and anything the rocket carries can be piped or
hoppered back out, all in real time. There's no separate launchpad-side buffer to manage — what you
see via the Lua API or the rocket's own inventory screen (right-click it while grounded) is exactly
what's in the pipes' destination.

| Side | Function |
|------|----------|
| **Bottom** | Fuel tank (synced to the grounded rocket's fuel tank) |
| **Top / Sides** | Cargo fluid tank (synced to the grounded rocket's cargo tank) |
| **Any side** | Forge Energy (FE) input |
| **Hopper-accessible slots** | The grounded rocket's 9-slot inventory |

- Connect fuel pipes to the **bottom** face to supply rocket fuel (e.g. `ad_astra:fuel`, `ad_astra:cryo_fuel`).
- Connect fluid pipes to the **top or side** faces to transport cargo liquids.
- Hoppers (or any item-transport mod) pointed at the launchpad insert into / extract from the
  rocket's inventory directly.
- You can also right-click the launchpad faces with a bucket to fill/drain the rocket's tanks the
  same way you would a normal tank, or right-click the rocket itself to use its inventory screen
  with buckets.
- If no rocket is grounded on the pad — empty, mid-flight, or still descending — these connections
  have nowhere to deliver to and simply accept nothing, the same as an unconnected pipe.

---

## Added Useful CC's Function
**[v1.2.4]** `loadAllItems`/`unloadAllItems` have been removed. They used to move items *between*
the launchpad's own inventory and the rocket's; now that the launchpad has no inventory of its own
and is just a direct window into the grounded rocket's inventory, there's nothing to move between
— pipes and hoppers write straight into the rocket, so there's no separate "load" step to script.
Instead, use the new functions below, available from the launchpad peripheral:

- `isRocketInventoryEmpty()` — `boolean`, throws if no rocket is present. Useful for holding a
  rocket on the pad until it's been fully unloaded (whether by hand or by hopper/pipe).
- `getSecondsSinceLanded()` — `int`. Seconds since the rocket actually came to a stop (not a fixed
  timer from launch). Returns `0` while in flight.
- `getTargetPlanet()` — `string`. The destination dimension ID while the rocket is in flight, or
  `"none"` when grounded.

See the [Rocket Launchpad Lua API](#rocket-launchpad-lua-api) section below for the full list.




# Rocket Launchpad Lua API

This API allows you to control a rocket launchpad from a CC:Tweaked computer. It provides functions to launch rockets, manage inventories, and check energy levels.

---

## Connecting
You must connect the computer to the central block of the launch pad to access the below methods.
Connecting to the outer blocks will allow you to access the generic inventory methods.

---

## 📦 Inventory Slot Indexing
- All inventory slot indexes in Lua start at **1**, matching CC:Tweaked's conventions.

---

## 🧨 `launch(planet)`
Attempts to launch a rocket to the specified planet. (See `getValidDestinations`)

### Parameters
- `planet` (string): The name of the destination planet.

### Errors
- `"No rocket found"` – No rocket is on the launchpad.
- `"<planet> is not a valid planet"` – The specified planet name is invalid.
- `"Not enough energy to launch"` – The launchpad lacks sufficient energy.
- `"<planet> is too high of a tier for this rocket"` – The rocket tier is too low for the destination.


---
## 📭 `isRocketInventoryEmpty()`
**[v1.2.4]** Checks whether the rocket's own inventory (9 slots) is completely empty. Useful as a
safety check before relaunching, so a rocket isn't sent off again with last trip's cargo still
aboard if it hasn't been unloaded yet (whether by hand or by hopper).

### Returns
- `boolean`: `true` if every slot in the rocket's inventory is empty.

### Errors
- `"No rocket found"` – No rocket is on the launchpad.

---

## ⏱️ `getSecondsSinceLanded()`
**[v1.2.4]** Returns how long the rocket has actually been sitting still (grounded), measured from
the moment it physically stopped moving — not a fixed timer counted from when it launched. Returns
`0` while the rocket is still ascending, descending, or if there's no rocket present. Use this
instead of a flat `sleep()` after launching to reliably give players time to use the rocket's
inventory screen before a script launches it again, regardless of how long the actual flight took.

### Returns
- `int`: Seconds since the rocket came to a stop, or `0` if not grounded / no rocket found.

---

## 🌐 `getTargetPlanet()`
**[v1.2.4]** Returns the destination dimension ID while the rocket associated with this launchpad is
in flight (tracked even after it has physically left the pad). Returns `"none"` while grounded or if
no rocket is found.

### Returns
- `string`: Destination dimension ID (e.g. `"ad_astra:moon"`), or `"none"`.

---

## ⚡ `getEnergyRequiredForLaunch()`
Returns the amount of energy required to launch the rocket.

### Returns
- `int`: Energy required.

---

## 🔋 `getEnergy()`
Returns the current stored energy in the launchpad.

### Returns
- `long`: Current energy.

---

## 🔋 `getMaxEnergy()`
Returns the maximum energy capacity of the launchpad.

### Returns
- `long`: Maximum energy.

---

## 🌍 `getValidDestinations()`
Returns a table of valid destination planet names, with the key being the planet and the value being the required rocket tier to reach it.

### Returns
- `table<string, int>`: Table with the key being the planet and the value being the required rocket tier to reach it.

---

## 🚀 `isRocketPresent()`
Checks whether a rocket is present on the launchpad.

### Returns
- `boolean`: `true` if a rocket is present, `false` otherwise.

---

## 🛢️ `getFuel()`
**[v1.2.4]** Returns the current fuel amount in the rocket's own fuel tank (previously the
launchpad's). While a rocket is grounded, the launchpad's BOTTOM-face pipe writes directly into
this same tank, so this reflects pipe-fed fuel immediately. If no rocket is currently on the pad,
returns the last-tracked rocket's value if one is being followed mid-flight, or `0` otherwise.

### Returns
- `int`: Current fuel in mB.

---

## 🛢️ `getMaxFuel()`
Returns the maximum fuel capacity of the rocket's fuel tank.

### Returns
- `int`: Maximum fuel in mB.

---

## 🛢️ `getFuelType()`
Returns the fluid ID of the fuel currently in the rocket's fuel tank.

### Returns
- `string`: Fluid registry ID (e.g. `"ad_astra:fuel"`), or `"empty"` if the tank is empty.

---

## 🧪 `getCargoFluid()`
**[v1.2.4]** Returns the current amount of cargo fluid in the rocket's own cargo tank (previously
the launchpad's). While a rocket is grounded, the launchpad's TOP/SIDE-face pipes write directly
into this same tank. This is the same fluid the rocket carries with it in flight — landing doesn't
move it anywhere, so check it via the rocket wherever it currently is.

### Returns
- `int`: Current cargo fluid in mB.

---

## 🧪 `getMaxCargoFluid()`
Returns the maximum cargo fluid capacity.

### Returns
- `int`: Maximum cargo fluid in mB.

---

## 🧪 `getCargoFluidType()`
Returns the fluid ID of the cargo fluid currently in the rocket's cargo tank.

### Returns
- `string`: Fluid registry ID, or `"empty"` if the tank is empty.

---

## 📡 `setRocketStatus(status)`
Sets a custom status string on the rocket currently sitting on this launchpad. This string is shown
in the Rocket Scanner GUI (see below) and takes priority over the mod's automatic wait-reason guess.
Useful for telling players exactly what your script is waiting for (e.g. `"Waiting for 64 iron ingots"`).

### Parameters
- `status` (`string`): Free-form text to display. Pass an empty string to clear it and fall back to
  the mod's automatic inference.

### Errors
- `"No rocket found"`: No rocket is currently on the launchpad.

---

## 🏷️ `setRocketName(name)`
Sets a display name on the rocket currently sitting on this launchpad. The name is shown in the
Rocket Scanner GUI's rocket list, and can also be set/edited manually from the GUI itself.

### Parameters
- `name` (`string`): Name to display (max 32 characters; longer names are truncated).

### Errors
- `"No rocket found"`: No rocket is currently on the launchpad.

---

# 🔍 Rocket Scanner

The **Rocket Scanner** is a new item that lets you keep track of every cargo rocket in the world
without needing to fly out and check on them manually.

Right-click with the Rocket Scanner in hand to open the scanner GUI. The left panel lists every
rocket currently loaded in the world (across all dimensions), showing its name and position.
Selecting a rocket from the list shows:

- **Position & dimension** — where the rocket currently is.
- **Flight state** — `Grounded`, `Ascending`, or `Descending`. While the rocket is airborne, the
  Scanner reports `Currently in flight` directly instead of trying to find a nearby launchpad
  (there usually isn't one at altitude).
- **Waiting on** — why the rocket is sitting on the pad. This is either:
  - a message your Lua script explicitly set via `setRocketStatus(status)` (this also picks up
    errors from `launch()` and unload retries automatically if you use the generator's scripts), or
  - the mod's own best guess (`Not enough energy`, `Not enough fuel`, `Ready (idle)`, etc.) based on
    the nearby launchpad's current energy/fuel levels, if no script-provided status is set.
- **Fuel / Cargo fluid** — **[v1.2.4]** the current and max amount in the rocket's own tanks, plus
  the fluid type. Since these tanks now live on the rocket itself, they're shown regardless of
  whether the rocket is grounded or in flight, and regardless of whether there's a nearby launchpad.
  While grounded on a launchpad, these fill/drain via the launchpad's pipe connections in real time;
  you can also fill/drain them directly by right-clicking the rocket and using the bucket slots in
  its inventory screen.
- **Inventory** — every item currently aboard the rocket, slot by slot. **[v1.2.4]** This is the
  same inventory a launchpad's hopper-accessible slots read from and write to while the rocket is
  grounded, and the same one you can edit directly by right-clicking the rocket — the Scanner shows
  it read-only.
- **Name field** — a text box to rename the rocket directly from the GUI. Renaming here has the same
  effect as calling `setRocketName(name)` from a script.

The list refreshes automatically every couple of seconds while the GUI is open, or you can press the
**Refresh** button to update immediately.

> **Note on non-Latin names:** Minecraft's vanilla `EditBox` text field has known limitations with
> IME input (e.g. Japanese, Chinese, Korean) on some platforms — composed text can render
> incorrectly while typing. This is a Minecraft/LWJGL limitation outside the mod's control. Names
> set via the Lua `setRocketName(name)` function are not affected by this, since they bypass the
> text field entirely.

---

## Credit
Fork Source: Ad-Astra-Cargo-Rockets-Unofficial by ChiyahaRe 
URL: https://github.com/ChiyahaRe/Ad-Astra-Cargo-Rockets-Unofficial
fork of the original fork Source: Ad Astra Cargo Rockets by BillBodkin
URL: https://modrinth.com/mod/ad-astra-cargo-rockets
