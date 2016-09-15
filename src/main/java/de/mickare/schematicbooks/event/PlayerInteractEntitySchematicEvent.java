package de.mickare.schematicbooks.event;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import com.google.common.base.Preconditions;

import de.mickare.schematicbooks.data.SchematicEntity;
import lombok.Getter;

public class PlayerInteractEntitySchematicEvent extends PlayerSchematicEvent {

  private @Getter static final HandlerList handlerList = new HandlerList();

  private @Getter final SchematicEntity entity;

  public PlayerInteractEntitySchematicEvent(Player player, World world, Cancellable origin,
      SchematicEntity entity) {
    super(player, world, origin);
    Preconditions.checkNotNull(entity);
    this.entity = entity;
  }


  @Override
  public HandlerList getHandlers() {
    return handlerList;
  }

}
