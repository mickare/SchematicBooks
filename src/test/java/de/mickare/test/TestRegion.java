package de.mickare.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.mickare.schematicbooks.util.IntRegion;
import de.mickare.schematicbooks.util.IntVector;

public class TestRegion {

  @Test
  public void testContainsTrue() {

    IntRegion rg = new IntRegion(new IntVector(0, 0, 0), 1, 1, 1);

    for (int x = -1; x <= 0; x++) {
      for (int y = -1; y <= 0; y++) {
        for (int z = -1; z <= 0; z++) {
          assertTrue(new IntRegion(new IntVector(x, y, z), 2, 2, 2).contains(rg));
        }
      }
    }

    for (int x = -2; x <= 0; x++) {
      for (int y = -2; y <= 0; y++) {
        for (int z = -2; z <= 0; z++) {
          assertTrue(new IntRegion(new IntVector(x, y, z), 3, 3, 3).contains(rg));
        }
      }
    }

    rg = new IntRegion(new IntVector(0, 0, 0), 1, 3, 1);
    assertTrue(rg.contains(new IntRegion(new IntVector(0, 1, 0), 1, 1, 1)));
    
  }


  @Test
  public void testContainsFalse() {

    IntRegion rg = new IntRegion(new IntVector(0, 0, 0), 3, 1, 1);

    assertFalse(new IntRegion(new IntVector(1, 0, -1), 1, 1, 3).contains(rg));

    assertFalse(new IntRegion(new IntVector(0, 1, 0), 3, 1, 1).contains(rg));
    assertFalse(new IntRegion(new IntVector(0, -1, 0), 3, 1, 1).contains(rg));

    assertFalse(new IntRegion(new IntVector(0, 0, 1), 3, 1, 1).contains(rg));
    assertFalse(new IntRegion(new IntVector(0, 0, -1), 3, 1, 1).contains(rg));

    assertFalse(new IntRegion(new IntVector(-1, 0, 0), 1, 1, 1).contains(rg));
    assertFalse(new IntRegion(new IntVector(3, 0, 0), 1, 1, 1).contains(rg));

  }

}
