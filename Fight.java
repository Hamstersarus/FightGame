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
            return;  // defensive — stay put while shielding
        }

        if (opponent.canUseFireShot() && rng.nextBoolean()) {
            opponent.fireShot(player);
        } else {
            opponent.attack(player);
        }

        moveOpponentToward(opponent, player, board);
    }

    // ── Entry point ───────────────────────────────────────────────────────────

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Random  rng     = new Random();

        System.out.println("╔══════════════════════════════╗");
        System.out.println("║           F I G H T          ║");
        System.out.println("╚══════════════════════════════╝\n");
        System.out.println("Choose your character:\n");
        for (int i = 0; i < NAMES.length; i++)
            System.out.printf("  %2d. %s  %-16s  HP:%-4d  ATK:%d%n",
                    i + 1, SYMBOLS[i], NAMES[i], HP[i], ATTACK[i]);

        int choice = 0;
        System.out.print("\nEnter number (1-10): ");
        while (choice < 1 || choice > 10) {
            try { choice = Integer.parseInt(scanner.nextLine().trim()); }
            catch (NumberFormatException e) { }
            if (choice < 1 || choice > 10) System.out.print("Enter 1-10: ");
        }

        int playerIdx = choice - 1;
        Opponent player = createCharacter(playerIdx);

        int oppIdx;
        do { oppIdx = rng.nextInt(NAMES.length); } while (oppIdx == playerIdx);
        Opponent opponent = createCharacter(oppIdx);

        System.out.println("\nYou chose:     " + player.symbol   + "  " + player.name);
        System.out.println("Your opponent: " + opponent.symbol + "  " + opponent.name);

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

            boolean opponentIsNinja = opponent.name.equals("Ninja");
            if (opponentIsNinja) {
                System.out.println("  🥷 FIRST STRIKE! " + opponent.name
                        + " moves faster than the eye can see!");
                opponent.attack(player);
                if (!player.isAlive()) break;
            }

            if (player.inBatForm) {
                printStats(player, opponent);
                System.out.println("  🦇 You are in bat form this turn — immune but cannot act.");
            } else {
                boolean combatActionTaken = false;
                while (!combatActionTaken) {
                    printStats(player, opponent);

                    System.out.println("  Your turn! Choose an action:");
                    System.out.println("    1. Basic Attack");
                    int nextOpt = 2;
                    int fireShotOpt = 0, shieldOpt = 0;

                    if (player.canUseFireShot()) {
                        fireShotOpt = nextOpt++;
                        System.out.println("    " + fireShotOpt + ". Fire Shot 🔥  (deals 30, 3-turn cooldown)");
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
                        player.attack(opponent);
                        combatActionTaken = true;
                    } else if (fireShotOpt > 0 && playerChoice == fireShotOpt) {
                        player.fireShot(opponent);
                        combatActionTaken = true;
                    } else if (shieldOpt > 0 && playerChoice == shieldOpt) {
                        player.raiseShield();
                        combatActionTaken = true;
                    } else {
                        // Movement — opponent does not act; loop back for another action
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
                }
            }

            if (!opponent.isAlive()) {
                board.removeCharacter(opponent.row, opponent.col);
                break;
            }

            if (!opponentIsNinja) {
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
