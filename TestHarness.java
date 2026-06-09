import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * TestHarness — automated character-vs-character stress test.
 *
 * Runs EVERY character against EVERY character (all 100 ordered matchups,
 * mirror matches included), N times each, twice over (once without map objects,
 * once with). Both sides are driven by the existing opponentAI, so no Scanner
 * input is needed.
 *
 * This is a crash / hang / invalid-state test, NOT a balance tuner. A lopsided
 * matchup is expected and fine. What counts as a failure:
 *   - an exception or AssertionError thrown during a battle
 *   - a battle that never ends (hits MAX_TURNS) — recorded as a timeout
 *
 * Run with assertions on so the core classes' assert preconditions also fire:
 *   javac *.java
 *   java -ea TestHarness
 *
 * Not part of the shipped game — purely a developer tool.
 */
public class TestHarness {

    static final int N         = 20;                    // runs per matchup, per pass
    static final int MAX_TURNS = 1000;                  // round cap before declaring a timeout
    static final int COUNT     = Fight.NAMES.length;    // number of characters (10)

    // The original console stream, captured before any redirection. Used to print
    // the final report after the (silent) sweep finishes.
    static final PrintStream REAL_OUT = System.out;

    // A throwaway stream that discards everything — swallows the game's per-turn output.
    static final PrintStream NULL_OUT = new PrintStream(new OutputStream() {
        @Override public void write(int b) { /* discard */ }
    });

    // Outcome of a single battle, from the perspective of character i (== a).
    enum Result { I_WINS, J_WINS, TIMEOUT }

    // A logged problem: an exception or a timeout, with a human-readable label.
    static class Failure {
        final String kind;    // "EXCEPTION" or "TIMEOUT"
        final String detail;
        Failure(String kind, String detail) { this.kind = kind; this.detail = detail; }
    }

    static final List<Failure> failures = new ArrayList<>();
    static long battlesRun     = 0;
    static long exceptionsCount = 0;
    static long timeoutsCount   = 0;

    public static void main(String[] args) {
        Fight.animationsEnabled = false;          // skip Thread.sleep so the sweep is fast
        Random rng = new Random();
        long start = System.currentTimeMillis();

        // winsI[i][j] = number of times character i beat character j, summed across both passes.
        int[][] winsI = new int[COUNT][COUNT];

        runPass("A (pure combat)", false, rng, winsI);
        runPass("B (with objects)", true,  rng, winsI);

        System.setOut(REAL_OUT);                  // restore the real console for the report
        printReport(winsI, System.currentTimeMillis() - start);
    }

    // Runs every ordered matchup N times for one pass, accumulating wins and failures.
    static void runPass(String passName, boolean withObjects, Random rng, int[][] winsI) {
        for (int i = 0; i < COUNT; i++) {
            for (int j = 0; j < COUNT; j++) {
                for (int run = 0; run < N; run++) {
                    battlesRun++;
                    String label = passName + ": " + Fight.NAMES[i] + " vs " + Fight.NAMES[j];
                    try {
                        Result r = runBattle(i, j, withObjects, rng);
                        if (r == Result.I_WINS) {
                            winsI[i][j]++;
                        } else if (r == Result.TIMEOUT) {
                            timeoutsCount++;
                            failures.add(new Failure("TIMEOUT", label));
                        }
                        // J_WINS needs no tally for the i-perspective matrix.
                    } catch (Throwable t) {
                        // Catch Throwable so AssertionError (from -ea) is captured too,
                        // and so one bad battle never halts the whole sweep.
                        exceptionsCount++;
                        failures.add(new Failure("EXCEPTION", label + "\n        " + describe(t)));
                    }
                }
            }
        }
    }

    // Plays one battle between character i (placed top-left) and character j (bottom-right).
    // Mirrors the round structure of Fight.main: both tick start-of-turn, i acts, j acts,
    // both tick end-of-turn. Returns who won, or TIMEOUT if neither died within MAX_TURNS.
    static Result runBattle(int i, int j, boolean withObjects, Random rng) {
        System.setOut(NULL_OUT);   // silence the game's output for the whole battle

        Opponent a = Fight.createCharacter(i);
        Opponent b = Fight.createCharacter(j);

        Board board = new Board();
        a.row = 0; a.col = 0;
        b.row = 9; b.col = 9;
        board.placeCharacter(a, a.row, a.col);
        board.placeCharacter(b, b.row, b.col);
        if (withObjects) board.spawnObjects(rng);

        for (int round = 1; round <= MAX_TURNS; round++) {
            // Start-of-turn damage-over-time (Hex, Burn) for both — either can be lethal.
            a.tickStartOfTurn();
            if (!a.isAlive()) return Result.J_WINS;
            b.tickStartOfTurn();
            if (!b.isAlive()) return Result.I_WINS;

            // Character i acts (the "player" slot — always first, as in the real game).
            a.opponentAI(b, rng, board);
            if (!b.isAlive()) return Result.I_WINS;

            // Character j acts (the "opponent" slot).
            b.opponentAI(a, rng, board);
            if (!a.isAlive()) return Result.J_WINS;

            // End-of-turn upkeep: shield regen, cooldowns, house countdown, bat-form expiry.
            a.tickEndOfTurn();
            b.tickEndOfTurn();
            board.ejectFromHouseIfExpired(a);
            board.ejectFromHouseIfExpired(b);
        }
        return Result.TIMEOUT;
    }

    // Builds a compact one-liner for a thrown Throwable: type, message, top 3 stack frames.
    static String describe(Throwable t) {
        StringBuilder sb = new StringBuilder(t.getClass().getSimpleName());
        if (t.getMessage() != null) sb.append(": ").append(t.getMessage());
        StackTraceElement[] st = t.getStackTrace();
        for (int k = 0; k < Math.min(3, st.length); k++)
            sb.append("\n          at ").append(st[k]);
        return sb.toString();
    }

    // Prints the full report to the real console: failures first, then legend, matrix, totals.
    static void printReport(int[][] winsI, long ms) {
        System.out.println();
        System.out.println("==================== TEST HARNESS REPORT ====================");
        System.out.println();

        if (failures.isEmpty()) {
            System.out.println("  ALL MATCHUPS CLEAN — no exceptions, no timeouts.");
        } else {
            System.out.println("  " + failures.size() + " ISSUE(S) FOUND:");
            for (Failure f : failures)
                System.out.println("    [" + f.kind + "] " + f.detail);
        }
        System.out.println();

        System.out.println("  Legend (index = character):");
        for (int i = 0; i < COUNT; i++)
            System.out.printf("    %2d = %s %s%n", i, Fight.SYMBOLS[i], Fight.NAMES[i]);
        System.out.println();

        System.out.println("  Win-rate matrix — cell = row character's win% vs column character");
        System.out.println("  (over " + (2 * N) + " runs each; rows act first):");
        System.out.print("        ");
        for (int j = 0; j < COUNT; j++) System.out.printf("%4d", j);
        System.out.println();
        for (int i = 0; i < COUNT; i++) {
            System.out.printf("    %2d  ", i);
            for (int j = 0; j < COUNT; j++) {
                int pct = (int) Math.round(winsI[i][j] * 100.0 / (2 * N));
                System.out.printf("%4d", pct);
            }
            System.out.println();
        }
        System.out.println();

        System.out.println("  Battles run : " + battlesRun);
        System.out.println("  Exceptions  : " + exceptionsCount);
        System.out.println("  Timeouts    : " + timeoutsCount);
        System.out.println("  Time        : " + ms + " ms");
        System.out.println("=============================================================");
    }
}
