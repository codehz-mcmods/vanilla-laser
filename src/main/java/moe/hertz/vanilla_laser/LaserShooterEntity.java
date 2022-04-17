package moe.hertz.vanilla_laser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import moe.hertz.vanilla_laser.mixins.GuardianEntityMixin;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.listener.EntityGameEventHandler;

public class LaserShooterEntity extends Entity {
  private class FakeGuardian extends FakeEntity {
    FakeGuardian() {
      super(EntityType.GUARDIAN, getPos());
    }

    final Packet<?> data = new DataUpdate()
        .flag(EntityFlag.INVISIBLE)
        .noGravity(true)
        .silent(true)
        .build();

    Packet<?> updateTarget(int target) {
      return new DataUpdate()
          .add(GuardianEntityMixin.getTargetTracker(), target)
          .build();
    }
  }

  private static class DummyEntity extends FakeEntity {
    DummyEntity() {
      super(EntityType.BAT, Vec3d.ZERO);
    }

    final Packet<?> data = new DataUpdate()
        .flag(EntityFlag.INVISIBLE)
        .noGravity(true)
        .silent(true)
        .build();
  }

  private final Set<ServerPlayerEntity> trackedPlayers = new HashSet<>();
  private final FakeGuardian fake = new FakeGuardian();
  private DummyEntity dummy = null;
  private Optional<UUID> targetEntity = Optional.empty();
  private Optional<Vec3d> targetPosition = Optional.empty();
  private Entity cachedTarget;
  private int cachedTargetId = 0;
  private Packet<?>[] cachedPackets = null;

  private final EntityGameEventHandler handler = new EntityGameEventHandler(null) {
    public void onEntityRemoval(World world) {
      var despawn = fake.getDespawnPacket();
      for (var player : trackedPlayers)
        player.networkHandler.sendPacket(despawn);
      if (dummy != null) {
        var dummy_despawn = dummy.getDespawnPacket();
        for (var player : trackedPlayers)
          player.networkHandler.sendPacket(dummy_despawn);
      }
      trackedPlayers.clear();
    };

    public void onEntitySetPos(World world) {
      var move = fake.getMovePacket(getPos());
      for (var player : trackedPlayers) {
        player.networkHandler.sendPacket(move);
      }
    };
  };

  public LaserShooterEntity(EntityType<LaserShooterEntity> type, World world) {
    super(type, world);
    setInvulnerable(true);
  }

  @Override
  public EntityGameEventHandler getGameEventHandler() {
    return handler;
  }

  @Override
  protected void initDataTracker() {
  }

  @Override
  protected void readCustomDataFromNbt(NbtCompound nbt) {
    if (nbt.contains("TargetUUID", NbtElement.INT_ARRAY_TYPE)) {
      var uuid = nbt.getUuid("TargetUUID");
      targetEntity = Optional.of(uuid);
    } else
      targetEntity = Optional.empty();
    var pos = nbt.getCompound("TargetPosition");
    if (pos != null) {
      var x = pos.getDouble("x");
      var y = pos.getDouble("y");
      var z = pos.getDouble("z");
      targetPosition = Optional.of(new Vec3d(x, y, z));
    } else {
      targetPosition = Optional.empty();
    }
    updateTarget();
  }

  private void updateTarget() {
    if (targetEntity.isPresent()) {
      var targetUuid = targetEntity.get();
      if (cachedTarget == null || !cachedTarget.getUuid().equals(targetUuid)) {
        var sw = (ServerWorld) world;
        cachedTarget = sw.getEntity(targetEntity.get());
      }
      if (cachedTarget == null) {
        targetEntity = Optional.empty();
        updateTarget(0);
      } else {
        updateTarget(cachedTarget.getId());
      }
    } else if (targetPosition.isPresent()) {
      var pos = targetPosition.get();
      if (dummy == null) {
        dummy = new DummyEntity();
        broadcastPacket(dummy.getSpawnPacket(pos), dummy.data);
      } else {
        broadcastPacket(dummy.getMovePacket(pos));
      }
      updateTarget(dummy.id);
    } else {
      updateTarget(0);
    }
  }

  private void updateTarget(int id) {
    cachedTargetId = id;
    broadcastPacket(fake.updateTarget(id));
  }

  private void broadcastPacket(Packet<?>... packets) {
    for (var player : trackedPlayers)
      for (var packet : packets)
        player.networkHandler.sendPacket(packet);
  }

  @Override
  protected void writeCustomDataToNbt(NbtCompound nbt) {
    if (targetEntity.isPresent())
      nbt.putUuid("TargetUUID", targetEntity.get());
    if (targetPosition.isPresent()) {
      var data = targetPosition.get();
      var pos = new NbtCompound();
      pos.putDouble("x", data.x);
      pos.putDouble("y", data.y);
      pos.putDouble("z", data.z);
      nbt.put("TargetPosition", pos);
    }
  }

  @Override
  public Packet<?> createSpawnPacket() {
    throw new IllegalStateException("Laser shooter should never be sent");
  }

  @Override
  public void tick() {
    super.tick();
    var sw = (ServerWorld) world;
    var list = sw.getPlayers();
    if (cachedPackets == null) {
      var packets = new ArrayList<Packet<?>>(5);
      packets.add(fake.getSpawnPacket(getPos()));
      packets.add(fake.data);
      if (dummy != null) {
        packets.add(dummy.getSpawnPacket(targetPosition.get()));
        packets.add(dummy.data);
      }
      if (cachedTargetId != 0)
        packets.add(fake.updateTarget(cachedTargetId));
      cachedPackets = packets.toArray(Packet<?>[]::new);
    }
    for (var player : list) {
      if (trackedPlayers.contains(player))
        continue;
      trackedPlayers.add(player);
      for (var packet : cachedPackets) {
        player.networkHandler.sendPacket(packet);
      }
    }
    trackedPlayers.retainAll(list);
  }
}
