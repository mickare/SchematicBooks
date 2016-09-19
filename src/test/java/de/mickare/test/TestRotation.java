package de.mickare.test;

import static org.junit.Assert.*;

import org.junit.Test;

import de.mickare.schematicbooks.util.IntVector;
import de.mickare.schematicbooks.util.IntVectorAxis;
import de.mickare.schematicbooks.util.Rotation;

public class TestRotation {

  @Test
  public void testRotationForError() {
    for (float yaw = -720; yaw <= 720; yaw++) {
      Rotation.fromYaw(yaw);
    }
  }

  @Test
  public void testRotationForValue() {

    // NORTH(180),
    // EAST(270),
    // SOUTH(0),
    // WEST(90);

    assertEquals(Rotation.NORTH, Rotation.fromYaw(180));
    assertEquals(Rotation.NORTH, Rotation.fromYaw(180 + 360));
    assertEquals(Rotation.NORTH, Rotation.fromYaw(180 - 360));
    assertEquals(Rotation.EAST, Rotation.fromYaw(270));
    assertEquals(Rotation.EAST, Rotation.fromYaw(270 + 360));
    assertEquals(Rotation.EAST, Rotation.fromYaw(270 - 360));
    assertEquals(Rotation.SOUTH, Rotation.fromYaw(0));
    assertEquals(Rotation.SOUTH, Rotation.fromYaw(0 + 360));
    assertEquals(Rotation.SOUTH, Rotation.fromYaw(0 - 360));
    assertEquals(Rotation.WEST, Rotation.fromYaw(90));
    assertEquals(Rotation.WEST, Rotation.fromYaw(90 + 360));
    assertEquals(Rotation.WEST, Rotation.fromYaw(90 - 360));

  }

  @Test
  public void testRotationVectorAxis() {

    IntVectorAxis axis = new IntVectorAxis(new IntVector(1, 1, 2), new IntVector(3, 1, 4));
    IntVectorAxis result;

    result = axis.rotate(90);
    assertEquals(new IntVector(-4, 1, 1), result.getNegativeAxis());
    assertEquals( new IntVector(-2, 1, 3), result.getPositiveAxis());

    result = axis.rotate(90 - 360);
    assertEquals(new IntVector(-4, 1, 1), result.getNegativeAxis());
    assertEquals( new IntVector(-2, 1, 3), result.getPositiveAxis());
    
    
    result = axis.rotate(180);
    assertEquals(new IntVector(-3, 1, -4), result.getNegativeAxis());
    assertEquals(new IntVector(-1, 1, -2), result.getPositiveAxis());
   
    result = axis.rotate(180 - 360);
    assertEquals(new IntVector(-3, 1, -4), result.getNegativeAxis());
    assertEquals(new IntVector(-1, 1, -2), result.getPositiveAxis());
    
    
    result = axis.rotate(270);
    assertEquals(new IntVector(2, 1, -3), result.getNegativeAxis());    
    assertEquals(new IntVector(4, 1, -1), result.getPositiveAxis());

    result = axis.rotate(270 - 360);
    assertEquals(new IntVector(2, 1, -3), result.getNegativeAxis());
    assertEquals(new IntVector(4, 1, -1), result.getPositiveAxis());
    
  }


  @Test
  public void testRotationVectorAxis2() {

    IntVectorAxis axis = new IntVectorAxis(new IntVector(1, 0, 0), new IntVector(0, 0, 0));
    IntVectorAxis result;

    result = axis.rotate(90);
    assertEquals(new IntVector(0, 0, 1), result.getNegativeAxis());
    assertEquals( new IntVector(0, 0, 0), result.getPositiveAxis());

    result = axis.rotate(180);
    assertEquals(new IntVector(0, 0, 0), result.getNegativeAxis());
    assertEquals( new IntVector(-1, 0, 0), result.getPositiveAxis());

    result = axis.rotate(270);
    assertEquals(new IntVector(0, 0, 0), result.getNegativeAxis());
    assertEquals( new IntVector(0, 0, -1), result.getPositiveAxis());
  }
  
}
