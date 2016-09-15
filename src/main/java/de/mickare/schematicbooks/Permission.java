package de.mickare.schematicbooks;

import org.bukkit.permissions.Permissible;

import com.google.common.base.Preconditions;

public enum Permission {

  SCHEMATIC_ITEM("schematicbooks"),

  LIST(SCHEMATIC_ITEM, "list"),
  LIST_ALL(LIST, "all"),
  RELOAD(SCHEMATIC_ITEM, "reload"),
  SAVE(SCHEMATIC_ITEM, "save"),
  GET(SCHEMATIC_ITEM, "get"),
  SHOW(SCHEMATIC_ITEM, "show"),

  INFO(SCHEMATIC_ITEM, "info"),
  INFO_OWNER(INFO, "owner"),
  INFO_UUID(INFO, "uuid"),
  INFO_PERMISSION(INFO, "permission"),
  INFO_GETTER(INFO, "getter"),

  PICKUP(SCHEMATIC_ITEM, "pickup"),

  PLACE(SCHEMATIC_ITEM, "place"),
  PLACE_PLAIN(PLACE, "plain"),
  PLACE_PLAIN_UNMASK(PLACE_PLAIN, "unmask"),

  OFFSET(SCHEMATIC_ITEM, "offset");

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
