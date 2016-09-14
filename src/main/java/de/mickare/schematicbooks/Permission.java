package de.mickare.schematicbooks;

import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.base.Preconditions;

public enum Permission {

  SCHEMATIC_ITEM("schematicbooks"),

  SAVE(SCHEMATIC_ITEM, "save"),
  GET(SCHEMATIC_ITEM, "get"),
  SHOW(SCHEMATIC_ITEM, "show"),

  INFO(SCHEMATIC_ITEM, "info"),
  INFO_OWNER(INFO, "owner"),
  INFO_PERMISSION(INFO, "permission"),
  INFO_GETTER(INFO, "getter"),

  PICKUP(SCHEMATIC_ITEM, "pickup"),

  PLACE(SCHEMATIC_ITEM, "place"),;

  private final String permission;

  private static <T> T notNull(T obj) {
    Preconditions.checkNotNull(obj);
    return obj;
  }

  private Permission(Permission parent, String permission) {
    this(notNull(parent) + "." + notNull(permission));
  }

  private Permission(String permission) {
    Preconditions.checkArgument(permission.length() > 0);
    this.permission = permission.toLowerCase();
  }

  public String toString() {
    return permission;
  }

  public boolean checkPermission(Permissible permissible) {
    return permissible.hasPermission(permission);
  }

  public boolean checkPermission(Permissible permissible, String extension) {
    return permissible.hasPermission(permission + "." + extension.toLowerCase());
  }

}
