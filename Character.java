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

    String  name; // e.g. "Knight", "Archer", etc. Used in status messages and character selection.
    int     hp; // current health points; if this drops to 0 or below, the character dies
    int     attackPower; // how much damage the character's basic attack does; also used in some special attacks
    int     shieldHp; // current HP of the character's shield; when this is above 0, it absorbs damage before HP is affected
    int     maxShieldHp; // the maximum HP of the shield; used to restore the shield to full strength when it's activated or fully regenerated
    int     shieldRegenTimer; // counts down the turns until the shield starts regenerating after taking damage; when this hits 0, shieldHp starts increasing by 1 each turn until it reaches maxShieldHp
    int     shieldRegenDelay; // how many turns the shield must wait to start regenerating after taking damage; set by each character's constructor and used to reset shieldRegenTimer whenever the shield takes damage
    boolean shieldActive; // true when the shield is currently active on the board (represented by the 🛡️ emoji); this is separate from shieldHp > 0 because the shield can be temporarily deactivated and removed from the board while it regenerates, even if it still has HP left
    String  symbol; // the emoji that represents the character on the board; set by each character's constructor and used to update the board grid when the character moves
    int     row, col; // the character's current position on the board grid; updated whenever the character moves and used to determine interactions with map objects and the opponent
    int     shieldRow = -1;   // board row of placed shield; -1 = not on board
    int     shieldCol = -1;   // board col of placed shield; -1 = not on board
    boolean shieldOnBoard;    // true when the shield emoji occupies a cell
    boolean insideHouse;         // true while standing on a house cell (immune to damage)
    int     houseTurnsRemaining; // turns of immunity left; auto-eject when this hits 0
    boolean needsHouseRestore;   // true after auto-eject so the house emoji is restored on next move
    boolean hasExtraLife;        // true after picking up a heart; revives at full HP once
    int     hexTurnsRemaining; // turns left of hex status effect; while this is above 0, the character takes 5 damage at the end of each turn and can't heal or regenerate their shield
    int     burnTurnsRemaining; // turns left of burn status effect; while this is above 0, the character takes 8 damage at the end of each turn and can't heal or regenerate their shield

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

}
