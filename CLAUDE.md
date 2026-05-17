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
