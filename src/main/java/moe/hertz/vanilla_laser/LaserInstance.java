package moe.hertz.vanilla_laser;

import moe.hertz.vanilla_laser.mixins.GuardianEntityMixin;
import net.minecraft.entity.EntityType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public class LaserInstance {
  static class DummyEntity extends FakeEntity {
    DummyEntity(Vec3d position) {
      super(EntityType.BAT, position);
    }

    void setupForPlayer(ServerPlayerEntity player) {
      player.networkHandler.sendPacket(getSpawnPacket());
      player.networkHandler.sendPacket(
          new DataUpdate()
              .flag(EntityFlag.INVISIBLE)
              .noGravity(true)
              .silent(true)
              .build());
    }
  }

  static class FakeGuardian extends FakeEntity {
    int target;

    FakeGuardian(Vec3d position, int target) {
      super(EntityType.GUARDIAN, position);
      this.target = target;
    }

    void setupForPlayer(ServerPlayerEntity player) {
      player.networkHandler.sendPacket(getSpawnPacket());
      player.networkHandler.sendPacket(
          new DataUpdate()
              .flag(EntityFlag.INVISIBLE)
              .noGravity(true)
              .silent(true)
              .add(GuardianEntityMixin.getTargetTracker(), target)
              .build());
    }
  }

  FakeGuardian start;
  DummyEntity end;

  public LaserInstance(Vec3d start, Vec3d end) {
    this.end = new DummyEntity(end);
    this.start = new FakeGuardian(start, this.end.id);
  }

  public void spawn(ServerWorld world) {
    for (var player : world.getPlayers()) {
      start.setupForPlayer(player);
      end.setupForPlayer(player);
    }
  }

  public void despawn(ServerWorld world) {
    for (var player : world.getPlayers()) {
      player.networkHandler.sendPacket(start.getDespawnPacket());
      player.networkHandler.sendPacket(end.getDespawnPacket());
    }
  }
}
