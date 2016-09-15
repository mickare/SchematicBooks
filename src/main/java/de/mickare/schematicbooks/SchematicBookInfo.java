package de.mickare.schematicbooks;

import java.nio.file.Path;
import java.util.List;

import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.java.JavaPlugin;

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


  // Pos1 = lower
  // Pos2 = higher
  private @Getter @Setter @NonNull IntVectorAxis hitBoxOffset = new IntVectorAxis();

  public SchematicBookInfo(String name, String creator, //
      Rotation rotation, //
      List<String> description, String permission) {
    Preconditions.checkNotNull(rotation);
    Preconditions.checkNotNull(description);
    this.name = checkStr(name);
    this.creator = checkStr(creator);
    this.rotation = rotation;
    this.description = Lists.newArrayList(description);
    this.permission = permission;
  }

  public String getKey() {
    return makeKey(name);
  }

  public Path getSchematicFilePath() {
    return getSchematicFilePath(
        JavaPlugin.getPlugin(SchematicBooksPlugin.class).getSchematicFolder());
  }

  public Path getSchematicFilePath(Path schematicFolder) {
    return schematicFolder.resolve(this.getKey() + ".schematic");
  }

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
