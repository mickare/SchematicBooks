package de.mickare.schematicbooks.we;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.bukkit.entity.Ambient;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Boat;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Golem;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Villager;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.entity.metadata.EntityType;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.util.Enums;

import lombok.Getter;

public class EntityEditSession extends EditSession {

  private @Getter final Set<GetterBukkitEntityType> createdEntities = Sets.newHashSet();

  @SuppressWarnings("deprecation")
  public EntityEditSession(LocalWorld world, int maxBlocks) {
    super(world, maxBlocks);
  }

  @SuppressWarnings("deprecation")
  public EntityEditSession(LocalWorld world, int maxBlocks, BlockBag blockBag) {
    super(world, maxBlocks, blockBag);
  }

  public Set<UUID> getCreatedEntityUUIDs() {
    return this.createdEntities.stream().map(GetterBukkitEntityType::getUniqueId)
        .collect(Collectors.toSet());
  }

  @Override
  @Nullable
  public Entity createEntity(com.sk89q.worldedit.util.Location location, BaseEntity entity) {
    Entity created = super.createEntity(location, entity);
    if (created != null) {
      GetterBukkitEntityType temp = created.getFacet(GetterBukkitEntityType.class);
      this.createdEntities.add(temp);
    }
    return created;
  }

  public static class GetterBukkitEntityType implements EntityType {

    private static final org.bukkit.entity.EntityType tntMinecartType =
        Enums.findByValue(org.bukkit.entity.EntityType.class, "MINECART_TNT");

    private @Getter final org.bukkit.entity.Entity entity;

    public GetterBukkitEntityType(org.bukkit.entity.Entity entity) {
      Preconditions.checkNotNull(entity);
      this.entity = entity;
    }

    public UUID getUniqueId() {
      return entity.getUniqueId();
    }

    @Override
    public boolean isPlayerDerived() {
      return entity instanceof HumanEntity;
    }

    @Override
    public boolean isProjectile() {
      return entity instanceof Projectile;
    }

    @Override
    public boolean isItem() {
      return entity instanceof Item;
    }

    @Override
    public boolean isFallingBlock() {
      return entity instanceof FallingBlock;
    }

    @Override
    public boolean isPainting() {
      return entity instanceof Painting;
    }

    @Override
    public boolean isItemFrame() {
      return entity instanceof ItemFrame;
    }

    @Override
    public boolean isBoat() {
      return entity instanceof Boat;
    }

    @Override
    public boolean isMinecart() {
      return entity instanceof Minecart;
    }

    @Override
    public boolean isTNT() {
      return entity instanceof TNTPrimed || entity.getType() == tntMinecartType;
    }

    @Override
    public boolean isExperienceOrb() {
      return entity instanceof ExperienceOrb;
    }

    @Override
    public boolean isLiving() {
      return entity instanceof LivingEntity;
    }

    @Override
    public boolean isAnimal() {
      return entity instanceof Animals;
    }

    @Override
    public boolean isAmbient() {
      return entity instanceof Ambient;
    }

    @Override
    public boolean isNPC() {
      return entity instanceof Villager;
    }

    @Override
    public boolean isGolem() {
      return entity instanceof Golem;
    }

    @Override
    public boolean isTamed() {
      return entity instanceof Tameable && ((Tameable) entity).isTamed();
    }

    @Override
    public boolean isTagged() {
      return entity instanceof LivingEntity && ((LivingEntity) entity).getCustomName() != null;
    }

  }

}
