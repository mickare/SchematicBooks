package de.mickare.schematicbooks;

import java.util.List;

import org.bukkit.permissions.Permissible;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import de.mickare.schematicbooks.util.IntVectorAxis;
import de.mickare.schematicbooks.util.Rotation;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
public class SchematicBookInfo {

  private static String checkStr(String str) throws IllegalArgumentException {
    Preconditions.checkArgument(str.length() > 0);
    return str;
  }

  public static String makeKey(String name) {
    return name.replaceAll("\\s", "_").toLowerCase();
  }

  private final String name;
  private final String creator;
  private final Rotation rotation;
  private final List<String> description;
  private final String permission;

  private boolean movable = false;
  private boolean rotatable = false;

  // In World coordinates
  private @Getter @Setter @NonNull IntVectorAxis hitBoxOffset = new IntVectorAxis();

  public SchematicBookInfo(String name, String creator, //
      Rotation rotation, //
      List<String> description, String permission, //
      boolean movable, boolean rotatable) {
    Preconditions.checkNotNull(rotation);
    Preconditions.checkNotNull(description);
    this.name = checkStr(name);
    this.creator = checkStr(creator);
    this.rotation = rotation;
    this.description = Lists.newArrayList(description);
    this.permission = permission;

    this.movable = movable;
    this.rotatable = rotatable;
  }

  public String getKey() {
    return makeKey(name);
  }

  /*
   * public Path getSchematicFilePath() { return getSchematicFilePath(
   * JavaPlugin.getPlugin(SchematicBooksPlugin.class).getSchematicFolder()); }
   * 
   * public Path getSchematicFilePath(Path schematicFolder) { return
   * schematicFolder.resolve(this.getKey() + ".schematic"); }
   */

  public boolean hasPermission() {
    return permission != null && permission.length() > 0;
  }

  public boolean checkPermission(Permissible permissible) {
    if (hasPermission()) {
      return permissible.hasPermission(permission);
    }
    return true;
  }

}
