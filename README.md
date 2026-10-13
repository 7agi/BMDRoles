# Blind Deaf Mute (bmdroles)

Minecraft **1.20.1**, Forge 47.x and NeoForge 1.20.1 (one jar for both), optional **Simple Voice Chat** support.

## Commands (operators by default)
| Command | What it does |
|---|---|
| `/bmd <players> <blind\|deaf\|mute\|none>` | give a role to any number of players (`@a`, `@a[team=x]`, names, ...) |
| `/bmd rotate` | Blind -> Deaf -> Mute -> Blind for everyone who has a role (no randomness) |
| `/bmd distribute` | spread the 3 roles evenly over all online players |
| `/bmd clear` | remove all roles |
| `/bmd list` | show who has which role |

## Roles
- **Blind**: grayscale, low-res grid look, can only see ~14 blocks (client needs the mod).
- **Deaf**: receives no Simple Voice Chat audio (server side) and hears no Minecraft sounds (client needs the mod).
- **Mute**: their microphone audio is dropped (server side).

## Build
GitHub Actions builds on every push; the jar is in the run's *Artifacts*.
Locally: `gradle build` (Gradle 8.8+, JDK 17) -> `build/libs/`.
