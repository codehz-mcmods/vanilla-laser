package moe.hertz.vanilla_laser.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.mob.GuardianEntity;

@Mixin(GuardianEntity.class)
public interface GuardianEntityMixin {
  @Accessor("BEAM_TARGET_ID")
  public static TrackedData<Integer> getTargetTracker() {
    throw new IllegalStateException();
  }
}
