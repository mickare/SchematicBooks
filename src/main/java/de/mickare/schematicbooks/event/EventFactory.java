package de.mickare.schematicbooks.event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

import de.mickare.schematicbooks.SchematicBookInfo;
import de.mickare.schematicbooks.data.SchematicEntity;
import de.mickare.schematicbooks.util.Rotation;

public class EventFactory {

  private static <E extends Event> E call(E event) {
    Bukkit.getPluginManager().callEvent(event);
    return event;
  }

  public static InfoSchematicEvent callInfoEvent(Player player, World world, Cancellable origin,
      SchematicEntity entity) {
    return call(new InfoSchematicEvent(player, world, origin, entity));
  }


  public static PickupSchematicEvent callPickupEvent(Player player, World world, Cancellable origin,
      SchematicEntity entity) {
    return call(new PickupSchematicEvent(player, world, origin, entity));
  }

  public static PlaceSchematicEvent callPlaceEvent(Player player, Location to, Cancellable origin,
      SchematicBookInfo info, Rotation rotation) {
    return call(new PlaceSchematicEvent(player, to, origin, info, rotation));
  }

}
