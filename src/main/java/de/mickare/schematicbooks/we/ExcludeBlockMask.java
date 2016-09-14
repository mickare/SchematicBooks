package de.mickare.schematicbooks.we;

import java.util.Collection;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.BlockMask;

public class ExcludeBlockMask extends BlockMask {

  public ExcludeBlockMask(Extent extent, Collection<BaseBlock> blocks) {
    super(extent, blocks);
  }

  public ExcludeBlockMask(Extent extent, BaseBlock[] block) {
    super(extent, block);
  }

  @Override
  public boolean test(Vector vector) {
    return !super.test(vector);
  }

}
