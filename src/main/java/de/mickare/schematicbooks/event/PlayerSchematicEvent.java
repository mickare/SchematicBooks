package de.mickare.schematicbooks.event;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class PlayerSchematicEvent extends Event implements Cancellable {

  private @Getter static final HandlerList handlerList = new HandlerList();

  private @Getter @NonNull final Player player;
  private @Getter @NonNull final World world;
  private @Getter @NonNull final Cancellable origin;
  private @Getter @Setter boolean cancelled = false;


  @Override
  public HandlerList getHandlers() {
    return handlerList;
  }

}
