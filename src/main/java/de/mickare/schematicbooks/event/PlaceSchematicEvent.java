package de.mickare.schematicbooks.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import com.google.common.base.Preconditions;

import de.mickare.schematicbooks.SchematicBookInfo;
import de.mickare.schematicbooks.util.Rotation;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class PlaceSchematicEvent extends PlayerSchematicEvent implements Cancellable {

  private @Getter static final HandlerList handlerList = new HandlerList();

  private @Getter Location to;
  private @Getter @Setter @NonNull SchematicBookInfo info;
  private @Getter @Setter @NonNull Rotation destinationRotation;

  public PlaceSchematicEvent(Player player, Location to, Cancellable origin, SchematicBookInfo info,
      Rotation rotation) {
    super(player, to.getWorld(), origin);
    this.to = to;
    this.info = info;
    this.destinationRotation = rotation;
  }

  public void setTo(Location to) {
    Preconditions.checkArgument(to.getWorld().equals(this.getWorld()));
    this.to = to;
  }

  @Override
  public HandlerList getHandlers() {
    return handlerList;
  }

}
