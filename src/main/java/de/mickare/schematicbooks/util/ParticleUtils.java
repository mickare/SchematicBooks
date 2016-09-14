package de.mickare.schematicbooks.util;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import de.mickare.schematicbooks.data.SchematicEntity;

public class ParticleUtils {


  public static void showColoredDust(Location location, int r, int g, int b, float speed,
      int particleCount, int radius) {
    location.getWorld().spigot().playEffect(location, Effect.COLOURED_DUST, 0, 0, r / 255f,
        g / 255f, b / 255f, speed, particleCount, radius);
  }


  public static void showParticles(final World world, final SchematicEntity entity) {
    IntVector start = entity.getHitBox().getPos1();
    final int r = start.getX() * 41 % 200;
    final int g = start.getY() * 41 % 200;
    final int b = start.getZ() * 41 % 200;
    showParticles(world, entity, r, g, b);
  }


  public static void showParticles(final World world, final SchematicEntity entity, final int r,
      final int g, final int b) {
    IntRegion box = new IntRegion(entity.getHitBox().getMinPoint(),
        entity.getHitBox().getMaxPoint().add(1, 1, 1));
    box.positions().map(p -> p.toLocation(world)).forEach(l -> {
      ParticleUtils.showColoredDust(l.add(0, 0, 0), r, g, b, 0.5f, 0, 16);
    });
  }


  public static void showParticlesForTime(final Plugin plugin, final int ticks, final World world,
      final SchematicEntity entity) {
    IntVector start = entity.getHitBox().getPos1();
    final int r = start.getX() * 41 % 200;
    final int g = start.getY() * 41 % 200;
    final int b = start.getZ() * 41 % 200;
    showParticlesForTime(plugin, ticks, world, entity, r, g, b);
  }

  private static final int TICKS_PER_PARTICLE = 5;

  public static void showParticlesForTime(final Plugin plugin, final int ticks, final World world,
      final SchematicEntity entity, final int r, final int g, final int b) {
    new BukkitRunnable() {

      final int _ticks = ticks / TICKS_PER_PARTICLE;
      int count = 0;

      @Override
      public void run() {
        count++;
        if (count > _ticks) {
          this.cancel();
          return;
        }
        showParticles(world, entity, r, g, b);
      }

    }.runTaskTimerAsynchronously(plugin, TICKS_PER_PARTICLE, TICKS_PER_PARTICLE);
  }


}
