import java.util.Random;
import java.util.Scanner;

public class Fight {

    // ── Roster data ───────────────────────────────────────────────────────────

    static final String[] NAMES   = { "Goblin", "Witch", "Vampire", "Ninja", "Knight",
                                      "Dragon", "Zombie", "Alien",  "Troll", "Computer Virus" };
    static final String[] SYMBOLS = { "👹", "🧙", "🧛", "🥷", "💂", "🐉", "🧟", "👽", "🧌", "👾" };
    static final int[]    HP      = {  50,   65,   90,   55,  120,  150,  100,   70,  130,   60 };
    static final int[]    ATTACK  = {  12,   25,   18,   30,   15,   35,   10,   20,   12,   28 };
    static final int[]    SHIELD  = {  15,   15,   20,   10,   40,   35,   25,   20,   30,   15 };
    static final int[]    REGEN   = {   3,    4,    4,    3,    3,    5,    4,    4,    5,    3  };

    static final int[]    POWER   = { 101, 175, 174, 195, 195, 295, 150, 170, 201, 179 };
    //                              Gob  Wit  Vam  Nin  Kni  Dra  Zom  Ali  Tro   CV

    static final String[] FLAVOR  = {
        "Scrappy brawler that gets more dangerous every turn it stays alive.",
        "Glass cannon mage — devastating attack but paper-thin HP.",
        "Durable fighter that turns into a bat every 5th turn, becoming fully immune.",
        "Extreme glass cannon with the highest attack in the game.",
        "Disciplined tank with high HP and a damage-blocking Parry.",
        "The most powerful character — near-impossible to kill, hits like a freight train.",
        "Refuses to die — the first killing blow is always survived with 1 HP.",
        "Unpredictable fighter whose every 4th attack drops 45 damage from a UFO.",
        "Nearly unkillable wall that heals 8 HP every single turn.",
        "Corrupting attacker whose damage escalates with every consecutive hit."
    };

    static final String[] SPECIAL = {
        "Frenzy: +2 ATK per turn survived (no cap)",
        "Hex: every 3rd attack — 5 HP/turn DoT on target for 3 turns",
        "Bat Form: fully immune every 5th turn (skips own attack)",
        "First Strike: attacks before the opponent at the start of every turn",
        "Parry: every 3rd incoming attack is completely blocked",
        "Inferno: every 4th attack deals 70 dmg + Burn  |  Fire Shot 🔥: 30 dmg (3-turn cooldown)",
        "Undying: survives the first killing blow with 1 HP (once per battle)",
        "Cattle Drop: every 4th attack — UFO 🛸 drops 3 cows 🐄🐄🐄 for 45 damage",
        "Regeneration: heals 8 HP at the end of every turn",
        "Corrupt: each attack deals +4 more damage than the last (stacks forever)"
    };

    // ── Helpers ───────────────────────────────────────────────────────────────

    // Reads a line from the user, trims whitespace, and uppercases it so that
    // "w", "W", " w " are all treated identically.
    static String readInput(Scanner scanner) {
        return scanner.nextLine().trim().toUpperCase();
    }

    static Opponent createCharacter(int i) {
        return new Opponent(NAMES[i], HP[i], ATTACK[i], SYMBOLS[i], SHIELD[i], REGEN[i]);
    }

    static void printStats(Opponent player, Opponent opponent) {
        System.out.println();
        System.out.println("  " + player.symbol + " YOU  — HP: " + player.hp + "/" + player.maxHp
                + "  " + shieldStatus(player));
        System.out.println("  " + opponent.symbol + " OPP  — HP: " + opponent.hp + "/" + opponent.maxHp
                + "  " + shieldStatus(opponent));
        System.out.println();
    }

    static String shieldStatus(Opponent ch) {
        if (ch.shieldActive)
            return "🛡️  UP  (" + ch.shieldHp + "/" + ch.maxShieldHp + ")";
        if (ch.shieldRegenTimer > 0)
            return "🛡️  BROKEN — regen in " + ch.shieldRegenTimer + " turn(s)";
        return "🛡️  ready (" + ch.shieldHp + "/" + ch.maxShieldHp + ")";
    }

