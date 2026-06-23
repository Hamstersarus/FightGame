> **For humans.** This file describes the project for people. For AI prompts and low-level implementation details, see [CLAUDE.md](CLAUDE.md).

# Fight

A Java turn-based fighting game where monsters, creatures, and chaos agents battle on a 10×10 arena.

## ▶ Play Online

**[https://fightgame.onrender.com/](https://fightgame.onrender.com/)** — play it right in your browser, no install needed.

> Hosted on a free tier, so the first load after a period of inactivity may take ~30–60 seconds to wake up. After that it's instant.

## Goals and Purpose

The intent of this project is to demonstrate software development in Java with heavy use of a modern LLM tool. I used Anthropic's Claude Sonnet 4.6 with occasional use of Opus 4.7. The development environment I chose is VSCode along with Claude CLI. This project serves as the final project in my APCSA Java programming class. The use of these AI tools is in contrast to everything we've done in the course where prior use of LLMs was prohibited.

## How to Run

The easiest way is to [play online](https://fightgame.onrender.com/). To run it locally instead:

```bash
javac *.java
java Fight
```

---

## Gameplay

### Choosing Your Character
Pick from 10 characters on the selection screen. Each shows their HP, attack power, and power score so you can gauge relative strength. Press **I** at any time to open the how-to-play instructions screen.

The game automatically picks a fair opponent using a **matchmaking system** — it finds characters whose power score is within ±35% of yours so the fight is competitive. If no close match exists (e.g. you picked Dragon), it picks whoever is closest.

### Each Turn
You always go first. Pick one action:

- **Basic Attack** — deal your ATK damage to the opponent from anywhere on the board
- **Special Ability** — your character's unique power; only available when its cooldown is at 0
- **Raise Shield 🛡️** — place a physical shield to your right; blocks all incoming damage until it breaks
- **Move** — W↑ S↓ A← D→ — repositioning does **not** use your turn
- **Quit** — press **Q** at any turn to exit the game

After you act, the opponent takes their turn automatically.

### Shield
The shield is placed as an object on the board to your right. Once up, all damage hits the shield first. When the shield breaks it recharges over a few turns and can be raised again. The cell to your right must be empty — if something is blocking it, move first.

### Map Objects
Three items spawn randomly on the board at the start of every match. Walk onto one to use it — this counts as your turn action.

| Object | Effect |
|--------|--------|
| 🏠 House | Immune to all damage for 3 turns. The house stays on the board and can be re-entered. |
| 🏥 Hospital | Instantly heals 40 HP. One-time use — disappears after. |
| 💙 Heart | Grants one extra life. If you die while holding it, you revive at full HP instead. |

The opponent AI will also go after these items when they're hurt — heading for the hospital below 50% HP and the house below 25%.

### Status Effects
Some abilities apply a lasting effect to the target:

- 🧙 **Hex** — lose 5 HP at the start of each of your next 3 turns
- 🔥 **Burn** — lose 8 HP at the start of each of your next 3 turns

You can't move to avoid these.

### Winning
First to reach 0 HP loses. Some characters have abilities that let them survive a killing blow — watch out.

---

## Character Database

| Character | Symbol | HP | ATK | Power | Special Ability |
|---|---|---|---|---|---|
| Goblin | 👹 | 50 | 12 | 101 | **Frenzy** — deals 2× attack power; 3-turn cooldown |
| Witch | 🧙 | 65 | 25 | 175 | **Hex** — curses opponent for 5 HP/turn over 3 turns; 4-turn cooldown |
| Vampire | 🧛 | 90 | 18 | 174 | **Bat Form** — become fully immune to damage for 1 turn; 5-turn cooldown |
| Ninja | 🥷 | 55 | 30 | 195 | **First Strike** — attack before the opponent acts this turn; 3-turn cooldown |
| Knight | 💂 | 120 | 15 | 195 | **Parry** — block the next incoming attack completely; 4-turn cooldown |
| Dragon | 🐉 | 150 | 35 | 295 | **Fire Shot** — deal 30 damage from anywhere on the board; 3-turn cooldown |
| Zombie | 🧟 | 100 | 10 | 150 | **Undying** — survive the next killing blow with 1 HP instead of dying; 5-turn cooldown |
| Alien | 👽 | 70 | 20 | 170 | **Cattle Drop** — UFO drops 3 cows for 45 total damage; 4-turn cooldown |
| Troll | 🧌 | 130 | 12 | 201 | **Regenerate** — instantly heal 24 HP; 5-turn cooldown |
| Computer Virus | 👾 | 60 | 28 | 179 | **Corrupt** — deal ATK+20 corrupted damage; 4-turn cooldown |

---

## Visual Features

The game runs entirely in the terminal with ANSI color output:

- **HP bar** — 10-block bar next to each character's stats; turns yellow then red as HP drops
- **Status indicators** — active Hex/Burn/House turns shown inline in the stat bar (e.g. 🧙×2)
- **Colored messages** — damage in red, heals in green, shields in blue, DoT in magenta/red
- **Bordered board** — cyan border around the 10×10 grid

---

## Project Structure

```
Fight/
├── Fight.java        # Entry point, game loop, roster, character selection, player input, animations
├── Opponent.java     # Combat behavior, special abilities, shield logic, per-turn ticks, opponent AI
├── Character.java    # Base character data model + ANSI color constants
├── Board.java        # 10×10 arena grid, board rendering, map-object spawning and interaction
├── TestHarness.java  # Developer tool — runs every character vs every character to check for bugs
└── README.md
```

## Testing

`TestHarness.java` is a standalone developer tool (not part of the game). It pits every character against every other character — all 100 matchups, run many times each, with and without map objects — and reports any crashes, hangs, or invalid states. Both sides are driven by the opponent AI, so no input is needed.

```bash
javac *.java
java -ea TestHarness
```

It prints a win-rate matrix and flags any exceptions or never-ending battles, making it easy to catch bugs after changing combat code.

## Resources

- Emojis sourced from [EmojiCopy](https://emojicopy.com/)
