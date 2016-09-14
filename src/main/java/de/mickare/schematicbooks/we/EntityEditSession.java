package de.mickare.schematicbooks.we;

import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.google.common.collect.Sets;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.inventory.BlockBag;

import lombok.Getter;

public class EntityEditSession extends EditSession {

  private @Getter final Set<UUID> createdEntityUUIDs = Sets.newHashSet();

  @SuppressWarnings("deprecation")
  public EntityEditSession(LocalWorld world, int maxBlocks) {
    super(world, maxBlocks);
  }

  @SuppressWarnings("deprecation")
  public EntityEditSession(LocalWorld world, int maxBlocks, BlockBag blockBag) {
    super(world, maxBlocks, blockBag);
  }

  @Override
  @Nullable
  public Entity createEntity(com.sk89q.worldedit.util.Location location, BaseEntity entity) {
    Entity created = super.createEntity(location, entity);
    if (created != null) {
      WEUtils.getUUID(created).ifPresent(createdEntityUUIDs::add);
    }
    return created;
  }

}
