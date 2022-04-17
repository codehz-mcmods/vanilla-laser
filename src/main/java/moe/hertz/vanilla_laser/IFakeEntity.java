package moe.hertz.vanilla_laser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import moe.hertz.vanilla_laser.mixins.EntityAttributesS2CPacketMixin;
import moe.hertz.vanilla_laser.mixins.EntityMixin;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.math.Vec3d;

public interface IFakeEntity {
  public int getId();

  public UUID getUuid();

  public EntityType<?> getFakeType();

  public static int generateId() {
    return EntityMixin.getCurrentId().incrementAndGet();
  }

  default public Packet<?> getSpawnPacket(Vec3d position) {
    return getSpawnPacket(position, 0f, 0f, 0);
  }

  default public Packet<?> getSpawnPacket(Vec3d position, int data) {
    return getSpawnPacket(position, 0f, 0f, data);
  }

  default public Packet<?> getSpawnPacket(Vec3d position, float pitch, float yaw) {
    return getSpawnPacket(position, pitch, yaw, 0);
  }

  default public Packet<?> getSpawnPacket(Vec3d position, float pitch, float yaw, int data) {
    return new EntitySpawnS2CPacket(
        getId(),
        getUuid(),
        position.x,
        position.y,
        position.z,
        pitch,
        yaw,
        getFakeType(),
        data,
        Vec3d.ZERO);
  }

  default public Packet<?> getDespawnPacket() {
    return new EntitiesDestroyS2CPacket(getId());
  }

  default public Packet<?> getMovePacket(Vec3d position, double yaw, double pitch, boolean onGround) {
    PacketByteBuf byteBuf = PacketByteBufs.create();
    byteBuf.writeVarInt(getId());
    byteBuf.writeDouble(position.x);
    byteBuf.writeDouble(position.y);
    byteBuf.writeDouble(position.z);
    byteBuf.writeByte((byte) (yaw * 256.0f / 360.0f));
    byteBuf.writeByte((byte) (pitch * 256.0f / 360.0f));
    byteBuf.writeBoolean(onGround);
    return new EntityPositionS2CPacket(byteBuf);
  }

  default public Packet<?> getMovePacket(Vec3d position) {
    PacketByteBuf byteBuf = PacketByteBufs.create();
    byteBuf.writeVarInt(getId());
    byteBuf.writeDouble(position.x);
    byteBuf.writeDouble(position.y);
    byteBuf.writeDouble(position.z);
    byteBuf.writeByte(0);
    byteBuf.writeByte(0);
    byteBuf.writeBoolean(false);
    return new EntityPositionS2CPacket(byteBuf);
  }

  default public Packet<?> getStatusPacket(byte status) {
    PacketByteBuf byteBuf = PacketByteBufs.create();
    byteBuf.writeInt(getId());
    byteBuf.writeByte(status);
    return new EntityStatusS2CPacket(byteBuf);
  }

  default public AttributeUpdate updateAttribute() {
    return new AttributeUpdate(getId());
  }

  public class AttributeUpdate {
    private final EntityAttributesS2CPacket packet;
    private final List<EntityAttributesS2CPacket.Entry> entries;

    private AttributeUpdate(int id) {
      packet = new EntityAttributesS2CPacket(id, Collections.emptyList());
      entries = ((EntityAttributesS2CPacketMixin) packet).getEntries();
    }

    public AttributeUpdate add(EntityAttribute attribute, double baseValue, EntityAttributeModifier... modifiers) {
      entries.add(new EntityAttributesS2CPacket.Entry(attribute, baseValue, Arrays.asList(modifiers)));
      return this;
    }

    public Packet<?> build() {
      return packet;
    }
  }

  default public DataUpdate updateData() {
    return new DataUpdate(getId());
  }

  public class DataUpdate {
    private final int id;
    private final List<DataTracker.Entry<?>> entries = new ArrayList<DataTracker.Entry<?>>();

    private DataUpdate(int id) {
      this.id = id;
    }

    public <T> DataUpdate add(TrackedData<T> key, T value) {
      entries.add(new DataTracker.Entry<T>(key, value));
      return this;
    }

    public DataUpdate flag(EntityFlag... flags) {
      return add(EntityMixin.getFlagTracker(), EntityFlag.of(flags));
    }

    public DataUpdate noGravity(boolean value) {
      return add(EntityMixin.getNoGravityTracker(), value);
    }

    public DataUpdate silent(boolean value) {
      return add(EntityMixin.getSilentTracker(), value);
    }

    public DataUpdate pose(EntityPose value) {
      return add(EntityMixin.getEntityPoseTracker(), value);
    }

    public Packet<?> build() {
      return new EntityTrackerUpdateS2CPacket(id,
          new DataTracker(null) {
            @Override
            public List<Entry<?>> getDirtyEntries() {
              return entries;
            }
          },
          false);
    }
  }
}
