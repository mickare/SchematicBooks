package de.mickare.schematicbooks.event;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import de.mickare.schematicbooks.data.SchematicEntity;
import lombok.Getter;

public class PickupSchematicEvent extends PlayerInteractEntitySchematicEvent implements Cancellable {

  private @Getter static final HandlerList handlerList = new HandlerList();

  public PickupSchematicEvent(Player player, World world, Cancellable origin,
      SchematicEntity entity) {
    super(player, world, origin, entity);
  }

  @Override
  public HandlerList getHandlers() {
    return handlerList;
  }

}