    static void shootAnimation(String projectile, int delayMs) {
        int steps = 8;
        int width = 36;
        try {
            for (int i = 0; i <= steps; i++) {
                int pos = (i * width) / steps;
                String line = "  " + " ".repeat(pos) + projectile;
                System.out.print("\r" + line + " ".repeat(Math.max(0, width - pos)));
                System.out.flush();
                Thread.sleep(delayMs);
            }
            System.out.print("\r" + " ".repeat(width + 4) + "\r");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static void moveOpponentToward(Opponent opponent, Opponent player, Board board) {
        int rowDiff = player.row - opponent.row;
        int colDiff = player.col - opponent.col;
        if (rowDiff == 0 && colDiff == 0) return;

        int nr = opponent.row;
        int nc = opponent.col;

        if (Math.abs(rowDiff) >= Math.abs(colDiff))
            nr += Integer.compare(rowDiff, 0);
        else
            nc += Integer.compare(colDiff, 0);

        if (nr == player.row && nc == player.col) return;

        opponent.move(nr, nc, board);
        System.out.println("  " + opponent.symbol + " " + opponent.name
                + " advances to (" + nr + ", " + nc + ").");
    }

    static void opponentAI(Opponent opponent, Opponent player, Random rng, Board board) {
        if (opponent.inBatForm) return;

        System.out.println("\n  [" + opponent.symbol + " " + opponent.name + "'s turn]");

        boolean canShield = !opponent.shieldActive
                && opponent.shieldHp > 0
                && opponent.shieldRegenTimer == 0;
        if (canShield && opponent.hp < (int)(opponent.maxHp * 0.45)) {
            opponent.raiseShield();
            return;
        }

        // Vampire Bat Form is useless for the opponent (player already acted)
        boolean willUseAbility = opponent.canUseAbility()
                && !opponent.name.equals("Vampire")
                && rng.nextBoolean();

        if (willUseAbility) {
            if (opponent.isOffensiveAbility())
                shootAnimation(opponent.abilityProjectile(), 250);
            opponent.useAbility(player);
        } else {
            shootAnimation("•", 250);
            opponent.attack(player);
        }

        moveOpponentToward(opponent, player, board);
    }

    // ── Extracted helpers ─────────────────────────────────────────────────────

    static Opponent[] selectCharacters(Scanner scanner, Random rng) {
        System.out.println("╔══════════════════════════════╗");
        System.out.println("║           F I G H T          ║");
        System.out.println("╚══════════════════════════════╝\n");
        System.out.println("Choose your character:\n");
        for (int i = 0; i < NAMES.length; i++) {
            System.out.printf("  %2d. %s  %-16s  HP:%-4d  ATK:%-4d  PWR:%d%n",
                    i + 1, SYMBOLS[i], NAMES[i], HP[i], ATTACK[i], POWER[i]);
            System.out.println("      " + FLAVOR[i]);
            System.out.println("      ✦ " + SPECIAL[i]);
            System.out.println();
        }

        int choice = 0;
        System.out.print("\nEnter number (1-10): ");
        while (choice < 1 || choice > 10) {
            try { choice = Integer.parseInt(scanner.nextLine().trim()); }
            catch (NumberFormatException e) { }
            if (choice < 1 || choice > 10) System.out.print("Enter 1-10: ");
        }

        int playerIdx = choice - 1;
        Opponent player = createCharacter(playerIdx);

        // Matchmaking — prefer opponents within ±35% of the player's power score
        int playerPower = POWER[playerIdx];
        int tolerance   = (int)(playerPower * 0.35);

        java.util.List<Integer> candidates = new java.util.ArrayList<>();
        for (int i = 0; i < NAMES.length; i++) {
            if (i != playerIdx && Math.abs(POWER[i] - playerPower) <= tolerance)
                candidates.add(i);
        }

        int oppIdx;
        if (!candidates.isEmpty()) {
            oppIdx = candidates.get(rng.nextInt(candidates.size()));
        } else {
            // Fallback — closest power score outside the window
            oppIdx = -1;
            int bestDiff = Integer.MAX_VALUE;
            for (int i = 0; i < NAMES.length; i++) {
                if (i == playerIdx) continue;
                int diff = Math.abs(POWER[i] - playerPower);
                if (diff < bestDiff) { bestDiff = diff; oppIdx = i; }
            }
        }

        Opponent opponent = createCharacter(oppIdx);

        System.out.println("\nYou chose:     " + player.symbol + "  " + player.name
                + "  (PWR:" + playerPower + ")");
        System.out.println("Your opponent: " + opponent.symbol + "  " + opponent.name
                + "  (PWR:" + POWER[oppIdx] + ")");

        return new Opponent[] { player, opponent };
    }

    static void handleMove(Scanner scanner, String rawChoice, Opponent player, Opponent opponent, Board board) {
        String dir;
        if (rawChoice.equals("W") || rawChoice.equals("A")
                || rawChoice.equals("S") || rawChoice.equals("D")) {
            dir = rawChoice;
        } else {
            System.out.print("  Direction: ");
            dir = readInput(scanner);
        }
        int nr = player.row, nc = player.col;
        boolean validDir = true;
        switch (dir) {
            case "W" -> nr--;
            case "S" -> nr++;
            case "A" -> nc--;
            case "D" -> nc++;
            default  -> validDir = false;
        }
        if (!validDir) {
            System.out.println("  Unknown direction — staying put.");
        } else if (nr < 0 || nr >= Board.SIZE || nc < 0 || nc >= Board.SIZE) {
            System.out.println("  Edge of board — can't move that way.");
        } else if (nr == opponent.row && nc == opponent.col) {
            System.out.println("  Opponent is there — can't move into them.");
        } else {
            player.move(nr, nc, board);
            System.out.println("  Moved to (" + nr + ", " + nc + ").");
        }
        board.display();
    }

    // Returns true if the player used Ninja First Strike (opponent skips their turn).
    static boolean doPlayerTurn(Scanner scanner, Opponent player, Opponent opponent, Board board) {
        boolean combatActionTaken = false;
        while (!combatActionTaken) {
            printStats(player, opponent);

            System.out.println("  Your turn! Choose an action:");
            System.out.println("    1. Basic Attack");
            int nextOpt = 2;
            int abilityOpt = 0, shieldOpt = 0;

            if (player.canUseAbility()) {
                abilityOpt = nextOpt++;
                System.out.println("    " + abilityOpt + ". " + player.abilityMenuText());
            }
            boolean canShield = !player.shieldActive
                    && player.shieldHp > 0
                    && player.shieldRegenTimer == 0;
            if (canShield) {
                shieldOpt = nextOpt++;
                System.out.println("    " + shieldOpt + ". Raise Shield 🛡️  ("
                        + player.shieldHp + "/" + player.maxShieldHp + " HP)");
            }
            int moveOpt = nextOpt;
            System.out.println("    " + moveOpt + ". Move  (W=up  S=down  A=left  D=right)");

            int playerChoice = 0;
            String rawChoice = "";
            while (playerChoice < 1 || playerChoice > moveOpt) {
                System.out.print("  Choice: ");
                rawChoice = readInput(scanner);
                if (rawChoice.equals("W") || rawChoice.equals("A")
                        || rawChoice.equals("S") || rawChoice.equals("D")) {
                    playerChoice = moveOpt;
                    break;
                }
                try { playerChoice = Integer.parseInt(rawChoice); }
                catch (NumberFormatException e) { }
            }

            if (playerChoice == 1) {
                shootAnimation("•", 80);
                player.attack(opponent);
                combatActionTaken = true;
            } else if (abilityOpt > 0 && playerChoice == abilityOpt) {
                if (player.isOffensiveAbility())
                    shootAnimation(player.abilityProjectile(), 80);
                player.useAbility(opponent);
                combatActionTaken = true;
                if (player.name.equals("Ninja")) return true;  // skip opponent's turn
            } else if (shieldOpt > 0 && playerChoice == shieldOpt) {
                player.raiseShield();
                combatActionTaken = true;
            } else {
                handleMove(scanner, rawChoice, player, opponent, board);
            }
        }
        return false;
    }

    // ── Entry point ───────────────────────────────────────────────────────────

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Random  rng     = new Random();

        Opponent[] chars   = selectCharacters(scanner, rng);
        Opponent   player   = chars[0];
        Opponent   opponent = chars[1];

        Board board = new Board();
        player.row = 0;   player.col = 0;
        opponent.row = 9; opponent.col = 9;
        board.placeCharacter(player,   player.row,   player.col);
        board.placeCharacter(opponent, opponent.row, opponent.col);

        System.out.println("\n=== FIGHT BEGINS ===");
        board.display();

        int turn = 0;
        while (player.isAlive() && opponent.isAlive()) {
            turn++;
            System.out.println("\n════════════════ Turn " + turn + " ════════════════");

            player.tickStartOfTurn();
            if (!player.isAlive()) break;
            opponent.tickStartOfTurn();
            if (!opponent.isAlive()) break;

            // Ninja First Strike — opponent attacks before player's menu (cooldown-gated)
            boolean opponentUsedFirstStrike = false;
            if (opponent.name.equals("Ninja") && opponent.canUseAbility()) {
                System.out.println("  🥷 FIRST STRIKE! " + opponent.name
                        + " moves faster than the eye can see!");
                shootAnimation("🥷", 250);
                opponent.useAbility(player);
                opponentUsedFirstStrike = true;
                if (!player.isAlive()) break;
            }

            boolean skipOpponentTurn = doPlayerTurn(scanner, player, opponent, board);

            if (!opponent.isAlive()) {
                board.removeCharacter(opponent.row, opponent.col);
                break;
            }

            if (!skipOpponentTurn && !opponentUsedFirstStrike) {
                opponentAI(opponent, player, rng, board);
                if (!player.isAlive()) break;
            }

            player.tickEndOfTurn();
            opponent.tickEndOfTurn();

            System.out.println();
            board.display();
        }

        System.out.println();
        board.display();
        System.out.println("\n═══════════════ GAME OVER ═══════════════");
        if (player.isAlive())
            System.out.println("YOU WIN!  " + player.symbol + "  " + player.name + " is victorious!");
        else
            System.out.println("YOU LOSE. " + opponent.symbol + "  " + opponent.name + " defeated you.");

        scanner.close();
    }
}
