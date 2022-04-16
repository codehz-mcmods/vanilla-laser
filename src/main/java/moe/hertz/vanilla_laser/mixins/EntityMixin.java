package moe.hertz.vanilla_laser.mixins;

import java.util.concurrent.atomic.AtomicInteger;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.data.TrackedData;

@Mixin(Entity.class)
public interface EntityMixin {
  @Accessor("CURRENT_ID")
  public static AtomicInteger getCurrentId() {
    throw new IllegalStateException();
  }

  @Accessor("FLAGS")
  public static TrackedData<Byte> getFlagTracker() {
    throw new IllegalStateException();
  }

  @Accessor("NO_GRAVITY")
  public static TrackedData<Boolean> getNoGravityTracker() {
    throw new IllegalStateException();
  }

  @Accessor("SILENT")
  public static TrackedData<Boolean> getSilentTracker() {
    throw new IllegalStateException();
  }

  @Accessor("POSE")
  public static TrackedData<EntityPose> getEntityPoseTracker() {
    throw new IllegalStateException();
  }
}
