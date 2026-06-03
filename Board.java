import java.util.Random;

public class Board {
    static final int SIZE = 10;
    String[][] grid;

    // Positions of map objects; -1 means not present (consumed or never spawned)
    int houseRow = -1,    houseCol = -1;
    int hospitalRow = -1, hospitalCol = -1;
    int heartRow = -1,    heartCol = -1;

    public Board() {
        grid = new String[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                grid[r][c] = ".";
    }

    public void placeCharacter(Character ch, int row, int col) {
        assert ch != null              : "character must not be null";
        assert row >= 0 && row < SIZE  : "row out of bounds: " + row;
        assert col >= 0 && col < SIZE  : "col out of bounds: " + col;
        grid[row][col] = ch.symbol;
        assert grid[row][col].equals(ch.symbol) : "symbol must be placed after call";
    }

    public void removeCharacter(int row, int col) {
        assert row >= 0 && row < SIZE  : "row out of bounds: " + row;
        assert col >= 0 && col < SIZE  : "col out of bounds: " + col;
        grid[row][col] = ".";
        assert grid[row][col].equals(".") : "cell must be cleared after removal";
    }

    public void display() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                System.out.print(grid[r][c] + " ");
            }
            System.out.println();
        }
    }

    // ── Display ───────────────────────────────────────────────────────────────

    // Prints the board with fixed column positions using ANSI cursor jumps (\033[nG)
    // so emoji width differences in the terminal can't push cells out of alignment.
    void printBoard() {
        final int CELL  = 3;
        final int START = 5;
        System.out.println("  " + Character.CYAN + "┌" + "─".repeat(SIZE * CELL) + Character.RESET);
        for (int r = 0; r < SIZE; r++) {
            System.out.print("  " + Character.CYAN + "│" + Character.RESET + " ");
            for (int c = 0; c < SIZE; c++) {
                System.out.print("\033[" + (START + c * CELL) + "G");
                String cell = grid[r][c];
                System.out.print(cell.equals(".") ? ". " : cell);
            }
            System.out.println();
        }
        System.out.println("  " + Character.CYAN + "└" + "─".repeat(SIZE * CELL) + Character.RESET);
    }

    // ── Map objects ───────────────────────────────────────────────────────────

    // Places house, hospital, and heart on random empty cells at game start.
    void spawnObjects(Random rng) {
        String[] emojis    = { "🏠", "🏥", "💙" };
        int[][]  positions = new int[3][2];
        for (int i = 0; i < emojis.length; i++) {
            int r, c;
            do { r = rng.nextInt(SIZE); c = rng.nextInt(SIZE); }
            while (!grid[r][c].equals("."));
            grid[r][c]     = emojis[i];
            positions[i][0] = r;
            positions[i][1] = c;
        }
        houseRow    = positions[0][0]; houseCol    = positions[0][1];
        hospitalRow = positions[1][0]; hospitalCol = positions[1][1];
        heartRow    = positions[2][0]; heartCol    = positions[2][1];
    }

    // Restores the house emoji when ch moves off the house cell — either voluntarily
    // (insideHouse still true) or after an auto-eject (needsHouseRestore set instead).
    void restoreHouseIfVacating(Opponent ch) {
        if (ch.insideHouse || ch.needsHouseRestore) {
            grid[houseRow][houseCol] = "🏠";
            ch.insideHouse      = false;
            ch.needsHouseRestore = false;
        }
    }

    // Applies the effect of whichever map object was at the destination cell.
    // cellWas must be captured from grid[nr][nc] BEFORE the move executes.
    void checkObjectInteraction(Opponent ch, String cellWas) {
        switch (cellWas) {
            case "🏠" -> {
                ch.insideHouse = true;
                ch.houseTurnsRemaining = 3;
                System.out.println("  🏠 " + ch.name + " ducks into the house! Immune to attacks for 3 turns.");
            }
            case "🏥" -> {
                int healed = Math.min(40, ch.maxHp - ch.hp);
                ch.hp = Math.min(ch.hp + 40, ch.maxHp);
                hospitalRow = -1; hospitalCol = -1;
                System.out.println("  🏥 " + ch.name + " enters the hospital and recovers "
                        + healed + " HP! (" + ch.hp + "/" + ch.maxHp + ")");
            }
            case "💙" -> {
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

    // Called after tickEndOfTurn — ejects ch from the house when their 3-turn stay expires.
    void ejectFromHouseIfExpired(Opponent ch) {
        if (ch.insideHouse && ch.houseTurnsRemaining == 0) {
            ch.insideHouse       = false;
            ch.needsHouseRestore = true;
            System.out.println("  🏠 " + ch.name + "'s 3-turn stay is up — no longer immune.");
        }
    }
}
