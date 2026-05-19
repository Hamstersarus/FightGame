public class Opponent extends Character {

    int     maxHp;
    int     abilityCooldown;  // unified cooldown for every special ability
    boolean inBatForm;        // Vampire — set true by useAbility, cleared by tickEndOfTurn
    boolean parryReady;       // Knight  — set true by useAbility, cleared by applyDamage
    boolean hasUndying;       // Zombie  — set true by useAbility, cleared on trigger

    public Opponent(String name, int hp, int attackPower, String symbol,
                    int maxShieldHp, int shieldRegenDelay) {
        super(name, hp, attackPower, symbol, maxShieldHp, shieldRegenDelay);
        this.maxHp = hp;
    }

    // ── Incoming damage resolution ────────────────────────────────────────────

    void applyDamage(int rawDamage) {
        assert rawDamage >= 0 : "rawDamage must be non-negative";

        if (inBatForm) {
            System.out.println("  🦇 " + name + " is a bat — the attack passes right through!");
            return;
        }

        if (parryReady) {
            parryReady = false;
            System.out.println("  💂 PARRY! " + name + " blocks the attack completely!");
            return;
        }

        int incoming = rawDamage;

        if (shieldActive && shieldHp > 0) {
            if (shieldHp >= incoming) {
                shieldHp -= incoming;
                incoming = 0;
                System.out.println("  🛡️  Shield absorbs the hit! ("
                        + shieldHp + "/" + maxShieldHp + " shield HP remaining)");
                if (shieldHp == 0) {
                    shieldActive = false;
                    shieldRegenTimer = shieldRegenDelay;
                    System.out.println("  💥 " + name + "'s shield is broken! Regenerates in "
                            + shieldRegenTimer + " turns.");
                }
            } else {
                incoming -= shieldHp;
                System.out.println("  💥 Shield breaks! " + incoming
                        + " damage bleeds through to " + name + "!");
                shieldHp = 0;
                shieldActive = false;
                shieldRegenTimer = shieldRegenDelay;
            }
        }

        hp -= incoming;
        if (incoming > 0)
            System.out.println("  " + name + " has " + Math.max(0, hp) + " HP remaining.");

        if (hp <= 0 && hasUndying) {
            hp = 1;
            hasUndying = false;
            System.out.println("  🧟 UNDYING! " + name + " refuses to die — rises with 1 HP!");
        }
    }

    // ── Basic attack ──────────────────────────────────────────────────────────

    void attack(Opponent target) {
        assert target != null : "target must not be null";
        assert isAlive()      : "attacker must be alive";
        System.out.println("  " + name + " shoots at " + target.name
                + " for " + attackPower + " damage!");
        target.applyDamage(attackPower);
    }

    // ── Special ability ───────────────────────────────────────────────────────

    boolean canUseAbility() {
        return abilityCooldown == 0;
    }

    boolean isOffensiveAbility() {
        return switch (name) {
            case "Goblin", "Witch", "Ninja", "Dragon", "Alien", "Computer Virus" -> true;
            default -> false;
        };
    }

    String abilityProjectile() {
        return switch (name) {
            case "Dragon"         -> "🔥";
            case "Ninja"          -> "🥷";
            case "Alien"          -> "🛸";
            case "Witch"          -> "🌀";
            case "Computer Virus" -> "💾";
            default               -> "⚡";
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
                System.out.println("  👹 FRENZY! " + name
                        + " attacks with wild rage for " + dmg + " damage!");
                target.applyDamage(dmg);
                abilityCooldown = 3;
            }
            case "Witch" -> {
                target.hexTurnsRemaining = 3;
                System.out.println("  🧙 HEX! " + name + " curses " + target.name
                        + "! (" + HEX_DAMAGE + " HP/turn for 3 turns)");
                abilityCooldown = 4;
            }
            case "Vampire" -> {
                inBatForm = true;
                System.out.println("  🧛 " + name
                        + " transforms into a bat! 🦇 Immune to damage this turn!");
                abilityCooldown = 5;
            }
            case "Ninja" -> {
                System.out.println("  🥷 FIRST STRIKE! " + name
                        + " strikes before the opponent can react!");
                target.applyDamage(attackPower);
                abilityCooldown = 3;
            }
            case "Knight" -> {
                parryReady = true;
                System.out.println("  💂 " + name
                        + " raises their guard! Ready to parry the next attack!");
                abilityCooldown = 4;
            }
            case "Dragon" -> {
                System.out.println("  🐉 " + name + " shoots fire 🔥 at "
                        + target.name + " for 30 damage!");
                target.applyDamage(30);
                abilityCooldown = 3;
            }
            case "Zombie" -> {
                hasUndying = true;
                System.out.println("  🧟 " + name
                        + " embraces death... Undying activated! Survives the next"
                        + " killing blow with 1 HP!");
                abilityCooldown = 5;
            }
            case "Alien" -> {
                System.out.println("  👽 " + name + " summons a UFO! 🛸 Three cows 🐄🐄🐄"
                        + " rain down on " + target.name + "! (45 damage)");
                target.applyDamage(45);
                abilityCooldown = 4;
            }
            case "Troll" -> {
                int healed = Math.min(24, maxHp - hp);
                hp = Math.min(hp + 24, maxHp);
                System.out.println("  🧌 " + name + " surges with regenerative power! +"
                        + healed + " HP  (" + hp + "/" + maxHp + ")");
                abilityCooldown = 5;
            }
            case "Computer Virus" -> {
                int dmg = attackPower + 20;
                System.out.println("  👾 " + name + " corrupts " + target.name
                        + "'s systems! (" + dmg + " corrupted damage)");
                target.applyDamage(dmg);
                abilityCooldown = 4;
            }
        }
    }

    // ── Shield ────────────────────────────────────────────────────────────────

    void raiseShield() {
        assert !shieldActive         : "shield must not already be active";
        assert shieldHp > 0          : "shield must have HP to raise";
        assert shieldRegenTimer == 0 : "shield must be off cooldown";
        shieldActive = true;
        System.out.println("  🛡️  " + name + " raises their shield! ("
                + shieldHp + "/" + maxShieldHp + " shield HP)");
        assert shieldActive : "shield must be active after raising";
    }

    // ── Per-turn ticks ────────────────────────────────────────────────────────

    void tickStartOfTurn() {
        if (hexTurnsRemaining > 0) {
            System.out.println("  🧙 Hex ticks on " + name + "! -" + HEX_DAMAGE + " HP  ("
                    + (hexTurnsRemaining - 1) + " turns left)");
            hp -= HEX_DAMAGE;
            hexTurnsRemaining--;
        }

        if (burnTurnsRemaining > 0) {
            System.out.println("  🔥 Burn ticks on " + name + "! -" + BURN_DAMAGE + " HP  ("
                    + (burnTurnsRemaining - 1) + " turns left)");
            hp -= BURN_DAMAGE;
            burnTurnsRemaining--;
        }
    }

    void tickEndOfTurn() {
        if (!shieldActive && shieldRegenTimer > 0) {
            shieldRegenTimer--;
            if (shieldRegenTimer == 0) {
                shieldHp = maxShieldHp;
                System.out.println("  🛡️  " + name + "'s shield regenerated! ("
                        + shieldHp + "/" + maxShieldHp + " HP) — ready to raise.");
            }
        }

        if (abilityCooldown > 0) abilityCooldown--;

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
}
