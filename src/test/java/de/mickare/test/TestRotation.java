package de.mickare.test;

import static org.junit.Assert.*;

import org.junit.Test;

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

}
