import java.util.Random;
import java.util.Scanner;

/**
 * Fight — the entry point and game controller.
 *
 * This class owns all the static roster data (names, stats, power scores) and
 * every method that drives the game: character selection, the main game loop,
 * player input, opponent AI, animations, and board updates.
 *
 * Nothing is split across extra files — all methods live here so the full game
 * flow can be read top-to-bottom in one place.
 *
 * To run:
 *   javac *.java
 *   java Fight          (assertions off)
 *   java -ea Fight      (assertions on — recommended for debugging)
 */
public class Fight {

    // ── Roster data ───────────────────────────────────────────────────────────
    // All character stats are stored as parallel static arrays. Every array has
    // 10 entries in the same index order, so index 0 is always Goblin, index 1
    // is always Witch, and so on. A character's full profile is assembled by
    // looking up the same index across all arrays.
    //
    // Adding a new character means adding one entry to each array — no class
    // changes or subclasses required.

    /** Display names. Index here matches every other array below. */
    static final String[] NAMES   = { "Goblin", "Witch", "Vampire", "Ninja", "Knight",
                                      "Dragon", "Zombie", "Alien",  "Troll", "Computer Virus" };

    /**
     * Emoji symbols. Must be String (not char) because emojis are outside Java's
     * Basic Multilingual Plane and cannot be stored in a single char.
     */
    static final String[] SYMBOLS = { "👹", "🧙", "🧛", "🥷", "💂", "🐉", "🧟", "👽", "🧌", "👾" };

    /** Starting (and maximum) hit points. */
    static final int[]    HP      = {  50,   65,   90,   55,  120,  150,  100,   70,  130,   60 };

    /** Base damage per normal attack. Some abilities scale off this value. */
    static final int[]    ATTACK  = {  12,   25,   18,   30,   15,   35,   10,   20,   12,   28 };

    /** Maximum shield HP — how much damage the shield can absorb before breaking. */
    static final int[]    SHIELD  = {  15,   15,   20,   10,   40,   35,   25,   20,   30,   15 };

    /** Turns the shield takes to regenerate after being broken. */
    static final int[]    REGEN   = {   3,    4,    4,    3,    3,    5,    4,    4,    5,    3  };

    /**
     * Power scores used by the matchmaking system to find a fair opponent.
     * Formula: HP + (ATTACK × 3) + ABILITY_BONUS
     * Higher score = stronger character overall. Dragon (295) is clearly the outlier.
     */
    static final int[]    POWER   = { 101, 175, 174, 195, 195, 295, 150, 170, 201, 179 };
    //                              Gob  Wit  Vam  Nin  Kni  Dra  Zom  Ali  Tro   CV

    /**
     * One-line character descriptions shown on the selection screen.
     * Tells the player the character's general playstyle before they commit.
     */
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

    /**
     * Short special-ability descriptions shown on the selection screen.
     * Gives the player the mechanical summary of what each ability does.
     */
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

    /**
     * Reads one line from the player, trims surrounding whitespace, and converts
     * it to uppercase. This means "w", "W", and " W " are all treated the same way,
     * so the player doesn't have to worry about capitalization or accidental spaces.
     */
    static String readInput(Scanner scanner) {
        return scanner.nextLine().trim().toUpperCase();
    }

    /**
     * Factory method: builds an Opponent using the parallel roster arrays at index i.
     * Keeps the character-creation call sites clean — one line instead of six arguments
     * typed out by hand each time.
     *
     * @param i index into the NAMES/HP/ATTACK/etc. arrays (0 = Goblin, 9 = Computer Virus)
     */
    static Opponent createCharacter(int i) {
        return new Opponent(NAMES[i], HP[i], ATTACK[i], SYMBOLS[i], SHIELD[i], REGEN[i]);
    }

    /**
     * Prints a two-line HP summary for both characters above the action menu.
     * Shows current HP, max HP, and shield status so the player always knows the
     * state of the fight before choosing their action.
     */
    static void printStats(Opponent player, Opponent opponent) {
        System.out.println();
        System.out.println("  " + player.symbol + " YOU  — HP: " + player.hp + "/" + player.maxHp
                + "  " + shieldStatus(player));
        System.out.println("  " + opponent.symbol + " OPP  — HP: " + opponent.hp + "/" + opponent.maxHp
                + "  " + shieldStatus(opponent));
        System.out.println();
    }

