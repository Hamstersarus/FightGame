public class Opponent extends Character {

    int     maxHp;
    int     turnsAlive;           // Goblin — Frenzy
    int     attackCount;          // Witch — Hex / Dragon — Inferno / Alien — Cattle Drop
    int     turnCount;            // Vampire — Bat Form personal turn counter
    boolean inBatForm;            // Vampire — Bat Form active flag
    int     incomingAttackCount;  // Knight — Parry
    int     fireShotCooldown;     // Dragon — Fire Shot
    boolean hasUndying;           // Zombie — Undying
    int     corruptStacks;        // Computer Virus — Corrupt

    public Opponent(String name, int hp, int attackPower, String symbol,
                    int maxShieldHp, int shieldRegenDelay) {
        super(name, hp, attackPower, symbol, maxShieldHp, shieldRegenDelay);
        this.maxHp      = hp;
        this.hasUndying = name.equals("Zombie");
    }

    // ── Incoming damage resolution ────────────────────────────────────────────

    void applyDamage(int rawDamage) {
        assert rawDamage >= 0 : "rawDamage must be non-negative";
        // Vampire Bat Form — fully immune
        if (inBatForm) {
            System.out.println("  🦇 " + name + " is a bat — the attack passes right through!");
            return;
        }

        // Knight Parry — every 3rd incoming attack is blocked
        if (name.equals("Knight")) {
            incomingAttackCount++;
            if (incomingAttackCount % 3 == 0) {
                System.out.println("  💂 PARRY! " + name + " blocks the attack completely!");
                return;
            }
        }

        int incoming = rawDamage;

        // Shield absorption
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

        // Zombie Undying — survives the first lethal hit with 1 HP
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
        attackCount++;
        int damage = attackPower;

        // Goblin Frenzy — +2 attack per turn survived
        if (name.equals("Goblin"))
            damage = attackPower + (turnsAlive * 2);

        // Computer Virus Corrupt — +4 per stack, then stack grows
        if (name.equals("Computer Virus")) {
            damage = attackPower + (corruptStacks * 4);
            corruptStacks++;
        }

        // Alien Cattle Drop — every 4th attack
        if (name.equals("Alien") && attackCount % 4 == 0) {
            System.out.println("  👽 " + name + " summons a UFO! 🛸 Three cows 🐄🐄🐄 rain down on "
                    + target.name + "! (45 damage)");
            target.applyDamage(45);
            return;
        }

        // Dragon Inferno — every 4th attack
        if (name.equals("Dragon") && attackCount % 4 == 0) {
            int infernoDmg = attackPower * 2;
            System.out.println("  🐉 INFERNO! " + name + " breathes fire for " + infernoDmg + " damage!");
            target.applyDamage(infernoDmg);
            if (target.burnTurnsRemaining == 0) {
                target.burnTurnsRemaining = 3;
                System.out.println("  🔥 " + target.name + " is burning! ("
                        + BURN_DAMAGE + " HP/turn for 3 turns)");
            }
            return;
        }

        // Witch Hex — applies on every 3rd attack
        boolean applyHex = name.equals("Witch") && attackCount % 3 == 0;

        System.out.println("  " + name + " shoots at " + target.name + " for " + damage + " damage!");
        target.applyDamage(damage);

        if (applyHex) {
            target.hexTurnsRemaining = 3;
            System.out.println("  🧙 HEX! " + target.name + " is cursed! ("
                    + HEX_DAMAGE + " HP/turn for 3 turns)");
        }
    }

    // ── Dragon Fire Shot ──────────────────────────────────────────────────────

    void fireShot(Opponent target) {
        assert target != null      : "target must not be null";
        assert canUseFireShot()    : "Fire Shot must be off cooldown";
        System.out.println("  🐉 " + name + " shoots fire 🔥 at " + target.name + " for 30 damage!");
        target.applyDamage(30);
        fireShotCooldown = 3;
        assert fireShotCooldown == 3 : "cooldown must be set after firing";
    }

    boolean canUseFireShot() {
        return name.equals("Dragon") && fireShotCooldown == 0;
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
        // Goblin Frenzy — count turns survived
        if (name.equals("Goblin")) turnsAlive++;

        // Vampire Bat Form — every 5th personal turn
        if (name.equals("Vampire")) {
            turnCount++;
            if (turnCount % 5 == 0) {
                inBatForm = true;
                System.out.println("  🧛 " + name + " transforms into a bat! 🦇 Immune this turn!");
            } else {
                inBatForm = false;
            }
        }

        // Status effect: Hex
        if (hexTurnsRemaining > 0) {
            System.out.println("  🧙 Hex ticks on " + name + "! -" + HEX_DAMAGE + " HP  ("
                    + (hexTurnsRemaining - 1) + " turns left)");
            hp -= HEX_DAMAGE;
            hexTurnsRemaining--;
        }

        // Status effect: Burn
        if (burnTurnsRemaining > 0) {
            System.out.println("  🔥 Burn ticks on " + name + "! -" + BURN_DAMAGE + " HP  ("
                    + (burnTurnsRemaining - 1) + " turns left)");
            hp -= BURN_DAMAGE;
            burnTurnsRemaining--;
        }
    }

    void tickEndOfTurn() {
        // Troll Regeneration — heal 8 HP, capped at maxHp
        if (name.equals("Troll") && hp < maxHp) {
            hp = Math.min(hp + 8, maxHp);
            System.out.println("  🧌 " + name + " regenerates! (" + hp + "/" + maxHp + " HP)");
        }

        // Shield regen countdown
        if (!shieldActive && shieldRegenTimer > 0) {
            shieldRegenTimer--;
            if (shieldRegenTimer == 0) {
                shieldHp = maxShieldHp;
                System.out.println("  🛡️  " + name + "'s shield regenerated! ("
                        + shieldHp + "/" + maxShieldHp + " HP) — ready to raise.");
            }
        }

        // Dragon Fire Shot cooldown
        if (fireShotCooldown > 0) fireShotCooldown--;
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
