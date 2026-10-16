# Blind Deaf Mute (bmdroles)

Minecraft **1.20.1**, Forge 47.x and NeoForge 1.20.1 (one jar for both), optional **Simple Voice Chat** support.

## Commands (operators by default)
| Command | What it does |
|---|---|
| `/bmd <players> <blind\|deaf\|mute\|none>` | give a role to any number of players (`@a`, `@a[team=x]`, names, ...) |
| `/bmd rotate` | Blind -> Deaf -> Mute -> Blind for everyone who has a role (no randomness) |
| `/bmd distribute` | spread the 3 roles evenly over all online players |
| `/bmd easymode [on\|off]` | toggle easy mode (no argument = toggle). Default is OFF = original behaviour |
| `/bmd clear` | remove all roles |
| `/bmd list` | show who has which role |

## Roles
- **Blind**: grayscale, low-res grid look, can only see ~14 blocks (client needs the mod).
- **Deaf**: receives no Simple Voice Chat audio (server side) and hears no Minecraft sounds (client needs the mod).
- **Mute**: their microphone audio is dropped (server side).

## Easy mode (v1.1)
Off by default (everything works exactly like 1.0). With `/bmd easymode`:
- **Blind**: blurred vision instead of the grayscale grid + 14 block view distance.
- **Deaf**: voices come through muffled and blurry; Minecraft sounds are quieter and lower pitched (music/records stay silent).
- **Mute**: their voice is still heard, but muffled and blurry.

v1.1 changes the network protocol: update the server **and** the clients.

## Client debug commands
If the Blind view looks wrong: `/bmdclient status`, `/bmdclient fog off`, `/bmdclient shader off` (and `on` again) to find out which part is responsible. `/bmdclient vanilla desaturate` loads one of Minecraft's own post effects (`/bmdclient vanilla off` removes it): if that is black too, post-processing itself is broken on that setup (shader mods, drivers), not this mod.

## Build
GitHub Actions builds on every push; the jar is in the run's *Artifacts*.
Locally: `gradle build` (Gradle 8.8+, JDK 17) -> `build/libs/`.
