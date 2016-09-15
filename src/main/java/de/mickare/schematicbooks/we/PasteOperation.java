package de.mickare.schematicbooks.we;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;
import java.util.function.Function;

import org.bukkit.Location;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.MaskIntersection;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.registry.WorldData;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public class PasteOperation {


  private @Getter final Clipboard clipboard;
  private @Getter final WorldData worldData;
  private @Getter final Transform transform;
  private @Getter final Extent targetExtent;
  private @Getter final WorldData targetWorldData;

  private @Getter @Setter @NonNull Vector to = null;
  private @Getter @Setter boolean ignoreAirBlocks;

  private final Set<Function<PasteOperation, Mask>> sourceMasks = Sets.newHashSet();

  /**
   * Create a new instance.
   *
   * @param holder the clipboard holder
   * @param targetExtent an extent
   * @param targetWorldData world data of the target
   */
  public PasteOperation(ClipboardHolder holder, Extent targetExtent, WorldData targetWorldData) {
    checkNotNull(holder);
    checkNotNull(targetExtent);
    checkNotNull(targetWorldData);
    this.clipboard = holder.getClipboard();
    this.worldData = holder.getWorldData();
    this.transform = holder.getTransform();
    this.targetExtent = targetExtent;
    this.targetWorldData = targetWorldData;
  }

  public PasteOperation location(Location to) {
    this.to = new Vector(to.getBlockX(), to.getBlockY(), to.getBlockZ());
    return this;
  }

  public PasteOperation addSourceMask(Function<PasteOperation, Mask> maskFactory) {
    this.sourceMasks.add(maskFactory);
    return this;
  }


  /**
   * Build the operation.
   *
   * @return the operation
   */
  public Operation build() {
    BlockTransformExtent extent =
        new BlockTransformExtent(clipboard, transform, targetWorldData.getBlockRegistry());
    ForwardExtentCopy copy = new ForwardExtentCopy(extent, clipboard.getRegion(),
        clipboard.getOrigin(), targetExtent, to);
    copy.setTransform(transform);

    if (sourceMasks.isEmpty()) {
      if (ignoreAirBlocks) {
        copy.setSourceMask(new ExistingBlockMask(clipboard));
      }
    } else {
      MaskIntersection intersect = new MaskIntersection();
      intersect.add(new ExistingBlockMask(clipboard));
      this.sourceMasks.forEach(f -> {
        Mask m = f.apply(PasteOperation.this);
        Preconditions.checkNotNull(m);
        intersect.add(m);
      });
    }

    return copy;
  }

  public void buildAndPaste() throws MaxChangedBlocksException {
    Operations.completeLegacy(build());
  }

}
