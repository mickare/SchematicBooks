package de.mickare.schematicbooks;

import java.io.File;
import java.util.List;

import org.bukkit.entity.Player;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import de.mickare.schematicbooks.util.IntVectorTuple;
import de.mickare.schematicbooks.util.Rotation;
import lombok.Getter;

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
  private final IntVectorTuple hitBoxOffset = new IntVectorTuple();

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

  public File getSchematicFile(File schematicFolder) {
    return new File(schematicFolder, this.getKey() + ".schematic");
  }

  public boolean hasPermission(Player player) {
    if (permission != null && permission.length() > 0) {
      return player.hasPermission(permission);
    }
    return true;
  }

}
