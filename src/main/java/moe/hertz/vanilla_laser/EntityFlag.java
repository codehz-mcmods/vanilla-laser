package moe.hertz.vanilla_laser;

public enum EntityFlag {
  ON_FIRE(0),
  SNEAKING(1),
  SPRINTING(3),
  SWIMMING(4),
  INVISIBLE(5),
  GLOWING(6),
  FALLFLYING(7);

  private int id;

  private EntityFlag(int id) {
    this.id = id;
  }

  public static byte of(EntityFlag...flags) {
    byte res = 0;
    for (var flag : flags) {
      res |= 1 << flag.id;
    }
    return res;
  }
}
