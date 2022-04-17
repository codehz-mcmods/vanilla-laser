package moe.hertz.vanilla_laser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import moe.hertz.vanilla_laser.mixins.EntityAttributesS2CPacketMixin;
import moe.hertz.vanilla_laser.mixins.EntityMixin;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.util.math.Vec3d;

public class FakeEntity {
  public EntityType<?> type;
  public int id = EntityMixin.getCurrentId().incrementAndGet();
  public UUID uuid = UUID.randomUUID();
  public Vec3d position;
  public float pitch, yaw;
  public int entityData;

  public FakeEntity(EntityType<?> type, Vec3d position, float pitch, float yaw, int entityData) {
    this.type = type;
    this.position = position;
    this.pitch = pitch;
    this.yaw = yaw;
    this.entityData = entityData;
  }

  public FakeEntity(EntityType<?> type, Vec3d position) {
    this(type, position, 0f, 0f, 0);
  }

  public Packet<?> getSpawnPacket() {
    return new EntitySpawnS2CPacket(
        id,
        uuid,
        position.x,
        position.y,
        position.z,
        pitch,
        yaw,
        type,
        entityData,
        Vec3d.ZERO);
  }

  public Packet<?> getSpawnPacket(Vec3d position) {
    return new EntitySpawnS2CPacket(
        id,
        uuid,
        position.x,
        position.y,
        position.z,
        pitch,
        yaw,
        type,
        entityData,
        Vec3d.ZERO);
  }

  public Packet<?> getDespawnPacket() {
    return new EntitiesDestroyS2CPacket(id);
  }

  public Packet<?> getMovePacket(Vec3d position, double yaw, double pitch, boolean onGround) {
    PacketByteBuf byteBuf = PacketByteBufs.create();
    byteBuf.writeVarInt(id);
    byteBuf.writeDouble(position.x);
    byteBuf.writeDouble(position.y);
    byteBuf.writeDouble(position.z);
    byteBuf.writeByte((byte) (yaw * 256.0f / 360.0f));
    byteBuf.writeByte((byte) (pitch * 256.0f / 360.0f));
    byteBuf.writeBoolean(onGround);
    return new EntityPositionS2CPacket(byteBuf);
  }

  public Packet<?> getMovePacket(Vec3d position) {
    PacketByteBuf byteBuf = PacketByteBufs.create();
    byteBuf.writeVarInt(id);
    byteBuf.writeDouble(position.x);
    byteBuf.writeDouble(position.y);
    byteBuf.writeDouble(position.z);
    byteBuf.writeByte(0);
    byteBuf.writeByte(0);
    byteBuf.writeBoolean(false);
    return new EntityPositionS2CPacket(byteBuf);
  }

  public Packet<?> getStatusPacket(byte status) {
    PacketByteBuf byteBuf = PacketByteBufs.create();
    byteBuf.writeInt(id);
    byteBuf.writeByte(status);
    return new EntityStatusS2CPacket(byteBuf);
  }

  public class AttributeUpdate {
    EntityAttributesS2CPacket packet = new EntityAttributesS2CPacket(id, Collections.emptyList());
    List<EntityAttributesS2CPacket.Entry> entries = ((EntityAttributesS2CPacketMixin) packet).getEntries();

    AttributeUpdate add(EntityAttribute attribute, double baseValue, EntityAttributeModifier... modifiers) {
      entries.add(new EntityAttributesS2CPacket.Entry(attribute, baseValue, Arrays.asList(modifiers)));
      return this;
    }

    public Packet<?> build() {
      return packet;
    }
  }

  public class DataUpdate {
    private List<DataTracker.Entry<?>> entries = new ArrayList<DataTracker.Entry<?>>();

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
