package moe.hertz.vanilla_laser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import lombok.Getter;
import moe.hertz.side_effects.EntityFlag;
import moe.hertz.side_effects.IFakeEntity;
import moe.hertz.vanilla_laser.mixins.GuardianEntityMixin;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent.Action;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.listener.EntityGameEventHandler;

public class LaserShooterEntity extends Entity implements IFakeEntity {
  @Override
  public EntityType<?> getFakeType() {
    return EntityType.GUARDIAN;
  }

  Packet<?> getUpdateTarget(int target) {
    return updateData()
        .add(GuardianEntityMixin.getTargetTracker(), target)
        .build();
  }

  private static class DummyEntity implements IFakeEntity {
    @Override
    public final EntityType<?> getFakeType() {
      return EntityType.BAT;
    }

    @Getter
    public final int id = IFakeEntity.generateId();
    @Getter
    public final UUID uuid = UUID.randomUUID();

    final Packet<?> data = updateData()
        .flag(EntityFlag.INVISIBLE)
        .noGravity(true)
        .silent(true)
        .build();
  }

  private final Set<ServerPlayerEntity> trackedPlayers = new HashSet<>();
  private DummyEntity dummy = null;
  private @Nullable UUID targetEntity = null;
  @Getter
  private @Nullable Vec3d targetPosition = null;
  private Entity cachedTarget;
  private int cachedTargetId = 0;
  private Packet<?>[] cachedPackets = null;

  public void setTargetEntity(@Nullable Entity entity) {
    if (entity == null || entity == this) {
      targetEntity = null;
    } else {
      targetEntity = entity.getUuid();
      cachedTarget = entity;
    }
    updateTarget();
  }

  public void setTargetPosition(@Nullable Vec3d target) {
    targetPosition = target;
    updateTarget();
  }

  public @Nullable Entity getTargetEntity() {
    if (cachedTarget == null || !cachedTarget.getUuid().equals(targetEntity)) {
      var sw = (ServerWorld) world;
      cachedTarget = sw.getEntity(targetEntity);
    }
    return cachedTarget;
  }

  private final EntityGameEventHandler handler = new EntityGameEventHandler(null) {
    public void onEntityRemoval(World world) {
      if (dummy != null) {
        var dummy_despawn = dummy.getDespawnPacket();
        for (var player : trackedPlayers)
          player.networkHandler.sendPacket(dummy_despawn);
      }
      trackedPlayers.clear();
    };

    public void onEntitySetPos(World world) {
    };
  };

  public LaserShooterEntity(EntityType<LaserShooterEntity> type, World world) {
    super(type, world);
    setInvulnerable(true);
    setInvisible(true);
    setNoGravity(true);
    setSilent(true);
  }

  public LaserShooterEntity(World world, Vec3d position) {
    this(VanillaLaser.SHOOTER, world);
    setPosition(position);
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
      targetEntity = uuid;
    } else
      targetEntity = null;
    if (nbt.contains("TargetPosition", NbtElement.COMPOUND_TYPE)) {
      var pos = (NbtCompound) nbt.get("TargetPosition");
      var x = pos.getDouble("x");
      var y = pos.getDouble("y");
      var z = pos.getDouble("z");
      targetPosition = new Vec3d(x, y, z);
    } else {
      targetPosition = null;
    }
    updateTarget();
  }

  private void updateTarget() {
    cachedPackets = null;
    if (targetEntity != null) {
      if (cachedTarget == null || !cachedTarget.getUuid().equals(targetEntity)) {
        var sw = (ServerWorld) world;
        cachedTarget = sw.getEntity(targetEntity);
      }
      if (cachedTarget != null) {
        updateTarget(cachedTarget.getId());
        return;
      }
    }
    if (targetPosition != null) {
      if (dummy == null) {
        dummy = new DummyEntity();
        broadcastPacket(dummy.getSpawnPacket(targetPosition), dummy.data);
      } else {
        broadcastPacket(dummy.getMovePacket(targetPosition));
      }
      updateTarget(dummy.id);
    } else {
      updateTarget(0);
    }
  }

  private void updateTarget(int id) {
    cachedTargetId = id;
    broadcastPacket(getUpdateTarget(id));
  }

  private void broadcastPacket(Packet<?>... packets) {
    for (var player : trackedPlayers)
      for (var packet : packets)
        player.networkHandler.sendPacket(packet);
  }

  @Override
  protected void writeCustomDataToNbt(NbtCompound nbt) {
    if (targetEntity != null)
      nbt.putUuid("TargetUUID", targetEntity);
    if (targetPosition != null) {
      var pos = new NbtCompound();
      pos.putDouble("x", targetPosition.x);
      pos.putDouble("y", targetPosition.y);
      pos.putDouble("z", targetPosition.z);
      nbt.put("TargetPosition", pos);
    }
  }

  @Override
  public Packet<?> createSpawnPacket() {
    return getSpawnPacket(getPos());
  }

  @Override
  public void tick() {
    super.tick();
    var sw = (ServerWorld) world;
    var list = sw.getPlayers();
    if (cachedPackets == null) {
      var packets = new ArrayList<Packet<?>>(3);
      if (dummy != null) {
        packets.add(dummy.getSpawnPacket(targetPosition));
        packets.add(dummy.data);
      }
      if (cachedTargetId != 0)
        packets.add(getUpdateTarget(cachedTargetId));
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

  private static Text dumpVec3d(Vec3d vec) {
    return Text.of(String.format("(%.2f, %.2f, %.2f)", vec.x, vec.y, vec.z))
        .copy()
        .styled(style -> style
            .withInsertion(String.format("%f %f %f", vec.x, vec.y, vec.z)));
  }

  @Override
  public Text getDisplayName() {
    var txt = LiteralText.EMPTY.copy();
    txt.append(Text.of("Laser").copy()
        .styled(style -> style
            .withHoverEvent(getHoverEvent())
            .withClickEvent(new ClickEvent(Action.RUN_COMMAND, "/laser edit " + getUuidAsString()))
            .withInsertion(getUuidAsString())));
    txt.append(dumpVec3d(getPos()));
    txt.append(Text.of(" → "));
    var cached = getTargetEntity();
    if (cached != null) {
      txt.append(cached.getDisplayName());
    } else if (targetPosition != null) {
      txt.append(dumpVec3d(targetPosition));
    } else {
      txt.append(Text.of("(unknown)").copy().styled(style -> style.withStrikethrough(true)));
    }
    return txt;
  }
}
