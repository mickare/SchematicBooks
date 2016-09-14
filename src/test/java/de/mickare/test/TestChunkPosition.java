package de.mickare.test;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.mickare.schematicbooks.data.ChunkPosition;
import de.mickare.schematicbooks.util.IntRegion;
import de.mickare.schematicbooks.util.IntVector;

public class TestChunkPosition {

  @Test
  public void testIntersect() {

    for (int cx = 0; cx < 10; cx++) {
      for (int cz = 0; cz < 10; cz++) {
        ChunkPosition chunk = new ChunkPosition(cx, cz);
        for (int x = 0; x < 16; x++) {
          for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {



              IntVector start = chunk.getMinPoint().add(x, y, z);

              IntRegion box = new IntRegion(start, start);

              if (!chunk.intersects(box)) {
                System.out.println(
                    chunk.toString() + " x" + x + " y" + y + " z" + z + " -> " + box.toString());
              }

              assertTrue(chunk.intersects(box));

            }
          }
        }
      }
    }

  }
}
