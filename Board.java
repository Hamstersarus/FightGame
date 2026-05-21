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
}