    /**
     * Formats a short shield status string for display in the stat bar.
     * Three possible states:
     *   UP      — shield is raised and absorbing damage
     *   BROKEN  — shield was destroyed; shows turns until it regenerates
     *   ready   — shield is fully charged and can be raised as an action
     */
    static String shieldStatus(Opponent ch) {
        if (ch.shieldActive)
            return "🛡️  UP  (" + ch.shieldHp + "/" + ch.maxShieldHp + ")";
        if (ch.shieldRegenTimer > 0)
            return "🛡️  BROKEN — regen in " + ch.shieldRegenTimer + " turn(s)";
        return "🛡️  ready (" + ch.shieldHp + "/" + ch.maxShieldHp + ")";
    }

    /**
     * Animates a projectile sliding across the terminal before damage resolves.
     *
     * Uses carriage return (\r) to overwrite the same line on each frame, creating
     * the illusion of movement without scrolling. The projectile travels from the
     * left margin to the right over 'steps' frames.
     *
     * Player actions use 80 ms/frame (fast and snappy).
     * Opponent actions use 250 ms/frame (slower and more menacing).
     *
     * @param projectile the emoji or character to animate (e.g. "•", "🔥", "🥷")
     * @param delayMs    milliseconds to pause between frames
     */
    static void shootAnimation(String projectile, int delayMs) {
        int steps = 8;    // number of animation frames
        int width = 36;   // total horizontal space the projectile travels across
        try {
            for (int i = 0; i <= steps; i++) {
                // Calculate how far across the line the projectile should be this frame
                int pos = (i * width) / steps;
                String line = "  " + " ".repeat(pos) + projectile;
                // \r moves the cursor back to column 0 so the next print overwrites this line
                System.out.print("\r" + line + " ".repeat(Math.max(0, width - pos)));
                System.out.flush();
                Thread.sleep(delayMs);
            }
            // Clear the animation line completely before returning
            System.out.print("\r" + " ".repeat(width + 4) + "\r");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Moves the opponent one step toward the player on the board.
     *
     * Movement is Manhattan (no diagonals). The opponent steps along whichever axis
     * has the larger gap first. If stepping would land on the player's cell, the move
     * is skipped — characters can't occupy the same cell.
     *
     * Called at the end of opponentAI() after the opponent's action so they close
     * the distance to the player each turn.
     *
     * @param opponent the character to move
     * @param player   the target to move toward
     * @param board    the arena grid to update
     */
    static void moveOpponentToward(Opponent opponent, Opponent player, Board board) {
        int rowDiff = player.row - opponent.row;
        int colDiff = player.col - opponent.col;
        if (rowDiff == 0 && colDiff == 0) return;  // already on top of each other (shouldn't happen)

        int nr = opponent.row;
        int nc = opponent.col;

        // Move along the axis with the larger gap; ties go to rows
        if (Math.abs(rowDiff) >= Math.abs(colDiff))
            nr += Integer.compare(rowDiff, 0);   // +1 or -1 in the row direction
        else
            nc += Integer.compare(colDiff, 0);   // +1 or -1 in the col direction

        // Don't step onto the player's cell or their shield
        if (nr == player.row && nc == player.col) return;
        if (player.shieldOnBoard && nr == player.shieldRow && nc == player.shieldCol) return;

        opponent.move(nr, nc, board);
        System.out.println("  " + opponent.symbol + " " + opponent.name
                + " advances to (" + nr + ", " + nc + ").");
    }

    /**
     * Decides and executes the opponent's action for the turn.
     *
     * The opponent AI follows this priority order:
     *   1. If in Bat Form (Vampire) — skip the turn entirely (already immune)
     *   2. If HP is low (below 45%) and the shield is available — raise it
     *   3. Otherwise, randomly choose between a basic attack and the special ability
     *      (ability is only an option when canUseAbility() is true; Vampire's ability
     *       is excluded because Bat Form is useless after the player has already acted)
     *
     * After attacking or using an ability, the opponent moves one step toward the player.
     *
     * @param opponent the CPU-controlled character
     * @param player   the human player's character (target for attacks)
     * @param rng      shared Random instance for random decisions
     * @param board    the arena grid (for movement)
     */
    static void opponentAI(Opponent opponent, Opponent player, Random rng, Board board) {
        // Vampire in Bat Form is immune but also can't act — just skip the turn
        if (opponent.inBatForm) return;

        System.out.println("\n  [" + opponent.symbol + " " + opponent.name + "'s turn]");

        // Raise shield defensively when health is low and the shield is available
        boolean canShield = !opponent.shieldActive
                && opponent.shieldHp > 0
                && opponent.shieldRegenTimer == 0;
        if (canShield && opponent.hp < (int)(opponent.maxHp * 0.45)) {
            if (opponent.raiseShield(board, -1)) return;
            // cell blocked — fall through to attack instead
        }

        // Decide whether to use the special ability or do a basic attack.
        // Vampire's Bat Form is excluded here because by the time the opponent acts,
        // the player has already attacked — immunity for the rest of the turn is useless.
        boolean willUseAbility = opponent.canUseAbility()
                && !opponent.name.equals("Vampire")
                && rng.nextBoolean();  // 50/50 chance when ability is available

        if (willUseAbility) {
            if (opponent.isOffensiveAbility())
                shootAnimation(opponent.abilityProjectile(), 250);
            opponent.useAbility(player);
        } else {
            shootAnimation("•", 250);
            opponent.attack(player);
        }

        // After attacking, close the distance to the player
        moveOpponentToward(opponent, player, board);
    }

    // ── Extracted helpers ─────────────────────────────────────────────────────

    /**
     * Shows the title screen and roster, takes the player's character choice,
     * and automatically selects a fair opponent using the matchmaking system.
     *
     * Matchmaking algorithm:
     *   1. Calculate a tolerance window of ±35% around the player's power score
     *   2. Collect all characters whose POWER score falls within that window
     *      (excluding the character the player chose)
     *   3. Pick randomly from the candidates
     *   4. If no candidates exist (Dragon is often isolated at 295), fall back to
     *      whichever character has the closest power score to the player's
     *
     * @return array of exactly two Opponents: [0] = player, [1] = opponent
     */
    static Opponent[] selectCharacters(Scanner scanner, Random rng) {
        System.out.println("╔══════════════════════════════╗");
        System.out.println("║           F I G H T          ║");
        System.out.println("╚══════════════════════════════╝\n");
        System.out.println("Choose your character:\n");

        // Print the full roster with stats, flavor text, and ability description
        for (int i = 0; i < NAMES.length; i++) {
            System.out.printf("  %2d. %s  %-16s  HP:%-4d  ATK:%-4d  PWR:%d%n",
                    i + 1, SYMBOLS[i], NAMES[i], HP[i], ATTACK[i], POWER[i]);
            System.out.println("      " + FLAVOR[i]);
            System.out.println("      ✦ " + SPECIAL[i]);
            System.out.println();
        }

        // Input validation loop — keep asking until the player enters 1–10
        int choice = 0;
        System.out.print("\nEnter number (1-10): ");
        while (choice < 1 || choice > 10) {
            try { choice = Integer.parseInt(scanner.nextLine().trim()); }
            catch (NumberFormatException e) { }   // non-numeric input — just re-prompt
            if (choice < 1 || choice > 10) System.out.print("Enter 1-10: ");
        }

        int playerIdx    = choice - 1;   // convert from 1-based user input to 0-based array index
        Opponent player  = createCharacter(playerIdx);

        // ── Matchmaking ───────────────────────────────────────────────────────
        // Find opponents whose power is within ±35% of the player's power score.
        // This avoids obviously unfair matchups (e.g. Goblin vs Dragon).
        int playerPower = POWER[playerIdx];
        int tolerance   = (int)(playerPower * 0.35);

        java.util.List<Integer> candidates = new java.util.ArrayList<>();
        for (int i = 0; i < NAMES.length; i++) {
            if (i != playerIdx && Math.abs(POWER[i] - playerPower) <= tolerance)
                candidates.add(i);
        }

        int oppIdx;
        if (!candidates.isEmpty()) {
            // Normal case: pick randomly from the pool of fair opponents
            oppIdx = candidates.get(rng.nextInt(candidates.size()));
        } else {
            // Fallback: no one is within 35% (most likely because the player chose Dragon).
            // Find whoever has the closest power score to minimize the skill gap.
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

    /**
     * Handles the Move action when the player chooses to reposition on the board.
     *
     * Accepts the direction either from the already-read rawChoice (if the player
     * typed W/A/S/D directly at the main menu) or by prompting again. Validates:
     *   - the direction letter is one of W/A/S/D
     *   - the destination is within the 10×10 grid boundaries
     *   - the destination is not already occupied by the opponent
     *
     * Moving does NOT end the turn — the action menu loops back after a move so the
     * player can move multiple times before attacking or using an ability.
     *
     * @param rawChoice the string the player already typed (may be a direction or something else)
     */
    static void handleMove(Scanner scanner, String rawChoice, Opponent player, Opponent opponent, Board board) {
        // If rawChoice is already a direction, use it; otherwise ask for one
        String dir;
        if (rawChoice.equals("W") || rawChoice.equals("A")
                || rawChoice.equals("S") || rawChoice.equals("D")) {
            dir = rawChoice;
        } else {
            System.out.print("  Direction: ");
            dir = readInput(scanner);
        }

        // Calculate the destination cell based on the direction
        int nr = player.row, nc = player.col;
        boolean validDir = true;
        switch (dir) {
            case "W" -> nr--;   // up (lower row index)
            case "S" -> nr++;   // down (higher row index)
            case "A" -> nc--;   // left (lower column index)
            case "D" -> nc++;   // right (higher column index)
            default  -> validDir = false;
        }

        if (!validDir) {
            System.out.println("  Unknown direction — staying put.");
        } else if (nr < 0 || nr >= Board.SIZE || nc < 0 || nc >= Board.SIZE) {
            System.out.println("  Edge of board — can't move that way.");
        } else if (nr == opponent.row && nc == opponent.col) {
            System.out.println("  Opponent is there — can't move into them.");
        } else if (opponent.shieldOnBoard && nr == opponent.shieldRow && nc == opponent.shieldCol) {
            System.out.println("  Opponent's shield is there — can't move into it.");
        } else if (player.shieldOnBoard && nr == player.shieldRow && nc == player.shieldCol) {
            System.out.println("  Your own shield is there — move backwards to get away from it.");
        } else {
            player.move(nr, nc, board);
            System.out.println("  Moved to (" + nr + ", " + nc + ").");
        }

        // Always redraw the board after a move attempt so the player sees the result
        board.display();
    }

    /**
     * Runs the player's full action for one turn, looping until a combat action is taken.
     *
     * The action menu is shown every iteration. The player can:
     *   1. Basic Attack       — always available; ends the turn
     *   2. Special Ability    — shown only when canUseAbility() is true; ends the turn
     *   3. Raise Shield       — shown only when the shield is available; ends the turn
     *   4. Move (or W/A/S/D)  — repositions on the board; does NOT end the turn
     *
     * Moving loops back to the same menu, so the player can move freely until they
     * decide to commit to a combat action. The board state is shown after each move.
     *
     * Ninja special case: if the player is a Ninja and uses First Strike, this method
     * returns true to signal that the opponent should skip their turn this round.
     *
     * @return true if the Ninja used First Strike (opponent skips turn), false otherwise
     */
    static boolean doPlayerTurn(Scanner scanner, Opponent player, Opponent opponent, Board board) {
        boolean combatActionTaken = false;
        while (!combatActionTaken) {
            printStats(player, opponent);

            System.out.println("  Your turn! Choose an action:");
            System.out.println("    1. Basic Attack");
            int nextOpt = 2;
            int abilityOpt = 0, shieldOpt = 0;

            // Show ability option only when it's off cooldown
            if (player.canUseAbility()) {
                abilityOpt = nextOpt++;
                System.out.println("    " + abilityOpt + ". " + player.abilityMenuText());
            }

            // Show shield option only when the shield is available to raise
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

            // Read the player's choice, accepting either a number or a WASD shortcut
            int playerChoice = 0;
            String rawChoice = "";
            while (playerChoice < 1 || playerChoice > moveOpt) {
                System.out.print("  Choice: ");
                rawChoice = readInput(scanner);
                // WASD typed directly at this prompt counts as the Move option
                if (rawChoice.equals("W") || rawChoice.equals("A")
                        || rawChoice.equals("S") || rawChoice.equals("D")) {
                    playerChoice = moveOpt;
                    break;
                }
                try { playerChoice = Integer.parseInt(rawChoice); }
                catch (NumberFormatException e) { }  // non-numeric — loop and re-prompt
            }

            if (playerChoice == 1) {
                // Basic Attack — fire a projectile then deal attackPower damage
                shootAnimation("•", 80);
                player.attack(opponent);
                combatActionTaken = true;
            } else if (abilityOpt > 0 && playerChoice == abilityOpt) {
                // Special Ability — animate if offensive, then execute
                if (player.isOffensiveAbility())
                    shootAnimation(player.abilityProjectile(), 80);
                player.useAbility(opponent);
                combatActionTaken = true;
                // Ninja First Strike causes the opponent to skip their turn this round
                if (player.name.equals("Ninja")) return true;
            } else if (shieldOpt > 0 && playerChoice == shieldOpt) {
                // Raise Shield — only ends the turn if placement succeeded
                if (player.raiseShield(board, 1)) combatActionTaken = true;
            } else {
                // Move — does not end the turn; loops back to the action menu
                handleMove(scanner, rawChoice, player, opponent, board);
            }
        }
        return false;  // normal turn end — opponent acts next
    }

    // ── Entry point ───────────────────────────────────────────────────────────

    /**
     * Main game loop. Runs the full game from character selection to game over.
     *
     * Flow:
     *   1. selectCharacters() — title screen, roster, player picks, matchmaking assigns opponent
     *   2. Board setup — player starts top-left (0,0), opponent starts bottom-right (9,9)
     *   3. Turn loop:
     *        a. tickStartOfTurn() for both — apply DoT (Hex, Burn)
     *        b. Ninja First Strike check — if the opponent is a Ninja with ability ready,
     *           they attack before the player's menu appears (not a full turn; just the strike)
     *        c. doPlayerTurn() — player chooses and executes their action
     *        d. opponentAI() — opponent acts (skipped if player used Ninja First Strike
     *           or if the opponent already used their Ninja First Strike this turn)
     *        e. tickEndOfTurn() for both — shield regen, ability cooldowns, Bat Form expiry
     *        f. board.display() — show updated positions
     *   4. Game over — print result and close the scanner
     */
    public static void main(String[] args) {
        Random rng = new Random();
        try (Scanner scanner = new Scanner(System.in)) {

        // Step 1: Character selection and matchmaking
        Opponent[] chars   = selectCharacters(scanner, rng);
        Opponent   player   = chars[0];
        Opponent   opponent = chars[1];

        // Step 2: Place characters at opposite corners of the 10×10 board
        Board board = new Board();
        player.row = 0;   player.col = 0;      // top-left corner
        opponent.row = 9; opponent.col = 9;    // bottom-right corner
        board.placeCharacter(player,   player.row,   player.col);
        board.placeCharacter(opponent, opponent.row, opponent.col);

        System.out.println("\n=== FIGHT BEGINS ===");
        board.display();

        // Step 3: Main turn loop — continues until one character runs out of HP
        int turn = 0;
        while (player.isAlive() && opponent.isAlive()) {
            turn++;
            System.out.println("\n════════════════ Turn " + turn + " ════════════════");

            // 3a. Apply start-of-turn status effects (Hex, Burn) to both characters.
            // Check isAlive() immediately — a DoT tick could be the killing blow.
            player.tickStartOfTurn();
            if (!player.isAlive()) break;
            opponent.tickStartOfTurn();
            if (!opponent.isAlive()) break;

            // 3b. Opponent Ninja First Strike — happens before the player's menu.
            // This is separate from the opponent's normal turn: it only fires if the
            // opponent IS a Ninja AND the ability is off cooldown. If it kills the
            // player, the loop breaks immediately. opponentUsedFirstStrike is tracked
            // so we don't give the opponent a second action later in the same turn.
            boolean opponentUsedFirstStrike = false;
            if (opponent.name.equals("Ninja") && opponent.canUseAbility()) {
                System.out.println("  🥷 FIRST STRIKE! " + opponent.name
                        + " moves faster than the eye can see!");
                shootAnimation("🥷", 250);
                opponent.useAbility(player);
                opponentUsedFirstStrike = true;
                if (!player.isAlive()) break;
            }

            // 3c. Player's turn — returns true if the player used Ninja First Strike
            boolean skipOpponentTurn = doPlayerTurn(scanner, player, opponent, board);

            // Check if the opponent was killed by the player's action
            if (!opponent.isAlive()) {
                board.removeCharacter(opponent.row, opponent.col);
                break;
            }

            // 3d. Opponent's turn — skipped if:
            //   - The player's Ninja First Strike was used (skipOpponentTurn = true), OR
            //   - The opponent already used their Ninja First Strike at the top of this turn
            if (!skipOpponentTurn && !opponentUsedFirstStrike) {
                opponentAI(opponent, player, rng, board);
                if (!player.isAlive()) break;
            }

            // 3e. End-of-turn upkeep for both characters
            player.tickEndOfTurn();
            opponent.tickEndOfTurn();

            // 3f. Show the current board state
            System.out.println();
            board.display();
        }

        // Step 4: Game over — show final board and result
        System.out.println();
        board.display();
        System.out.println("\n═══════════════ GAME OVER ═══════════════");
        if (player.isAlive())
            System.out.println("YOU WIN!  " + player.symbol + "  " + player.name + " is victorious!");
        else
            System.out.println("YOU LOSE. " + opponent.symbol + "  " + opponent.name + " defeated you.");
        }  // end try-with-resources (scanner closed automatically)
    }
}
