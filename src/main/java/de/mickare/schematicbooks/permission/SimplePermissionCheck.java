package de.mickare.schematicbooks.permission;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SimplePermissionCheck implements PermissionCheck {

  @Override
  public boolean canBuild(Player player, Location location) {
    return true;
  }

}
