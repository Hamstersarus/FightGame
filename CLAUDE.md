> **For Claude.** This file stores prompts and low-level implementation details for AI-assisted development. For a human-readable project overview, see [README.md](README.md).

# Fight — Implementation Reference

## Build & Run

```bash
javac *.java
java Fight
```

No dependencies. All classes live in the default package.

---

## Gameplay Loop

### Combat Turn Structure
Combat is player-driven, not automatic. Each turn follows this order:

```
1. Player is prompted to choose an action:
      1. Basic Attack    — always available; ranged/shoot by default
      2. Special Ability — only available when off cooldown or trigger condition is met
2. Player's chosen action executes
3. Opponent takes their turn (basic attack or special ability, chosen automatically)
4. Repeat until one character reaches 0 HP
```

The player must actively decide what to do each turn — nothing happens until a choice is made.

### Shields 🛡️
Both the player and the opponent have a shield 🛡️. Raising it is an active action — the shield stays up until the opponent breaks it by dealing enough damage to deplete its HP. Once broken, the shield goes on a cooldown before it fully regenerates and can be raised again.

**States:**
- **Up** — `shieldActive = true`; all incoming damage hits `shieldHp` first
- **Broken** — `shieldHp` reached 0; `shieldActive = false`; `shieldRegenTimer` counts down
- **Regenerated** — `shieldRegenTimer` hits 0; `shieldHp` restored to `maxShieldHp`; can be raised again

**Rules:**
- Raising the shield costs the character their action for that turn
- While the shield is up, all incoming damage depletes `shieldHp` before reaching `hp`; overflow carries through to `hp` if the shield breaks mid-hit
- The opponent must deal enough cumulative damage to drive `shieldHp` to 0 to break through
- Once broken, the shield cannot be raised again until `shieldRegenTimer` reaches 0
- Regen timer length is a per-character value (e.g. 3–5 turns)

**Shield placement on the board:**
When a character raises their shield, the 🛡️ emoji is placed as a physical object on the board in the cell directly in front of them:
- **Player** — shield goes in the cell to the right (col + 1)
- **Opponent** — shield goes in the cell to the left (col - 1)

Before placing the shield, check whether that cell is free:
- **Cell is free** — place the shield on the board and set `shieldActive = true` as normal
- **Cell is occupied** — print a message like `"Something is in the way — can't raise your shield here."` and do NOT end the turn; the player returns to the action menu and can choose a different action or move away to free up the space

