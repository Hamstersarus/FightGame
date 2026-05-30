public class Character {
    static final int HEX_DAMAGE  = 5;
    static final int BURN_DAMAGE = 8;

    // ANSI escape codes — accessible from Opponent (via inheritance) and Fight (via Character.X)
    static final String RESET   = "\033[0m";
    static final String BOLD    = "\033[1m";
    static final String DIM     = "\033[2m";
    static final String RED     = "\033[91m";
    static final String GREEN   = "\033[92m";
    static final String YELLOW  = "\033[93m";
    static final String BLUE    = "\033[94m";
    static final String MAGENTA = "\033[95m";
    static final String CYAN    = "\033[96m";
    static final String WHITE   = "\033[97m";

    String  name;
    int     hp;
    int     attackPower;
    int     shieldHp;
    int     maxShieldHp;
    int     shieldRegenTimer;
    int     shieldRegenDelay;
    boolean shieldActive;
    String  symbol;
    int     row, col;
    int     shieldRow = -1;   // board row of placed shield; -1 = not on board
    int     shieldCol = -1;   // board col of placed shield; -1 = not on board
    boolean shieldOnBoard;    // true when the shield emoji occupies a cell
    boolean insideHouse;         // true while standing on a house cell (immune to damage)
    int     houseTurnsRemaining; // turns of immunity left; auto-eject when this hits 0
    boolean needsHouseRestore;   // true after auto-eject so the house emoji is restored on next move
    boolean hasExtraLife;        // true after picking up a heart; revives at full HP once
    int     hexTurnsRemaining;
    int     burnTurnsRemaining;

    public Character(String name, int hp, int attackPower, String symbol,
                     int maxShieldHp, int shieldRegenDelay) {
        this.name             = name;
        this.hp               = hp;
        this.attackPower      = attackPower;
        this.symbol           = symbol;
        this.maxShieldHp      = maxShieldHp;
        this.shieldHp         = maxShieldHp;
        this.shieldRegenDelay = shieldRegenDelay;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String  getName()               { return name; }
    public int     getHp()                 { return hp; }
    public int     getAttackPower()        { return attackPower; }
    public int     getShieldHp()           { return shieldHp; }
    public int     getMaxShieldHp()        { return maxShieldHp; }
    public int     getShieldRegenTimer()   { return shieldRegenTimer; }
    public int     getShieldRegenDelay()   { return shieldRegenDelay; }
    public boolean isShieldActive()        { return shieldActive; }
    public String  getSymbol()             { return symbol; }
    public int     getRow()                { return row; }
    public int     getCol()                { return col; }
    public int     getHexTurnsRemaining()  { return hexTurnsRemaining; }
    public int     getBurnTurnsRemaining() { return burnTurnsRemaining; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setName(String name)                       { this.name = name; }
    public void setHp(int hp)                              { this.hp = hp; }
    public void setAttackPower(int attackPower)            { this.attackPower = attackPower; }
    public void setShieldHp(int shieldHp)                  { this.shieldHp = shieldHp; }
    public void setMaxShieldHp(int maxShieldHp)            { this.maxShieldHp = maxShieldHp; }
    public void setShieldRegenTimer(int shieldRegenTimer)  { this.shieldRegenTimer = shieldRegenTimer; }
    public void setShieldRegenDelay(int shieldRegenDelay)  { this.shieldRegenDelay = shieldRegenDelay; }
    public void setShieldActive(boolean shieldActive)      { this.shieldActive = shieldActive; }
    public void setSymbol(String symbol)                   { this.symbol = symbol; }
    public void setRow(int row)                            { this.row = row; }
    public void setCol(int col)                            { this.col = col; }
    public void setHexTurnsRemaining(int hexTurnsRemaining)   { this.hexTurnsRemaining = hexTurnsRemaining; }
    public void setBurnTurnsRemaining(int burnTurnsRemaining) { this.burnTurnsRemaining = burnTurnsRemaining; }
}
