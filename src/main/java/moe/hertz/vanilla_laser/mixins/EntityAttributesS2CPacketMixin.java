package moe.hertz.vanilla_laser.mixins;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket;

@Mixin(EntityAttributesS2CPacket.class)
public interface EntityAttributesS2CPacketMixin {
  @Accessor
  List<EntityAttributesS2CPacket.Entry> getEntries();
}
