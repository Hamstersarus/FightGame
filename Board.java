import java.util.Random;

/**
 * Board — the 10×10 arena grid.
 *
 * Stores the game board as a 2D array of strings. Each cell holds either "."
 * (empty), a character emoji, the shield emoji "🛡️", or a map object emoji.
 * Also owns all map-object logic: spawning, interaction effects, and house
 * duration management.
 */
public class Board {

    // Every board is always SIZE × SIZE. Changing this one constant resizes everything.
    static final int SIZE = 10;

    // The grid itself. Each cell is a String so it can hold multi-codepoint emoji.
    String[][] grid;

    // Tracked positions of the three map objects.
    // Set to -1 when the object has been consumed (hospital, heart) or was never placed.
    int houseRow = -1,    houseCol = -1;
    int hospitalRow = -1, hospitalCol = -1;
    int heartRow = -1,    heartCol = -1;

    // ── Constructor ───────────────────────────────────────────────────────────

    public Board() {
        grid = new String[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                grid[r][c] = ".";   // "." means the cell is empty
    }

    // ── Grid primitives ───────────────────────────────────────────────────────

    // Writes a character's emoji symbol into the cell at (row, col).
    public void placeCharacter(Character ch, int row, int col) {
        assert ch != null              : "character must not be null";
        assert row >= 0 && row < SIZE  : "row out of bounds: " + row;
        assert col >= 0 && col < SIZE  : "col out of bounds: " + col;
        grid[row][col] = ch.symbol;
        assert grid[row][col].equals(ch.symbol) : "symbol must be placed after call";
    }

    // Clears the cell at (row, col) back to ".".
    public void removeCharacter(int row, int col) {
        assert row >= 0 && row < SIZE  : "row out of bounds: " + row;
        assert col >= 0 && col < SIZE  : "col out of bounds: " + col;
        grid[row][col] = ".";
        assert grid[row][col].equals(".") : "cell must be cleared after removal";
    }

    // Legacy plain display — no border or color. Kept for debugging convenience.
    public void display() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                System.out.print(grid[r][c] + " ");
            }
            System.out.println();
        }
    }

    // ── Display ───────────────────────────────────────────────────────────────

    // Prints the board with a cyan border and fixed column positions.
    // Uses ANSI cursor jumps (\033[nG) to move to an absolute column before each
    // cell, so emoji that render at different widths in the terminal can never
    // push adjacent cells out of alignment.
    void printBoard() {
        final int CELL  = 3;  // display columns reserved per cell
        final int START = 5;  // 1-based column where cell 0 begins (after "  │ ")
        System.out.println("  " + Character.CYAN + "┌" + "─".repeat(SIZE * CELL) + Character.RESET);
        for (int r = 0; r < SIZE; r++) {
            System.out.print("  " + Character.CYAN + "│" + Character.RESET + " ");
            for (int c = 0; c < SIZE; c++) {
                System.out.print("\033[" + (START + c * CELL) + "G");
                String cell = grid[r][c];
                // Dot cells are 1 display-char wide; add a space so they match emoji width.
                System.out.print(cell.equals(".") ? ". " : cell);
            }
            System.out.println();
        }
        System.out.println("  " + Character.CYAN + "└" + "─".repeat(SIZE * CELL) + Character.RESET);
    }

    // ── Map objects ───────────────────────────────────────────────────────────

    // Randomly places the house 🏠, hospital 🏥, and heart 💙 on three distinct
    // empty cells at the start of the game. Called once from main() after both
    // characters have been placed on the board.
    void spawnObjects(Random rng) {
        String[] emojis    = { "🏠", "🏥", "💙" };
        int[][]  positions = new int[3][2];
        for (int i = 0; i < emojis.length; i++) {
            int r, c;
            // Keep picking random cells until we land on an empty one.
            do { r = rng.nextInt(SIZE); c = rng.nextInt(SIZE); }
            while (!grid[r][c].equals("."));
            grid[r][c]      = emojis[i];
            positions[i][0] = r;
            positions[i][1] = c;
        }
        houseRow    = positions[0][0]; houseCol    = positions[0][1];
        hospitalRow = positions[1][0]; hospitalCol = positions[1][1];
        heartRow    = positions[2][0]; heartCol    = positions[2][1];
    }

    // When a character moves off the house cell, restores the 🏠 emoji so the
    // house stays visible and re-enterable. Called after every successful move.
    // Handles both voluntary exits (insideHouse still true) and auto-ejects
    // (needsHouseRestore set by ejectFromHouseIfExpired).
    void restoreHouseIfVacating(Opponent ch) {
        if (ch.insideHouse || ch.needsHouseRestore) {
            grid[houseRow][houseCol] = "🏠";
            ch.insideHouse       = false;
            ch.needsHouseRestore = false;
        }
    }

    // Checks whether the cell a character just moved onto holds a map object
    // and applies its effect. cellWas must be captured from grid[nr][nc] BEFORE
    // the move executes, because the move overwrites that cell with the character's symbol.
    void checkObjectInteraction(Opponent ch, String cellWas) {
        switch (cellWas) {
            case "🏠" -> {
                // House: grants 3 turns of full damage immunity.
                ch.insideHouse         = true;
                ch.houseTurnsRemaining = 3;
                System.out.println("  🏠 " + ch.name + " ducks into the house! Immune to attacks for 3 turns.");
            }
            case "🏥" -> {
                // Hospital: heals up to 40 HP, capped at maxHp. Consumed on use.
                int healed = Math.min(40, ch.maxHp - ch.hp);
                ch.hp = Math.min(ch.hp + 40, ch.maxHp);
                hospitalRow = -1; hospitalCol = -1;
                System.out.println("  🏥 " + ch.name + " enters the hospital and recovers "
                        + healed + " HP! (" + ch.hp + "/" + ch.maxHp + ")");
            }
            case "💙" -> {
                // Heart: grants one extra life (full HP revival on death). Consumed on pickup.
                // A character who already has an extra life can't pick up another one.
                if (!ch.hasExtraLife) {
                    ch.hasExtraLife = true;
                    heartRow = -1; heartCol = -1;
                    System.out.println("  💙 " + ch.name + " picks up an extra life!");
                } else {
                    System.out.println("  💙 " + ch.name + " already has an extra life — can't pick this up.");
                }
            }
        }
    }

    // Checks at the end of each turn whether a character's house stay has expired.
    // If so, strips immunity and sets needsHouseRestore so the house emoji gets
    // put back the next time the character moves off that cell.
    void ejectFromHouseIfExpired(Opponent ch) {
        if (ch.insideHouse && ch.houseTurnsRemaining == 0) {
            ch.insideHouse       = false;
            ch.needsHouseRestore = true;
            System.out.println("  🏠 " + ch.name + "'s 3-turn stay is up — no longer immune.");
        }
    }
}
