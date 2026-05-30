public class Opponent extends Character {

    int     maxHp;
    int     abilityCooldown;  // unified cooldown for every special ability
    boolean inBatForm;        // Vampire — set true by useAbility, cleared by tickEndOfTurn
    boolean parryReady;       // Knight  — set true by useAbility, cleared by applyDamage
    boolean hasUndying;       // Zombie  — set true by useAbility, cleared on trigger
    Board   boardRef;         // set when shield is placed; used to remove it on break

    public Opponent(String name, int hp, int attackPower, String symbol,
                    int maxShieldHp, int shieldRegenDelay) {
        super(name, hp, attackPower, symbol, maxShieldHp, shieldRegenDelay);
        this.maxHp = hp;
    }

    // ── Incoming damage resolution ────────────────────────────────────────────

    void applyDamage(int rawDamage) {
        assert rawDamage >= 0 : "rawDamage must be non-negative";

        if (inBatForm) {
            System.out.println("  " + CYAN + "🦇 " + name + " is a bat — the attack passes right through!" + RESET);
            return;
        }

        if (insideHouse) {
            System.out.println("  " + CYAN + "🏠 " + name + " is sheltered in the house — the attack has no effect!" + RESET);
            return;
        }

        if (parryReady) {
            parryReady = false;
            System.out.println("  " + CYAN + BOLD + "💂 PARRY! " + name + " blocks the attack completely!" + RESET);
            return;
        }

        int incoming = rawDamage;

        if (shieldActive && shieldHp > 0) {
            if (shieldHp >= incoming) {
                shieldHp -= incoming;
                incoming = 0;
                System.out.println("  " + BLUE + "🛡️  Shield absorbs the hit! ("
                        + shieldHp + "/" + maxShieldHp + " shield HP remaining)" + RESET);
                if (shieldHp == 0) {
                    shieldActive = false;
                    shieldRegenTimer = shieldRegenDelay;
                    removeShieldFromBoard();
                    System.out.println("  " + YELLOW + "💥 " + name + "'s shield is broken! Regenerates in "
                            + shieldRegenTimer + " turns." + RESET);
                }
            } else {
                incoming -= shieldHp;
                System.out.println("  " + YELLOW + "💥 Shield breaks! " + RED + incoming
                        + YELLOW + " damage bleeds through to " + name + "!" + RESET);
                shieldHp = 0;
                shieldActive = false;
                shieldRegenTimer = shieldRegenDelay;
                removeShieldFromBoard();
            }
        }

        hp -= incoming;
        if (incoming > 0) {
            int remaining = Math.max(0, hp);
            String hpColor = remaining <= maxHp * 0.25 ? RED : remaining <= maxHp * 0.5 ? YELLOW : GREEN;
            System.out.println("  " + name + " has " + hpColor + remaining + RESET + " HP remaining.");
        }

        if (hp <= 0 && hasUndying) {
            hp = 1;
            hasUndying = false;
            System.out.println("  " + MAGENTA + BOLD + "🧟 UNDYING! " + name + " refuses to die — rises with 1 HP!" + RESET);
        }

        if (hp <= 0 && hasExtraLife) {
            hp = maxHp;
            hasExtraLife = false;
            System.out.println("  " + GREEN + BOLD + "💙 EXTRA LIFE! " + name + " is revived at full HP! (" + hp + "/" + maxHp + ")" + RESET);
        }
    }

    // ── Basic attack ──────────────────────────────────────────────────────────

    void attack(Opponent target) {
        assert target != null : "target must not be null";
        assert isAlive()      : "attacker must be alive";
        System.out.println("  " + name + " shoots at " + target.name
                + " for " + RED + attackPower + RESET + " damage!");
        target.applyDamage(attackPower);
    }

    // ── Special ability ───────────────────────────────────────────────────────

    boolean canUseAbility() {
        return abilityCooldown == 0;
    }

    boolean isOffensiveAbility() {
        return switch (name) {
            case "Goblin", "Witch", "Ninja", "Dragon", "Alien", "Computer Virus",
                 "Vampire", "Knight", "Zombie", "Troll" -> true;
            default -> false;
        };
    }

    String abilityProjectile() {
        return switch (name) {
            case "Goblin"         -> "💢";
            case "Dragon"         -> "🔥";
            case "Ninja"          -> "🥷";
            case "Alien"          -> "🛸";
            case "Witch"          -> "🌀";
            case "Computer Virus" -> "💾";
            case "Vampire"        -> "🩸";
            case "Knight"         -> "⚔️";
            case "Zombie"         -> "🧠";
            case "Troll"          -> "🍃";
            default               -> "•";
        };
    }

    String abilityMenuText() {
        return switch (name) {
            case "Goblin"         -> "Frenzy 👹  (2× attack = " + (attackPower * 2) + " dmg,  cooldown: 3 turns)";
            case "Witch"          -> "Hex 🧙  (curse: " + HEX_DAMAGE + " HP/turn × 3 turns,  cooldown: 4 turns)";
            case "Vampire"        -> "Bat Form 🦇  (immune this turn,  cooldown: 5 turns)";
            case "Ninja"          -> "First Strike 🥷  (attack + skip opponent's turn,  cooldown: 3 turns)";
            case "Knight"         -> "Parry 💂  (block next incoming attack,  cooldown: 4 turns)";
            case "Dragon"         -> "Fire Shot 🔥  (30 damage,  cooldown: 3 turns)";
            case "Zombie"         -> "Undying 🧟  (survive next killing blow with 1 HP,  cooldown: 5 turns)";
            case "Alien"          -> "Cattle Drop 🛸  (45 damage,  cooldown: 4 turns)";
            case "Troll"          -> "Regenerate 🧌  (heal 24 HP,  cooldown: 5 turns)";
            case "Computer Virus" -> "Corrupt 👾  (ATK+20 = " + (attackPower + 20) + " dmg,  cooldown: 4 turns)";
            default               -> "Special Ability";
        };
    }

    void useAbility(Opponent target) {
        assert target != null  : "target must not be null";
        assert canUseAbility() : "ability must be off cooldown";

        switch (name) {
            case "Goblin" -> {
                int dmg = attackPower * 2;
                System.out.println("  " + RED + BOLD + "👹 FRENZY! " + name
                        + " attacks with wild rage for " + dmg + " damage!" + RESET);
                target.applyDamage(dmg);
                abilityCooldown = 3;
            }
            case "Witch" -> {
                target.hexTurnsRemaining = 3;
                System.out.println("  " + MAGENTA + BOLD + "🧙 HEX! " + RESET + MAGENTA + name + " curses " + target.name
                        + "! (" + HEX_DAMAGE + " HP/turn for 3 turns)" + RESET);
                abilityCooldown = 4;
            }
            case "Vampire" -> {
                inBatForm = true;
                System.out.println("  " + CYAN + BOLD + "🧛 " + name
                        + " transforms into a bat! 🦇 Immune to damage this turn!" + RESET);
                abilityCooldown = 5;
            }
            case "Ninja" -> {
                System.out.println("  " + YELLOW + BOLD + "🥷 FIRST STRIKE! " + RESET + YELLOW + name
                        + " strikes before the opponent can react!" + RESET);
                target.applyDamage(attackPower);
                abilityCooldown = 3;
            }
            case "Knight" -> {
                parryReady = true;
                System.out.println("  " + CYAN + BOLD + "💂 " + name
                        + " raises their guard! Ready to parry the next attack!" + RESET);
                abilityCooldown = 4;
            }
            case "Dragon" -> {
                System.out.println("  " + RED + BOLD + "🐉 " + name + " shoots fire 🔥 at "
                        + target.name + " for 30 damage!" + RESET);
                target.applyDamage(30);
                abilityCooldown = 3;
            }
            case "Zombie" -> {
                hasUndying = true;
                System.out.println("  " + MAGENTA + BOLD + "🧟 " + name
                        + " embraces death... Undying activated! Survives the next"
                        + " killing blow with 1 HP!" + RESET);
                abilityCooldown = 5;
            }
            case "Alien" -> {
                System.out.println("  " + RED + BOLD + "👽 " + name + " summons a UFO! 🛸 Three cows 🐄🐄🐄"
                        + " rain down on " + target.name + "! (45 damage)" + RESET);
                target.applyDamage(45);
                abilityCooldown = 4;
            }
            case "Troll" -> {
                int healed = Math.min(24, maxHp - hp);
                hp = Math.min(hp + 24, maxHp);
                System.out.println("  " + GREEN + BOLD + "🧌 " + name + " surges with regenerative power! +"
                        + healed + " HP  (" + hp + "/" + maxHp + ")" + RESET);
                abilityCooldown = 5;
            }
            case "Computer Virus" -> {
                int dmg = attackPower + 20;
                System.out.println("  " + RED + BOLD + "👾 " + name + " corrupts " + target.name
                        + "'s systems! (" + dmg + " corrupted damage)" + RESET);
                target.applyDamage(dmg);
                abilityCooldown = 4;
            }
        }
    }

    // ── Shield ────────────────────────────────────────────────────────────────

    // Returns true if the shield was placed successfully, false if the cell was blocked.
    boolean raiseShield(Board board, int colOffset) {
        assert !shieldActive         : "shield must not already be active";
        assert shieldHp > 0          : "shield must have HP to raise";
        assert shieldRegenTimer == 0 : "shield must be off cooldown";

        int sr = row;
        int sc = col + colOffset;

        if (sc < 0 || sc >= Board.SIZE) return false;
        if (!board.grid[sr][sc].equals("."))  return false;

        board.grid[sr][sc] = "🛡️";
        shieldRow = sr;
        shieldCol = sc;
        shieldOnBoard = true;
        boardRef = board;
        shieldActive = true;
        System.out.println("  " + BLUE + "🛡️  " + name + " raises their shield! ("
                + shieldHp + "/" + maxShieldHp + " shield HP)" + RESET);
        assert shieldActive : "shield must be active after raising";
        return true;
    }

    void removeShieldFromBoard() {
        if (shieldOnBoard && boardRef != null) {
            boardRef.removeCharacter(shieldRow, shieldCol);
            shieldOnBoard = false;
            shieldRow = -1;
            shieldCol = -1;
        }
    }

    // ── Per-turn ticks ────────────────────────────────────────────────────────

    void tickStartOfTurn() {
        if (hexTurnsRemaining > 0) {
            System.out.println("  " + MAGENTA + "🧙 Hex ticks on " + name + "! -" + HEX_DAMAGE + " HP  ("
                    + (hexTurnsRemaining - 1) + " turns left)" + RESET);
            hp -= HEX_DAMAGE;
            hexTurnsRemaining--;
        }

        if (burnTurnsRemaining > 0) {
            System.out.println("  " + RED + "🔥 Burn ticks on " + name + "! -" + BURN_DAMAGE + " HP  ("
                    + (burnTurnsRemaining - 1) + " turns left)" + RESET);
            hp -= BURN_DAMAGE;
            burnTurnsRemaining--;
        }
    }

    void tickEndOfTurn() {
        if (!shieldActive && shieldRegenTimer > 0) {
            shieldRegenTimer--;
            if (shieldRegenTimer == 0) {
                shieldHp = maxShieldHp;
                System.out.println("  " + BLUE + "🛡️  " + name + "'s shield regenerated! ("
                        + shieldHp + "/" + maxShieldHp + " HP) — ready to raise." + RESET);
            }
        }

        if (abilityCooldown > 0) abilityCooldown--;

        if (insideHouse && houseTurnsRemaining > 0) houseTurnsRemaining--;

        if (inBatForm) inBatForm = false;
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    boolean isAlive() {
        return hp > 0;
    }

    void move(int newRow, int newCol, Board board) {
        assert newRow >= 0 && newRow < Board.SIZE : "newRow out of bounds: " + newRow;
        assert newCol >= 0 && newCol < Board.SIZE : "newCol out of bounds: " + newCol;
        assert board != null                      : "board must not be null";
        board.removeCharacter(row, col);
        row = newRow;
        col = newCol;
        board.placeCharacter(this, newRow, newCol);
        assert row == newRow && col == newCol : "position must update after move";
    }

    // Moves the character and drags the shield to the cell at (newRow, newCol + shieldColOffset).
    // Caller is responsible for validating that the new shield cell is in bounds and clear.
    void moveWithShield(int newRow, int newCol, int shieldColOffset, Board board) {
        int newSR = newRow, newSC = newCol + shieldColOffset;
        removeShieldFromBoard();
        move(newRow, newCol, board);
        board.grid[newSR][newSC] = "🛡️";
        shieldRow     = newSR;
        shieldCol     = newSC;
        shieldOnBoard = true;
    }

    // Returns a short shield status string for display in the stat bar (UP / BROKEN / ready).
    String shieldStatusText() {
        if (shieldActive)
            return BLUE + "🛡️  UP  (" + shieldHp + "/" + maxShieldHp + ")" + RESET;
        if (shieldRegenTimer > 0)
            return RED + "🛡️  BROKEN — regen in " + shieldRegenTimer + " turn(s)" + RESET;
        return DIM + "🛡️  ready (" + shieldHp + "/" + maxShieldHp + ")" + RESET;
    }
}
