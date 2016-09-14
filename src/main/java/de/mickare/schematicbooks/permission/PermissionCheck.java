package de.mickare.schematicbooks.permission;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import de.mickare.schematicbooks.data.SchematicEntity;
import de.mickare.schematicbooks.util.IntRegion;

public interface PermissionCheck {

  boolean canBuild(Player player, Location location);

  default boolean canBuild(Player player, World world, IntRegion region) {
    final Location loc = new Location(world, 0, 0, 0);
    return !region.positions().sequential()//
        .map(p -> p.setLocation(loc))//
        .filter(l -> !canBuild(player, l))//
        .findFirst().isPresent();
  }


  default boolean canBuild(Player player, World world, SchematicEntity entity) {
    return canBuild(player, world, entity.getHitBox());
  }

}