When the shield breaks or its timer expires, remove the 🛡️ emoji from the board (`board.removeCharacter` on the shield's cell). The shield cell must be tracked separately from the character's own row/col — add `int shieldRow, shieldCol` fields or a dedicated `boolean shieldOnBoard` flag alongside the existing shield HP fields.

The shield occupies a real cell, so neither character can move into it while it is up.

### Outside of Combat — Free Roam
When the player has not yet chosen an action (i.e. between turns or before combat begins), they can move their character around the board and interact with the environment.

- **Movement**: Player navigates the 10×10 grid freely
- **Interaction**: Environmental objects on the map can be interacted with (e.g. items, triggers, events) — specific interactables to be designed and added later
- **Transition**: Once the player chooses a combat action, free roam pauses and the turn resolves

### Implementation Notes
- The game loop must branch: if the player is in combat, prompt for a combat action; otherwise, accept movement/interaction input
- Player input during free roam: likely WASD or arrow keys for movement, a separate key to interact
- Combat action menu should list available options each turn (e.g. `1. Attack  2. Use Ability`)
- Opponent turn logic is automatic — no player input required for the opponent

---

## Special Ability System (Redesign)

### Design Rules
- Every character has exactly one player-triggered special ability shown as a numbered option in the action menu — same pattern as Dragon's Fire Shot.
- No ability triggers automatically. All previously passive/auto abilities (Frenzy, Hex, Bat Form, First Strike, Parry, Inferno, Undying, Cattle Drop, Regeneration, Corrupt) become deliberate player choices.
- Each ability has a turn-based cooldown. Once used, the menu option disappears until the cooldown expires.
- The opponent AI may also choose to use its ability. It follows the same cooldown rules.
- A single `int abilityCooldown` field on `Opponent` tracks the cooldown for every character. Decrement by 1 at the end of each turn (same pattern as `fireShotCooldown`). Dragon reuses its existing `fireShotCooldown` as its `abilityCooldown`.
- `canUseAbility()` returns `true` when `abilityCooldown == 0`.
- `useAbility(Opponent target)` executes the ability and sets `abilityCooldown` to the character's cooldown value.

### Per-Character Ability Specs

| Character | Ability Name | Effect | Cooldown | Ability Emoji |
|---|---|---|---|---|
| Goblin 👹 | Frenzy | This attack deals 2× attack power (24 damage) | 3 turns | *(none — rage effect only, no projectile)* |
| Witch 🧙 | Hex | Curses the opponent — 5 HP/turn for 3 turns | 4 turns | 🌀 |
| Vampire 🧛 | Bat Form | Become fully immune to damage for 1 turn | 5 turns | 🩸 |
| Ninja 🥷 | First Strike | Attack the opponent before they can act this turn | 3 turns | 🥷 |
| Knight 💂 | Parry | Block the next incoming attack completely (sets a flag) | 4 turns | ⚔️ |
| Dragon 🐉 | Fire Shot 🔥 | Deal 30 damage from anywhere (already implemented) | 3 turns | 🔥 |
| Zombie 🧟 | Undying | Re-arm: the next hit that would kill you leaves you at 1 HP instead | 5 turns | 🧠 |
| Alien 👽 | Cattle Drop | Summon UFO — 3 cows deal 45 total damage 🛸🐄🐄🐄 | 4 turns | 🛸 |
| Troll 🧌 | Regenerate | Instantly heal 24 HP (capped at maxHp) | 5 turns | 🍃 |
| Computer Virus 👾 | Corrupt | Deal attack power + 20 bonus corrupted damage | 4 turns | 💾 |

### Attack Animations
- **Basic attack** — all characters use the same generic `•` bullet projectile
- **Special ability** — each character has a unique emoji that animates across the screen before the ability resolves; Goblin is the only exception (Frenzy has no projectile, just the printed rage message)
- Player ability animations run at 80 ms/frame; opponent animations run at 250 ms/frame
- `isOffensiveAbility()` controls whether the animation plays at all — returns `true` for all characters except Goblin
- `abilityProjectile()` returns the emoji string for the animation — sourced from `abilityProjectile()` in `Opponent.java`

### Implementation Plan

**`Opponent.java` changes:**
- Add `int abilityCooldown` field (Dragon already has `fireShotCooldown` — keep it, Dragon's `canUseAbility()` delegates to `canUseFireShot()`).
- Add `boolean parryReady` field for Knight — set to `true` when Parry is activated; checked and cleared in `applyDamage()`.
- Add `boolean batFormReady` field for Vampire — set to `true` when activated; checked in `applyDamage()` and cleared at end of turn (replaces the old auto `inBatForm` every-5th-turn logic).
- Remove all auto-trigger logic from `tickStartOfTurn()` and `attack()`: Goblin `turnsAlive`, Vampire `turnCount`/`inBatForm`, Witch `attackCount % 3`, Dragon `attackCount % 4` (Inferno), Alien `attackCount % 4`, Computer Virus `corruptStacks` auto-increment.
- Add `boolean canUseAbility()` — returns `abilityCooldown == 0` (Dragon returns `fireShotCooldown == 0`).
- Add `void useAbility(Opponent target)` — branches on `name`, executes the ability, sets cooldown.
- Ninja's First Strike is handled in `Fight.java`'s game loop (not inside `useAbility`); `canUseAbility()` still controls when it's available.

**`Fight.java` changes (`doPlayerTurn`):**
- Add ability option to menu: shown when `player.canUseAbility()`.
- On selection: call `player.useAbility(opponent)` (or handle Ninja/Vampire/Knight which affect self or turn flow).
- Opponent AI in `opponentAI()`: use ability when `canUseAbility()` and a condition is met (e.g. low HP for defensive abilities, otherwise random chance).

**`tickEndOfTurn()` change:**
- Add `if (abilityCooldown > 0) abilityCooldown--;` alongside the existing `fireShotCooldown` decrement.

---

## Matchmaking System

### Goal
Instead of picking a random opponent, pair the player with a character whose overall power is close to theirs so every matchup feels competitive. A Goblin (fragile, low damage) should not be matched against a Dragon (highest HP and attack in the game) by default.

### Power Score
Each character gets a single integer `POWER` score. Add a new parallel static array to `Fight.java`:

```java
static final int[] POWER = { 101, 175, 174, 195, 195, 295, 150, 170, 201, 179 };
//                          Gob  Wit  Vam  Nin  Kni  Dra  Zom  Ali  Tro  CV
```

**Formula used to derive each score:**
`POWER = HP + (ATTACK × 3) + ABILITY_BONUS`

| Character | HP | ATK×3 | Ability Bonus | Total |
|---|---|---|---|---|
| Goblin | 50 | 36 | 15 | **101** |
| Zombie | 100 | 30 | 20 | **150** |
| Alien | 70 | 60 | 40 | **170** |
| Vampire | 90 | 54 | 30 | **174** |
| Witch | 65 | 75 | 35 | **175** |
| Computer Virus | 60 | 84 | 35 | **179** |
| Ninja | 55 | 90 | 50 | **195** |
| Knight | 120 | 45 | 30 | **195** |
| Troll | 130 | 36 | 35 | **201** |
| Dragon | 150 | 105 | 40 | **295** |

Ability bonuses are fixed subjective values reflecting utility, not raw damage.

### Matchmaking Algorithm
Implemented inside `selectCharacters`, replacing the current `do { random } while (same index)` loop:

```
playerScore = POWER[playerIdx]
tolerance   = playerScore * 0.35   // ±35% window

candidates  = all indices i where:
    i != playerIdx
    AND |POWER[i] - playerScore| <= tolerance

if candidates is empty:
    fallback — pick the index with the smallest |POWER[i] - playerScore|
               (excluding playerIdx)
else:
    pick a random index from candidates
```

A 35% tolerance means a player with score 175 accepts opponents scoring roughly 114–236, which gives most characters at least two or three valid opponents.

### Display Change
Show each character's power score on the selection screen so the player can see relative strength before choosing:

```
  1. 👹  Goblin            HP:50   ATK:12   PWR:101
```

Add `POWER[i]` to the existing `printf` line in the roster loop.

### Implementation Checklist
- [ ] Add `static final int[] POWER` array to `Fight.java` (10 values, same index order as NAMES)
- [ ] Update `printf` in `selectCharacters` roster loop to include `PWR:%d`
- [ ] Replace random opponent selection with the tolerance-window algorithm above
- [ ] Print chosen opponent's power score alongside their name after selection

---

## Standards

### Emoji sourcing
All emojis used anywhere in this project — in code print statements, documentation, or character symbols — must be sourced from [EmojiCopy](https://emojicopy.com/) (also listed in README.md Resources). Do not invent or paste emojis from other sources.

### Character abilities
[CharacterDatabase.txt](CharacterDatabase.txt) is the single source of truth for every character's special abilities. The descriptions, mechanics, damage values, cooldowns, and trigger conditions defined there govern what gets coded. If CharacterDatabase.txt and CLAUDE.md ever disagree on an ability, CharacterDatabase.txt wins.

---

## File Overview

| File | Class | Status | Role |
|------|-------|--------|------|
| [Fight.java](Fight.java) | `Fight` | Complete | Entry point — character selection, random opponent, game loop |
| [Character.java](Character.java) | `Character` | Complete | Plain data model for a combatant |
| [Opponent.java](Opponent.java) | `Opponent extends Character` | Complete | Adds combat behavior |
| [Board.java](Board.java) | `Board` | Complete | 10×10 String grid arena |

---

## Character.java

Package-private fields (no getters/setters):

```java
String name;
int hp;
int attackPower;
int shieldHp;          // current shield health; 0 = broken
int maxShieldHp;       // shield health when fully regenerated
int shieldRegenTimer;  // turns until shield regenerates; 0 = ready to raise
boolean shieldActive;  // true when the shield is currently raised
String symbol;         // emoji string displayed on the grid
String team;
int row, col;          // current position on the board
```

`symbol` is `String` (not `char`) because emojis are outside Java's Basic Multilingual Plane and cannot be stored as a single `char`. Constructor sets all fields via `this.field = param`. No validation, no default values. Fields are mutated directly by other classes (e.g., `Opponent.attack` writes to `target.hp`).

**Damage resolution with shield 🛡️:**
```
incoming = attackPower
if shieldActive:
    if shieldHp >= incoming:
        shieldHp -= incoming
        incoming = 0
    else:
        incoming -= shieldHp   // overflow carries through to hp
        shieldHp = 0
        shieldActive = false
        shieldRegenTimer = N   // N = character's regen delay (e.g. 4 turns)
hp -= incoming
```

**Shield regen (run at end of every turn):**
```
if shieldRegenTimer > 0:
    shieldRegenTimer--
    if shieldRegenTimer == 0:
        shieldHp = maxShieldHp   // fully restored, ready to raise again
```

---

## Opponent.java

Subclass of `Character` via `extends`. Constructor signature matches `Character` — takes `String symbol` and delegates entirely to `super(...)`.

**`attack(Character target)`**
- Subtracts `this.attackPower` from `target.hp` in-place.
- Prints: `"<name> attacks <target.name> for <power> damage! (<remaining> HP remaining)"`

**`isAlive()`**
- Returns `hp > 0`. HP at exactly 0 is treated as dead.

Movement logic is not yet implemented in `Opponent` — it will need to be added so the opponent can navigate the board each turn.

---

## Board.java

Internal state: `String[][] grid` of size `SIZE × SIZE` where `SIZE = 10` (static final).

**Constructor** — fills every cell with `"."` (String, not char) via nested loops.

**`placeCharacter(Character ch, int row, int col)`**
- Writes `ch.symbol` (a String) into `grid[row][col]`. No bounds checking; no collision detection.

**`removeCharacter(int row, int col)`**
- Resets `grid[row][col]` to `"."`. No bounds checking.

**`display()`**
- Prints every cell separated by a space, one row per line via `System.out.print` / `System.out.println`.

**Movement** — moving a character is done by combining the two existing methods: call `removeCharacter` on the current cell, then `placeCharacter` on the destination cell. The `Character` class will need `int row` and `int col` fields added to track each character's current position on the board so movement logic knows where they are.

---

## Fight.java

### Static Roster Arrays

All character data is stored in parallel static arrays at the top of `Fight`:

```java
static final String[] NAMES   = { "Goblin", "Witch", "Vampire", "Ninja", "Knight",
                                   "Dragon", "Zombie", "Alien", "Troll", "Computer Virus" };
static final String[] SYMBOLS = { "👹", "🧙", "🧛", "🥷", "💂", "🐉", "🧟", "👽", "🧌", "👾" };
static final int[]    HP      = { 50, 65, 90, 55, 120, 150, 100, 70, 130, 60 };
static final int[]    ATTACK  = { 12, 25, 18, 30, 15,  35,  10,  20, 12,  28 };
```

Index order is consistent across all four arrays. A character's full data is always at the same index.

### Helper: `createCharacter(int index, String team)`
Factory method — creates an `Opponent` using the parallel arrays at the given index. Keeps `main` clean and makes it easy to add characters by adding one entry to each array.

### Helper: `removeFromBoard(Board board, Opponent ch)`
Scans `board.grid` (accessible from the same default package) for cells where `grid[r][c].equals(ch.symbol)` and calls `board.removeCharacter(r, c)`. Uses `.equals()` not `==` because `symbol` is a `String`. Avoids needing to track character positions separately.

### Character Selection (in `main`)

```
1. Print numbered menu: index+1, emoji, name, HP, attack power for all 10 characters
2. Prompt player for a number 1–10
3. Validate in a loop:
   - Parse input as int; re-prompt on NumberFormatException
   - Re-prompt if value is outside [1, 10]
4. Convert to zero-based index: playerIndex = choice - 1
5. Create player Opponent via createCharacter(playerIndex, "Player")
```

### Opponent Selection

```
6. Loop: pick random index via Random.nextInt(10)
7. Repeat until index != playerIndex (guarantees a different character type)
8. Create opponent Opponent via createCharacter(opponentIndex, "Opponent")
```

### Board Setup

```
9.  Place player at row 0, col 0 (top-left)
10. Place opponent at row 9, col 9 (bottom-right)
11. Print "=== FIGHT BEGINS ===" and display board
```

### Game Loop

```
12. Loop while player.isAlive() && opponent.isAlive():
    a. Increment and print turn number
    b. Prompt player to choose an action:
          1. Basic Attack    — always available; ranged/shoot by default
          2. Special Ability — only shown when off cooldown / condition met
          3. Raise Shield 🛡️  — only shown when shieldHp > 0 and !shieldActive
          4. Move            — move to an adjacent cell instead of acting
    c. Player's chosen action executes and the board updates
       - If player attacked and !opponent.isAlive(): removeFromBoard, break
    d. Opponent takes their turn (move or attack, determined automatically)
       - If opponent attacked and !player.isAlive(): removeFromBoard, break
    e. Display board
13. Display final board state
14. Print "=== GAME OVER ===" and win/lose message
```

**Turn order**: Player always acts first within a turn.  
**Movement**: Both player and opponent can move to any adjacent cell (up, down, left, right) on their turn instead of attacking. Neither character is fixed to a starting position.  
**Win condition**: Whoever reaches `hp <= 0` first loses.

---

### Extracted Methods

`main` delegates to three named static methods so it reads as a high-level flow. All methods live in `Fight.java` — no new files.

#### `selectCharacters(Scanner scanner, Random rng) → Opponent[]`
- Prints the title banner and full roster (name, stats, flavor, special ability)
- Prompts the player for a number 1–10 with input validation loop
- Randomly picks a different opponent index
- Creates and returns both `Opponent` objects as `new Opponent[] { player, opponent }`

#### `doPlayerTurn(Scanner scanner, Opponent player, Opponent opponent, Board board)`
- Prints stats and the action menu each iteration
- Reads and validates the player's choice (number or WASD shortcut)
- Dispatches to `attack`, `fireShot`, `raiseShield`, or `handleMove`
- Loops until a combat action is taken (`combatActionTaken = true`)

#### `handleMove(Scanner scanner, String rawChoice, Opponent player, Opponent opponent, Board board)`
- If `rawChoice` is already a direction letter, uses it directly; otherwise prompts for one
- Parses W/A/S/D with a switch, validates bounds and opponent collision
- Calls `player.move(nr, nc, board)` on a valid move, prints result
- Calls `board.display()` after every move attempt

#### `main` structure
```
Opponent[] chars = selectCharacters(scanner, rng)
— board setup —
while alive:
    tickStartOfTurn
    ninja first-strike check
    doPlayerTurn(...)
    opponentAI(...)
    tickEndOfTurn
    board.display()
— game over —
```

---

## Character Reference

Implementation summary for every character type. **[CharacterDatabase.txt](CharacterDatabase.txt) is the authoritative source for all ability mechanics, damage values, and behavior** — refer there first. The entries below are coding-focused summaries that must stay consistent with it.

### Goblin 👹
| HP | Attack Power |
|----|--------------|
| 50 | 12           |

**Special Ability — Frenzy:** Attack power increases by +2 for every turn the Goblin survives. No cap.

**Implementation:**
- Add `int turnsAlive` field, initialized to 0; increment at the start of each turn
- Effective attack = `attackPower + (turnsAlive * 2)`; apply before subtracting from target's HP

---

### Witch 🧙
| HP | Attack Power |
|----|--------------|
| 65 | 25           |

**Special Ability — Hex:** Every 3rd attack, the target loses 5 HP at the start of each of their turns for 3 turns (15 total damage over time).

**Implementation:**
- Add `int attackCount` field; increment each time `attack()` is called
- When `attackCount % 3 == 0`, apply Hex to target
- Hex requires status fields on `Character`: `int hexTurnsRemaining`, `int hexDamagePerTurn = 5`
- Tick Hex damage at the start of the hexed character's turn

---

### Vampire 🧛
| HP | Attack Power |
|----|--------------|
| 90 | 18           |

**Special Ability — Bat Form:** Every 5th turn, the Vampire transforms into a bat for 1 turn. While in bat form it takes 0 damage from the opponent's attack and skips its own attack. Reverts automatically the following turn.

**Implementation:**
- Add `int turnCount` field; increment at the start of each turn
- When `turnCount % 5 == 0`, set a `boolean inBatForm = true` flag
- During damage resolution: if `inBatForm`, set incoming damage to 0 and skip the Vampire's attack
- At the end of the bat-form turn, set `inBatForm = false`
- Print a transformation message: e.g. `"🧛 transforms into a bat! 🦇 The attack passes right through!"`

---

### Ninja 🥷
| HP | Attack Power |
|----|--------------|
| 55 | 30           |

**Special Ability — First Strike:** Always attacks before the opponent at the start of a turn. If the opponent is killed by First Strike, they do not retaliate.

**Implementation:**
- Before normal turn resolution, check if either combatant is a Ninja
- If so, Ninja's `attack()` resolves first; call `isAlive()` on the target before allowing the opponent's attack

---

### Knight 💂‍♀️
| HP | Attack Power |
|----|--------------|
| 120 | 15          |

**Special Ability — Parry:** Every 3rd incoming attack is fully blocked — the Knight takes 0 damage from that hit. Counter resets after each successful parry.

**Implementation:**
- Add `int incomingAttackCount` field; increment each time the Knight is targeted
- When `incomingAttackCount % 3 == 0`, set incoming damage to 0 before subtracting from `hp`

---

### Dragon 🐉
| HP | Attack Power |
|----|--------------|
| 150 | 35          |

**Special Ability — Inferno:** Every 4th attack deals 2× attack power (70 damage) and applies Burn: 8 HP lost per turn for 3 turns.

**Implementation:**
- Add `int attackCount` field; trigger Inferno when `attackCount % 4 == 0`
- Burn requires status fields on `Character`: `int burnTurnsRemaining`, `int burnDamagePerTurn = 8`
- Apply burn damage at the start of the burned character's turn

**Special Ability — Fire Shot 🔥:** The Dragon can choose to shoot a bolt of fire at the opponent from anywhere on the board (ranged). Deals 30 damage. Has a 3-turn cooldown after each use.

**Implementation:**
- Add `int fireShotCooldown` field, initialized to 0
- Fire Shot is a player-selected action (listed alongside Attack and Move in the action menu)
- On use: deal 30 damage to opponent, set `fireShotCooldown = 3`; only available when `fireShotCooldown == 0`
- At the end of each turn, decrement `fireShotCooldown` by 1 if greater than 0
- Fire Shot ignores distance — the Dragon can fire from any cell on the board

---

### Zombie 🧟
| HP | Attack Power |
|----|--------------|
| 100 | 10          |

**Special Ability — Undying:** The first time the Zombie would reach 0 HP or below, it survives with 1 HP instead. Triggers once per battle.

**Implementation:**
- Add `boolean hasUndying = true` field
- In damage resolution: if `hp <= 0 && hasUndying`, set `hp = 1`, set `hasUndying = false`
- Does not protect against damage-over-time ticks (Hex, Burn apply after this check)

---

### Alien 👽
| HP | Attack Power |
|----|--------------|
| 70 | 20           |

**Special Ability — Cattle Drop:** Every 4th attack, the Alien skips its normal strike and summons a UFO that drops 3 cows on the opponent. Each cow deals 15 damage (45 total).

**Implementation:**
- Add `int attackCount` field; increment each time the Alien attacks
- When `attackCount % 4 == 0`, trigger Cattle Drop instead of normal attack
- Deal `3 * 15 = 45` damage directly to `target.hp`
- Print a flavored message: e.g. `"👽 summons a UFO! 🛸 Three cows 🐄🐄🐄 rain down on <target>! (45 damage)"`

---

### Troll 🧌
| HP | Attack Power |
|----|--------------|
| 130 | 12          |

**Special Ability — Regeneration:** Heals 8 HP at the end of every turn, capped at max HP.

**Implementation:**
- Add `int maxHp` field set at construction time
- At the end of each turn (after both attacks resolve), add 8 to `hp`, cap at `maxHp`

---

### Computer Virus 👾
| HP | Attack Power |
|----|--------------|
| 60 | 28           |

**Special Ability — Corrupt:** Each consecutive attack adds +4 damage (stacks indefinitely). No reset — there is only one opponent.

**Implementation:**
- Add `int corruptStacks` field, initialized to 0; increment before each attack
- Final damage = `attackPower + (corruptStacks * 4)`

---

## Character Flavor & Behavior

Full descriptions, matchup notes, and special ability narratives sourced from [CharacterDatabase.txt](CharacterDatabase.txt). These are displayed to the player on the character selection screen and inform AI opponent behavior. If CLAUDE.md and CharacterDatabase.txt ever disagree, CharacterDatabase.txt wins.

### Goblin 👹

**Description:** Goblins are scrappy and get more dangerous as a fight drags on. What they lack in raw power they make up for with relentless aggression — the longer they survive, the harder they hit.

**Special Ability — Frenzy:** The Goblin's attack power increases by +2 for every turn it survives. Turn 1: 12 damage. Turn 2: 14. Turn 3: 16. And so on, with no cap.

**Behavior Notes:**
- Starts weak; relies on outlasting the opponent long enough for Frenzy to bring its damage up to a threatening level
- Fragile — most opponents can finish it in 3–4 hits before Frenzy pays off
- Best against slow opponents (Troll, Zombie, Knight) who can't end the fight before Frenzy scales up

---

### Witch 🧙

**Description:** The Witch deals devastating magic damage but cannot take many hits. A high-risk, high-reward character who wins fights quickly or not at all.

**Special Ability — Hex:** Every 3rd attack, the Witch casts a Hex on the opponent. A Hexed opponent loses 5 HP at the start of each of their turns for 3 turns (15 total damage over time).

**Behavior Notes:**
- Glass cannon — wins fast or loses fast
- Hex stacks are dangerous: the DoT continues chipping away even when the Witch isn't attacking
- Vulnerable to Ninja (First Strike can end her before she attacks) and Dragon (Inferno burns through her low HP)

---

### Vampire 🧛

**Description:** The Vampire is a durable fighter who heals from every attack, making them harder to kill the longer a fight goes on. Best in prolonged 1v1 battles.

**Special Ability — Bat Form:** Every 5th turn, the Vampire transforms into a bat for 1 turn. While in bat form it is completely immune to the opponent's attack (0 damage) and skips its own attack. It reverts to Vampire form automatically the following turn.

**Behavior Notes:**
- Bat Form triggers on turns 5, 10, 15, 20… — the opponent effectively loses one attack every 5 turns
- Most valuable against high-damage opponents (Dragon, Ninja, Witch) where absorbing one hit can be the difference between surviving and dying
- Less impactful against low-attack opponents (Goblin, Troll, Zombie) where the avoided damage is small
- Countered by DoT effects (Hex, Burn) — status ticks are not blocked by Bat Form immunity

---

### Ninja 🥷

**Description:** The Ninja has the highest attack power in the game but the second-lowest HP. Built to eliminate the opponent before they can fight back.

**Special Ability — First Strike:** The Ninja always attacks before the opponent when a turn begins, regardless of normal turn order. If the opponent is defeated by First Strike, they do not get to retaliate that turn.

**Behavior Notes:**
- Most effective against high-HP opponents who can be severely weakened or one-shot before they retaliate
- Extremely fragile — falls quickly to anyone who survives the opening strike
- Countered by Dragon and Troll, who have enough HP to absorb the hit and retaliate hard

---

### Knight 💂

**Description:** The Knight is a disciplined fighter who uses their shield to absorb punishment. High HP and reliable damage, with a defensive ability that makes burst damage less effective against them.

**Special Ability — Parry:** Every 3rd incoming attack is fully blocked — the Knight takes 0 damage from that hit. The counter resets after each successful parry.

**Behavior Notes:**
- Strong against opponents who rely on consistent damage (Vampire, Goblin, Troll, Zombie)
- Less effective against burst or DoT (Dragon's Inferno, Witch's Hex) since Parry only negates direct hits
- Patient and reliable — rarely dominant but rarely loses badly

---

### Dragon 🐉

**Description:** The Dragon is the most powerful character in the game. Near-impossible to take down quickly and capable of eliminating most opponents in 2–3 hits.

**Special Ability — Inferno:** Every 4th attack, the Dragon breathes fire, dealing 2× attack power (70 damage) and applying Burn to the opponent: 8 HP lost per turn for 3 turns.

**Special Ability — Fire Shot 🔥:** The Dragon can shoot a bolt of fire at the opponent from anywhere on the board. Deals 30 damage, ignores distance entirely. Has a 3-turn cooldown after each use.

**Behavior Notes:**
- Wins nearly every 1v1 matchup on stats alone
- Burn from Inferno can finish off low-HP opponents (Goblin, Witch, Ninja) without a direct killing blow
- Fire Shot + Inferno on back-to-back turns is the most devastating combo: 30 damage then 70 + Burn
- Realistic counters are Alien (Cattle Drop burst) and Zombie (Undying forces an extra round)

---

### Zombie 🧟

**Description:** The Zombie refuses to die. Its attack is the weakest in the game, but a second-chance mechanic means finishing it off requires two kill shots instead of one.

**Special Ability — Undying:** The first time the Zombie would be reduced to 0 HP or below, it instead survives with 1 HP. This can only trigger once per battle.

**Behavior Notes:**
- Forces every opponent to land one extra killing blow, buying an additional turn to deal damage
- Poor against DoT (Hex, Burn) — Undying does not protect against damage-over-time ticks; a hexed or burning Zombie can die between turns
- No realistic path to victory against Dragon or Ninja

---

### Alien 👽

**Description:** The Alien's behavior is erratic and hard to plan against. Balanced stats make it a reliable pick, but its special ability introduces a dramatic swing that can reverse a losing fight instantly.

**Special Ability — Cattle Drop:** Every 4th attack, the Alien forgoes its normal strike and summons a UFO that drops 3 cows on the opponent. Each cow deals 15 damage for a total of 45 — more than double the Alien's normal attack.

**Behavior Notes:**
- Cattle Drop hits harder than any standard attack except Dragon's Inferno
- Rhythm: 20 / 20 / 20 / 45 / 20 / 20 / 20 / 45…
- Strong against high-HP opponents (Dragon, Troll, Knight) where burst matters
- Countered by Ninja — First Strike can eliminate the Alien before the first Cattle Drop on turn 4

---

### Troll 🧌

**Description:** The Troll is the hardest character to kill through sustained damage alone. Its attack output is low, but it regenerates health every turn, making it nearly impossible to whittle down.

**Special Ability — Regeneration:** At the end of every turn, the Troll heals 8 HP (up to its max HP).

**Behavior Notes:**
- Wins any endurance fight where incoming damage per turn is close to or less than 8 — Regeneration effectively nullifies weak attackers
- Countered by high-burst opponents (Dragon, Ninja) who deal damage faster than Regeneration can offset
- DoT effects (Hex, Burn) tick after Regeneration, making them efficient tools for bypassing the healing

---

### Computer Virus 👾

**Description:** The Computer Virus corrupts the opponent with escalating damage, dealing more harm with every consecutive attack. Fragile but terrifying if the fight goes on for more than a few turns.

**Special Ability — Corrupt:** Each consecutive attack adds +4 to the damage dealt (stacks indefinitely). Attack 1: 28 dmg. Attack 2: 32. Attack 3: 36. Attack 4: 40. And so on.

**Behavior Notes:**
- Starts below average in damage but overtakes most opponents by turn 4–5
- Fragile — needs to survive long enough for Corrupt to ramp up
- Devastating against high-HP opponents (Dragon, Troll) when allowed to stack: turn 6–7 deals 52–56 damage per hit
- Countered by Ninja (First Strike ends it before stacks build) and Dragon (raw HP and Inferno outpace early stacks)

---

## Animations

### Shoot Animation

Every time any character fires a projectile — basic attack, Fire Shot, or Ninja First Strike — a short terminal animation plays before the damage is resolved.

**Mechanic:** The projectile emoji slides across a fixed-width line using `\r` (carriage return) to overwrite the same terminal line on each frame. 6 frames at 80 ms each (~0.5 s total).

**Projectiles and timing by action:**
| Who | Action | Projectile | Delay |
|---|---|---|---|
| Player | Basic attack | `•` | 80 ms/frame (~0.6 s) |
| Player | Fire Shot | `🔥` | 80 ms/frame (~0.6 s) |
| Opponent | Basic attack | `•` | 250 ms/frame (~2 s) |
| Opponent | Fire Shot | `🔥` | 250 ms/frame (~2 s) |
| Opponent (Ninja) | First Strike | `🥷` | 250 ms/frame (~2 s) |

Player animations are quick and snappy. Opponent animations are slower and more menacing.

**Implementation:**
```java
static void shootAnimation(String projectile) {
    int steps = 6;
    int width = 36;
    try {
        for (int i = 0; i <= steps; i++) {
            int pos = (i * width) / steps;
            String line = "  " + " ".repeat(pos) + projectile;
            System.out.print("\r" + line + " ".repeat(Math.max(0, width - pos)));
            System.out.flush();
            Thread.sleep(80);
        }
        System.out.print("\r" + " ".repeat(width + 4) + "\r");
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
```

Called in `Fight.java` immediately before every `attack()` and `fireShot()` invocation (both player and opponent), and before the Ninja first-strike `attack()` call. Not called for passive abilities (Inferno, Cattle Drop, Hex, Burn ticks) — those print their own dramatic messages.

---

## Key Implementation Notes

- **`symbol` is `String`, not `char`**: Emojis are outside Java's Basic Multilingual Plane and require surrogate pairs — they cannot be stored in a single `char`. All three classes (`Character`, `Opponent`, `Board`) use `String` for symbol storage and `String.equals()` for comparison.
- **Direct field mutation**: `Opponent.attack` writes directly to `target.hp`. No setter needed; `Character` fields are package-private.
- **No bounds checking**: `Board.placeCharacter` and `Board.removeCharacter` do not validate row/col — starting positions must be within `[0, 9]`.
- **Inheritance over composition**: Both player and opponent use `Opponent` (not raw `Character`) because only `Opponent` has `attack()` and `isAlive()`.
- **Symbol uniqueness**: Each character must have a unique emoji `symbol`; `removeFromBoard` uses `.equals()` on the symbol to locate the character on the grid.
- **Movement**: Both the player and opponent can move around the 10×10 board. Moving is handled by calling `removeCharacter` on the current cell then `placeCharacter` on the destination. `Character` stores `int row` and `int col` to track current position. Bounds must be checked before moving (valid range: 0–9 on both axes).
- **Shields 🛡️**: Both characters have a shield. Raising it is an action that costs a turn. Damage depletes `shieldHp` before reaching `hp`; overflow carries through if the shield breaks mid-hit. Once broken, `shieldRegenTimer` counts down each turn until the shield fully restores to `maxShieldHp` and can be raised again.
- **Basic attack is always available**: Every character can perform a basic ranged/shoot attack each turn regardless of position or cooldowns.
- **Parallel arrays over objects for roster**: Character data lives in static arrays in `Fight` (not a separate class) to keep the roster easy to extend — adding a character means one new entry in each of the four arrays.
