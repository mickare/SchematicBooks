package de.mickare.schematicbooks.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Rotation {
  NORTH(180),
  EAST(270),
  SOUTH(0),
  WEST(90);

  private final @Getter int yaw;

  public static Rotation fromYaw(float yaw) {
    int temp = ((int) Math.round((((yaw % 360) + 360) % 360) / 90) * 90) % 360;
    for (Rotation rot : Rotation.values()) {
      if (rot.yaw == temp) {
        return rot;
      }
    }
    throw new RuntimeException("Should not happen! " + temp);
  }

  public static Rotation fromYaw(int yaw) {
    return fromYaw((float) yaw);
  }

}
