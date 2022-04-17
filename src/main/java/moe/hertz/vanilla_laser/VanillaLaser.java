package moe.hertz.vanilla_laser;

import me.lortseam.completeconfig.gui.ConfigScreenBuilder;
import me.lortseam.completeconfig.gui.cloth.ClothConfigScreenBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.argument.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.text.ClickEvent.Action;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;

import static net.minecraft.server.command.CommandManager.*;

import java.util.Arrays;
import java.util.Optional;

public class VanillaLaser implements ModInitializer {
  public static final EntityType<LaserShooterEntity> SHOOTER = Registry.register(
      Registry.ENTITY_TYPE,
      new Identifier("laser", "shooter"),
      FabricEntityTypeBuilder
          .<LaserShooterEntity>create(SpawnGroup.CREATURE, LaserShooterEntity::new)
          .dimensions(EntityDimensions.fixed(0f, 0f))
          .build());

  @Override
  public void onInitialize() {
    new ModConfig().load();

    if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
      ConfigScreenBuilder.setMain("vanilla-laser", new ClothConfigScreenBuilder());
    }

    if (ModConfig.isCommandEnabled()) {
      var cmdname = ModConfig.getCommandName();
      CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
        var root = literal(cmdname).requires(it -> it.hasPermissionLevel(2));
        var create = literal("create");
        create.then(argument("target-location", Vec3ArgumentType.vec3()).executes(ctx -> {
          var src = ctx.getSource();
          var source = src.getPosition();
          var target = Vec3ArgumentType.getVec3(ctx, "target-location");
          var laser = createLaser(src.getWorld(), source, target);
          src.sendFeedback(laser.getDisplayName(), false);
          return 1;
        }));
        create.then(argument("target-entity", EntityArgumentType.entity()).executes(ctx -> {
          var src = ctx.getSource();
          var source = src.getPosition();
          var target = EntityArgumentType.getEntity(ctx, "target-entity");
          var laser = createLaser(src.getWorld(), source, target);
          src.sendFeedback(laser.getDisplayName(), false);
          return 1;
        }));
        var list = literal("list").executes(ctx -> {
          var src = ctx.getSource();
          var world = src.getWorld();
          var entities = world.getEntitiesByType(TypeFilter.instanceOf(LaserShooterEntity.class), it -> true);
          src.sendFeedback(Texts.join(entities, Optional.of(Text.of("\n")), Entity::getDisplayName), false);
          return 1;
        });
        var editUuid = argument("uuid", UuidArgumentType.uuid());
        editUuid.executes(ctx -> {
          var src = ctx.getSource();
          var world = src.getWorld();
          var uuid = UuidArgumentType.getUuid(ctx, "uuid");
          var entity = world.getEntity(uuid);
          if (entity != null && entity instanceof LaserShooterEntity shooter) {
            var tp = Text.of("teleport").copy().styled(style -> style
                .withUnderline(true)
                .withClickEvent(
                    new ClickEvent(Action.SUGGEST_COMMAND, "/tp " + uuid.toString())));
            var retarget = Text.of("set target").copy().styled(style -> style
                .withUnderline(true)
                .withClickEvent(
                    new ClickEvent(Action.SUGGEST_COMMAND,
                        String.format("/%s edit %s set", cmdname, uuid))));
            var kill = Text.of("kill").copy().styled(style -> style
                .withUnderline(true)
                .withClickEvent(
                    new ClickEvent(Action.RUN_COMMAND, "/kill " + uuid.toString())));
            src.sendFeedback(Texts.join(Arrays.asList(tp, retarget, kill), Text.of("  ")), false);
            return 1;
          } else {
            src.sendError(Text.of("invalid uuid"));
            return 0;
          }
        });
        var editSet = literal("set").executes(ctx -> {
          var src = ctx.getSource();
          var world = src.getWorld();
          var uuid = UuidArgumentType.getUuid(ctx, "uuid");
          var entity = world.getEntity(uuid);
          if (entity != null && entity instanceof LaserShooterEntity shooter) {
            shooter.setTargetPosition(src.getPosition());
            shooter.setTargetEntity(src.getPlayer());
          } else {
            src.sendError(Text.of("invalid uuid"));
            return 0;
          }
          return 1;
        }).then(argument("new-location", Vec3ArgumentType.vec3()).executes(ctx -> {
          var src = ctx.getSource();
          var world = src.getWorld();
          var uuid = UuidArgumentType.getUuid(ctx, "uuid");
          var target = Vec3ArgumentType.getVec3(ctx, "new-location");
          var entity = world.getEntity(uuid);
          if (entity != null && entity instanceof LaserShooterEntity shooter) {
            shooter.setTargetPosition(target);
            shooter.setTargetEntity(null);
          } else {
            src.sendError(Text.of("invalid uuid"));
            return 0;
          }
          return 1;
        })).then(argument("new-entity", EntityArgumentType.entity()).executes(ctx -> {
          var src = ctx.getSource();
          var world = src.getWorld();
          var uuid = UuidArgumentType.getUuid(ctx, "uuid");
          var target = EntityArgumentType.getEntity(ctx, "new-entity");
          var entity = world.getEntity(uuid);
          if (entity != null && entity instanceof LaserShooterEntity shooter) {
            shooter.setTargetEntity(target);
          } else {
            src.sendError(Text.of("invalid uuid"));
            return 0;
          }
          return 1;
        }));
        editUuid.then(editSet);
        var edit = literal("edit").then(editUuid);
        dispatcher.register(root.then(create).then(list).then(edit));
      });
    }
  }

  public static LaserShooterEntity createLaser(ServerWorld world, Vec3d source, Vec3d target) {
    var entity = new LaserShooterEntity(world, source);
    entity.setTargetPosition(target);
    world.spawnEntity(entity);
    return entity;
  }

  public static LaserShooterEntity createLaser(ServerWorld world, Vec3d source, Entity target) {
    var entity = new LaserShooterEntity(world, source);
    entity.setTargetEntity(target);
    world.spawnEntity(entity);
    return entity;
  }
}
