/*
 * MODIFIED VERSION OF EXTERNAL SCRIPT, ORIGINAL IS FOUND HERE: https://gist.github.com/Jikoo/30ec040443a4701b8980
 */
package io.github.poorgrammerdev.xpcontrol.external;

import org.bukkit.entity.Player;

/**
 * A utility for managing player experience.
 */
public final class Experience {

  /**
   * Calculate a player's total experience based on level and progress to next.
   *
   * @param player the Player
   * @return the amount of experience the Player has
   *
   * @see <a href=http://minecraft.gamepedia.com/Experience#Leveling_up>Experience#Leveling_up</a>
   */
  public static double getExp(Player player) {
    return getExpFromLevel(player.getLevel())
        + Math.round(getExpToNext(player.getLevel()) * player.getExp());
  }

  /**
   * Calculate total experience based on level.
   *
   * @param level the level
   * @return the total experience calculated
   *
   * @see <a href=http://minecraft.gamepedia.com/Experience#Leveling_up>Experience#Leveling_up</a>
   */
  public static double getExpFromLevel(double level) {
    if (level > 30) {
      return (4.5 * level * level - 162.5 * level + 2220);
    }
    if (level > 15) {
      return (2.5 * level * level - 40.5 * level + 360);
    }
    return level * level + 6 * level;
  }

  /**
   * Calculate level (including progress to next level) based on total experience.
   *
   * @param exp the total experience
   * @return the level calculated
   */
  public static double getLevelFromExp(double exp) {
    double level = getIntLevelFromExp(exp);

    // Get remaining exp progressing towards next level. Cast to float for next bit of math.
    double remainder = exp - getExpFromLevel(level);

    // Get level progress with float precision.
    double progress = remainder / getExpToNext(level);

    // Slap both numbers together and call it a day. While it shouldn't be possible for progress
    // to be an invalid value (value < 0 || 1 <= value)
    return ((double) level) + progress;
  }

  /**
   * Calculate level based on total experience.
   *
   * @param exp the total experience
   * @return the level calculated
   */
  public static double getIntLevelFromExp(double exp) {
    if (exp > 1395D) {
      return ((Math.sqrt(72 * exp - 54215D) + 325) / 18);
    }
    if (exp > 315D) {
      return (Math.sqrt(40 * exp - 7839D) / 10 + 8.1);
    }
    if (exp > 0D) {
      return (Math.sqrt(exp + 9D) - 3);
    }
    return 0;
  }

  /**
   * Get the total amount of experience required to progress to the next level.
   *
   * @param level the current level
   *
   * @see <a href=http://minecraft.gamepedia.com/Experience#Leveling_up>Experience#Leveling_up</a>
   */
  public static double getExpToNext(double level) {
    if (level >= 30D) {
      // Simplified formula. Internal: 112 + (level - 30) * 9
      return level * 9D - 158D;
    }
    if (level >= 15D) {
      // Simplified formula. Internal: 37 + (level - 15) * 5
      return level * 5D - 38D;
    }
    // Internal: 7 + level * 2
    return level * 2D + 7D;
  }

  /**
   * Change a Player's experience.
   *
   * <p>This method is preferred over {@link Player#giveExp(int)}.
   * <br>In older versions the method does not take differences in exp per level into account.
   * This leads to overlevelling when granting players large amounts of experience.
   * <br>In modern versions, while differing amounts of experience per level are accounted for, the
   * approach used is loop-heavy and requires an excessive number of calculations, which makes it
   * quite slow.
   *
   * @param player the Player affected
   * @param exp the amount of experience to add or remove
   */
  public static void changeExp(Player player, int exp) {
    exp += getExp(player);

    if (exp < 0) {
      exp = 0;
    }

    double levelAndExp = getLevelFromExp(exp);
    int level = (int) levelAndExp;
    player.setLevel(level);
    player.setExp((float) (levelAndExp - level));
  }

  private Experience() {}

}
