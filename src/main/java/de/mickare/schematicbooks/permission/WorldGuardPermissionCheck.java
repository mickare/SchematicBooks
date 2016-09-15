package de.mickare.schematicbooks.permission;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.bukkit.WGBukkit;

public class WorldGuardPermissionCheck implements PermissionCheck {

  @Override
  public boolean canBuild(Player player, Location location) {
    return WGBukkit.getPlugin().canBuild(player, location);
  }

}
