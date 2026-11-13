# CrateForge

Weighted reward crates for Paper servers: physical keys, an animated spin menu, a preview menu showing the exact odds, and holograms with no extra plugin. It's configured in YAML and has no dependencies.

<img width="540" height="226" alt="image" src="https://github.com/user-attachments/assets/645dccfb-3785-4816-86e4-7bc7e9ed9bfd" />


## Features

- **Weighted rewards.** Any number of crates, any number of rewards, and decimal weights are allowed. The preview shows each reward's real chance to two decimal places.
- **Spin animation.** A CS:GO-style reel that slows down and lands on the result. Filler items are rolled with the real weights, so the reel doesn't tease jackpots more often than they happen.
- **Rolled before it spins.** The reward is decided on the server before the animation starts. Closing the menu, logging out or a `/crate reload` mid-spin skips to the same result, so players can't escape the animation to re-roll.
- **Tamper-proof keys.** Keys are recognised by a hidden data tag, not by name or lore, so an anvil-renamed tripwire hook can't open anything.
- **Dupe-safe menus.** Clicks, shift-clicks, number-key swaps and drags are blocked whenever a crate menu is the top inventory.
- **Protected crate blocks.** They can't be broken, blown up or moved by pistons.
- **Holograms.** These use vanilla text displays and are never saved to disk, so a crash can't leave orphaned holograms behind.
- **Item and command rewards.** Rewards can give items (with custom names, lore, enchants above vanilla limits, glow, unbreakable), run console commands, or both. They can also announce big wins server-wide.
- **Fully configurable.** Every message uses MiniMessage (gradients, hex colours), and a typo in `crates.yml` logs the exact path that's wrong instead of a stack trace.

## Commands

| Command | Description |
|---|---|
| `/crate set <crate>` | Turn the block you're looking at into a crate |
| `/crate remove` | Turn it back into a normal block |
| `/crate givekey <player\|all> <crate> [amount]` | Give physical keys (`all` = everyone online) |
| `/crate list` | List crates and how many are placed |
| `/crate reload` | Reload `config.yml` and `crates.yml` |

Aliases: `/crates`, `/cf`. Tab completion for everything.

## Permissions

| Permission | Default | |
|---|---|---|
| `crateforge.admin` | op | All `/crate` commands |
| `crateforge.use` | everyone | Open and preview crates |

## Using it

1. Place any block (a chest, an ender chest, a beacon...), look at it, and run `/crate set vote`.
2. `/crate givekey Steve vote 5`
3. Right-click the crate with a key to open it. Left-click to preview the odds.

## Configuring a crate

```yaml
crates:
  vote:
    display-name: "<green><bold>Vote Crate"
    key:
      material: TRIPWIRE_HOOK
      name: "<green>Vote Key"
    hologram:
      - "<green><bold>Vote Crate"
      - "<gray>Right-click to open"
    rewards:
      diamonds:
        weight: 10            # chance = weight / total weight
        item:
          material: DIAMOND
          amount: 3
      money:
        weight: 25
        give-item: false      # display only - the command is the reward
        item:
          material: GOLD_NUGGET
          name: "<yellow>$5,000"
        commands:
          - "eco give %player% 5000"
```

The full example with every option is in [`crates.yml`](src/main/resources/crates.yml).

## Compatibility

Built against Paper API 1.21.4, compiled to Java 21 bytecode. It runs on Paper 1.21.4 and newer, including the 26.x releases, and on Paper forks such as Purpur.

## Building

```
gradlew build
```

The jar is written to `build/libs/`. You need JDK 21 or newer.

## Structure

```
crate/     Crate + Reward models, crates.yml loader, key tagging, placed-crate storage, holograms
gui/       Preview menu, spin session (animation + payout), session tracking
listener/  Crate interaction + block protection, menu locking
command/   /crate and tab completion
```
