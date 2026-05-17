> **For humans.** This file describes the project for people. For AI prompts and low-level implementation details, see [CLAUDE.md](CLAUDE.md).

# Fight

A Java turn-based fighting game where monsters, creatures, and chaos agents from opposing teams clash on a 10×10 arena.

## Goals and Purpose

The intent of this project is to demonstrate software development in Java with heavy use of a modern LLM tool. I used Anthropic's Claude Sonnet 4.6 with occasional use of Opus 4.7. The development enviroment I chose is vscode along with Claude CLI. This project serves as the final project in my APCSA Java programming class. The use of these AI tools is in contrast to everything we've done in the course where prior use of LLMs was prohibited.

## Characters

Each character has five attributes:

| Attribute     | Type   | Description                                        |
|---------------|--------|----------------------------------------------------|
| `name`        | String | The character's name                               |
| `hp`          | int    | Hit points (health)                                |
| `attackPower` | int    | Damage dealt per attack                            |
| `symbol`      | char   | Emoji used to represent the character on the board |
| `team`        | String | The team this character belongs to                 |

### Example

```java
Character goblin  = new Character("Goblin",  50,  12, '👹', "Shadow");
Character vampire = new Character("Vampire", 90,  18, '🧛', "Shadow");
Character knight  = new Character("Knight",  120, 15, '💂', "Iron Wall");
```

## Board

The arena is a 10×10 grid where characters are placed and battle each other.

| Method                                | Description                                    |
|---------------------------------------|------------------------------------------------|
| `placeCharacter(Character, row, col)` | Places a character's emoji symbol on the grid  |
| `removeCharacter(row, col)`           | Clears a cell back to `'.'`                    |
| `display()`                           | Prints the current grid to the console         |

Empty cells are represented by `'.'`. Each character occupies one cell, identified by its emoji `symbol`.

### Example Output

```
👹 .  .  .  .  .  .  .  .  .
.  .  .  .  .  .  .  .  .  .
.  .  .  .  .  .  .  .  .  .
.  .  .  .  .  .  .  .  .  .
.  .  .  .  .  .  .  .  .  .
.  .  .  .  .  .  .  .  .  .
.  .  .  .  .  .  .  .  .  .
.  .  .  .  .  .  .  .  .  .
.  .  .  .  .  .  .  .  .  .
.  .  .  .  .  .  .  .  .  🐉
```

## How It Works

### Combat
Combat is turn-based and player-driven. Each turn you choose one action:

- **Basic Attack** — every character can shoot or strike the opponent regardless of position
- **Special Ability** — use your character's unique power (only available when it's off cooldown or its trigger condition is met)

After you act, the opponent automatically takes their turn — attacking or using their ability.

Combat ends when one character runs out of HP. Both characters have a **shield** 🛡️. Raising it costs your action for that turn. While the shield 🛡️ is up, all incoming damage hits it instead of your HP — the opponent must break through it first. Once the shield 🛡️ is broken it goes on cooldown, then fully regenerates after a set number of turns so it can be raised again.

Nothing happens until you make a choice.

### Exploring
When you haven't taken a combat action yet, you can freely move your character around the board and interact with the environment. Interactable objects and events on the map will be added in a future update.

---

## Character Database

All available character types with their base stats and playstyle.

| Character      | Symbol | HP  | Attack Power | Playstyle |
|----------------|--------|-----|--------------|-----------|
| Goblin         | 👹     | 50  | 12           | Swarms in numbers; individually weak but cheap to field |
| Witch          | 🧙     | 65  | 25           | Glass cannon; devastating attack, low survivability |
| Vampire        | 🧛     | 90  | 18           | Every 5th turn transforms into a bat 🦇 — immune to all damage that turn |
| Ninja          | 🥷     | 55  | 30           | Extreme glass cannon; highest attack, lowest HP |
| Knight         | 💂‍♀️     | 120 | 15           | Frontline tank; slow but absorbs punishment |
| Dragon         | 🐉     | 150 | 35           | Inferno every 4th attack; can also shoot fire 🔥 at the opponent from anywhere on the board |
| Zombie         | 🧟     | 100 | 10           | Near-unkillable; wins only through sheer endurance |
| Alien          | 👽     | 70  | 20           | Every 4th attack summons a UFO 🛸 that drops 3 cows 🐄🐄🐄 on the opponent for 45 damage |
| Troll          | 🧌     | 130 | 12           | Immovable wall; hardest to kill, trades damage for durability |
| Computer Virus | 👾     | 60  | 28           | High-burst digital attacker; corrupts targets fast but collapses under pressure |

### Suggested Teams

| Team | Roster | Strategy |
|------|--------|----------|
| Shadow | Vampire, Witch, Ninja | Fast and high-damage; burns through enemies before taking too many hits |
| Iron Wall | Knight, Troll, Zombie | Outlast everything; wins long fights through attrition |
| Balanced | Alien, Goblin, Computer Virus | Flexible mix of damage and survivability |

---

## Running the Game

```bash
javac *.java
java Fight
```

## Resources

- Emojis sourced from [EmojiCopy](https://emojicopy.com/)

## Project Structure

```
Fight/
├── Fight.java       # Entry point
├── Character.java   # Character model
├── Board.java       # 10x10 arena grid
└── README.md
```
