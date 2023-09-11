package io.github.thegatesdev.actionable.builder.action;

import io.github.thegatesdev.actionable.builder.ActionBuilder;
import io.github.thegatesdev.actionable.registry.BuilderRegistry;
import io.github.thegatesdev.maple.read.Options;
import io.github.thegatesdev.threshold.Threshold;
import io.github.thegatesdev.threshold.util.twin.MutableTwin;
import io.github.thegatesdev.threshold.util.twin.Twin;
import net.kyori.adventure.text.Component;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static io.github.thegatesdev.actionable.Actionable.*;
import static io.github.thegatesdev.actionable.registry.Builders.*;
import static io.github.thegatesdev.maple.read.Readable.*;

public final class EntityActions extends BuilderRegistry.Static<Consumer<Entity>, ActionBuilder<Entity>> {
    public EntityActions(String id) {
        super(id);
    }

    @Override
    public void registerStatic() {
        register(ActionBuilder.moreFactory(ENTITY_ACTION));
        register(ActionBuilder.ifElseFactory(ENTITY_CONDITION, ENTITY_ACTION));
        register(ActionBuilder.loopFactory(ENTITY_ACTION));
        register(ActionBuilder.loopWhileFactory(ENTITY_ACTION, ENTITY_CONDITION));

        register(new ActionBuilder<>("run_here_with", (data, entity) -> {
            Vector offset = data.getUnsafe("offset");
            var loc = entity.getLocation();
            if (data.getBoolean("relative")) offset = loc.getDirection().multiply(offset);
            data.<Consumer<Twin<Entity, Location>>>getUnsafe("action").accept(Twin.of(entity, loc.add(offset)));
        }, new Options.Builder()
            .add("offset", VECTOR, new Vector(0, 0, 0))
            .add("action", ENTITY_LOCATION_ACTION)
            .add("relative", bool(), false)
        ));

        register(new ActionBuilder<>("run_here", (data, entity) -> {
            Vector offset = data.getUnsafe("offset");
            var loc = entity.getLocation();
            if (data.getBoolean("relative")) offset = loc.getDirection().multiply(offset);
            data.<Consumer<Location>>getUnsafe("action").accept(loc.add(offset));
        }, new Options.Builder()
            .add("offset", VECTOR, new Vector(0, 0, 0))
            .add("action", LOCATION_ACTION)
            .add("relative", bool(), false)
        ));

        register(new ActionBuilder<>("run_in_world", (data, entity) -> data.<Consumer<World>>getUnsafe("action").accept(entity.getLocation().getWorld()), new Options.Builder()
            .add("action", WORLD_ACTION)
        ));

        register(new ActionBuilder<>("send_message", (data, entity) -> entity.sendMessage(data.<Component>getUnsafe("message")), new Options.Builder()
            .add("message", COLORED_STRING)
        ));

        register(new ActionBuilder<>("run_command", (data, entity) -> {
            if (entity instanceof Player player) player.performCommand(data.getString("command"));
        }, new Options.Builder()
            .add("command", string())
        ));

        register(new ActionBuilder<>("swing_hand", (data, entity) -> {
            if (entity instanceof LivingEntity livingEntity) {
                EquipmentSlot slot = data.getUnsafe("hand");
                if (slot == EquipmentSlot.OFF_HAND) livingEntity.swingOffHand();
                else livingEntity.swingMainHand();
            }
        }, new Options.Builder()
            .add("hand", enumeration(EquipmentSlot.class))
        ));

        register(new ActionBuilder<>("drop_item", (data, entity) -> {
            if (entity instanceof HumanEntity humanEntity) {
                final int slot = data.getInt("slot");
                final ItemStack stack = humanEntity.getInventory().getItem(slot);
                if (stack == null) return;
                final Consumer<Twin<Entity, Entity>> droppedItemAction = data.getUnsafe("drop_action", null);
                humanEntity.getInventory().clear(slot);
                final Item item = humanEntity.getWorld().dropItemNaturally(humanEntity.getLocation(), stack);
                if (droppedItemAction != null) droppedItemAction.accept(Twin.of(entity, item));
            }
        }, new Options.Builder()
            .add("slot", integer())
            .optional("drop_action", ENTITY_ENTITY_ACTION)
        ));

        register(new ActionBuilder<>("velocity", (data, entity) -> {
            Vector dir = data.getUnsafe("direction");
            if (data.getBoolean("relative")) dir = entity.getLocation().getDirection().multiply(dir);
            if (data.getBoolean("set")) entity.setVelocity(dir);
            else entity.setVelocity(entity.getVelocity().add(dir));
        }, new Options.Builder()
            .add("direction", VECTOR)
            .add("set", bool(), true)
            .add("relative", bool(), false)
        ));

        register(new ActionBuilder<>("run_in_area", (data, entity) -> {
            final Location location = entity.getLocation();
            final Vector range = data.getUnsafe("range");
            final Predicate<Twin<Entity, Entity>> condition = data.getUnsafe("condition");
            final boolean includeSelf = data.getBoolean("include_self");

            final MutableTwin<Entity, Entity> twinCache = new MutableTwin<>(entity, null);
            final List<Entity> nearbyEntities = new ArrayList<>(entity.getWorld().getNearbyEntities(location, range.getX(), range.getY(), range.getZ(), e ->
                (includeSelf || e.getEntityId() != entity.getEntityId()) && condition.test(twinCache.setTarget(e))
            ));
            if (nearbyEntities.isEmpty()) return;
            nearbyEntities.sort(Comparator.comparingDouble(o -> location.distanceSquared(o.getLocation())));

            final int maxEntities = data.getInt("max_entities");
            final Consumer<Twin<Entity, Entity>> hitAction = data.getUnsafe("action");

            for (int i = 0; i < nearbyEntities.size() && i < maxEntities; i++)
                hitAction.accept(twinCache.setTarget(nearbyEntities.get(i)));
        }, new Options.Builder()
            .add("range", VECTOR, new Vector(10, 10, 10))
            .add("include_self", bool(), false)
            .add("max_entities", integer(), 10)
            .optional("condition", ENTITY_ENTITY_CONDITION)
            .add("action", ENTITY_ENTITY_ACTION)
        ));

        register(new ActionBuilder<>("teleport", (data, entity) -> {
            final Vector where = data.getUnsafe("where");
            if (data.getBoolean("relative")) entity.teleport(entity.getLocation().add(where));
            else entity.teleport(where.toLocation(entity.getWorld()));
        }, new Options.Builder()
            .add("where", VECTOR)
            .add("relative", bool(), false)
        ));

        register(new ActionBuilder<>("set_fire", (data, entity) -> {
            final int fireTicks = data.getInt("ticks");
            if (data.getBoolean("force") || entity.getFireTicks() < fireTicks) entity.setFireTicks(fireTicks);
        }, new Options.Builder()
            .add("ticks", integer(), 1000)
            .add("force", bool(), false)
        ));

        register(new ActionBuilder<>("set_freezing", (data, entity) -> {
            final int freezeTicks = data.getInt("ticks");
            if (data.getBoolean("force") || entity.getFreezeTicks() < freezeTicks) entity.setFreezeTicks(freezeTicks);
        }, new Options.Builder()
            .add("ticks", integer(), 1000)
            .add("force", bool(), false)
        ));

        register(new ActionBuilder<>("set_fall_distance", (data, entity) -> entity.setFallDistance(data.getFloat("distance")), new Options.Builder().add("distance", number())));

        enum RaycastType {
            ENTITY, BLOCK, BOTH, COSMETIC
        }

        register(new ActionBuilder<>("raycast", (data, entity) -> {
            final Consumer<Twin<Entity, Location>> rayAction = data.getUnsafe("ray_action", null);
            final Consumer<Twin<Entity, Location>> hitAction = data.getUnsafe("hit_action", null);
            final Consumer<Twin<Entity, Location>> relativeHitAction = data.getUnsafe("relative_hit_action", null);
            final Consumer<Twin<Entity, Entity>> hitEntityAction = data.getUnsafe("hit_entity_action", null);
            final Predicate<Twin<Entity, Entity>> hitEntityCondition = data.getUnsafe("hit_entity_condition", null);
            if (rayAction == null && hitAction == null && hitEntityAction == null && hitEntityCondition == null && relativeHitAction == null)
                return;
            final RaycastType rayType = data.getUnsafe("cast_type");
            final double maxDistance = data.getDouble("max_distance");
            final Vector offset = data.getUnsafe("offset", null);
            final double stepDistance = data.getDouble("step_distance");

            final Predicate<Entity> entityPredicate;
            {
                if (hitEntityCondition != null)
                    entityPredicate = e -> e != entity && !e.isDead() && hitEntityCondition.test(Twin.of(entity, e));
                else entityPredicate = e -> e != entity && !e.isDead();
            }
            final Location location = entity.getLocation();
            if (offset != null) location.add(offset);
            final World world = entity.getWorld();
            final RayTraceResult rayResult = switch (rayType) {
                case ENTITY ->
                    world.rayTraceEntities(location, location.getDirection(), maxDistance, data.getDouble("ray_size"), entityPredicate);
                case BLOCK ->
                    world.rayTraceBlocks(location, location.getDirection(), maxDistance, FluidCollisionMode.SOURCE_ONLY, false);
                case BOTH ->
                    world.rayTrace(location, location.getDirection(), maxDistance, FluidCollisionMode.SOURCE_ONLY, false, data.getDouble("ray_size"), entityPredicate);
                case COSMETIC -> null;
            };

            final Vector hitPosition;
            if (rayResult == null) {
                hitPosition = location.toVector().add(location.getDirection().multiply(maxDistance));
            } else {
                hitPosition = rayResult.getHitPosition();
                if (hitEntityAction != null) {
                    final Entity hitEntity = rayResult.getHitEntity();
                    if (hitEntity != null) hitEntityAction.accept(Twin.of(entity, hitEntity));
                }
                if (relativeHitAction != null) {
                    final BlockFace hitFace = rayResult.getHitBlockFace();
                    if (hitFace != null)
                        relativeHitAction.accept(Twin.of(entity, Objects.requireNonNull(rayResult.getHitBlock()).getLocation().add(hitFace.getDirection().multiply(data.getFloat("relative_multiplier")))));
                }
            }

            if (hitAction != null)
                hitAction.accept(Twin.of(entity, hitPosition.toLocation(world)));

            if (rayAction != null) {
                final Vector[] positions = Threshold.fromTo(location.toVector(), hitPosition, stepDistance);
                final Location l = new Location(world, 0, 0, 0);
                final var twin = new MutableTwin<Entity, Location>(entity, null);
                for (Vector position : positions)
                    rayAction.accept(twin.setTarget(l.set(position.getX(), position.getY(), position.getZ())));
            }
        }, new Options.Builder()
            .add("max_distance", number(), 100)
            .add("step_distance", number(), 1)
            .add("ray_size", number(), 1)
            .optional("offset", VECTOR)
            .optional("ray_action", ENTITY_LOCATION_ACTION)
            .optional("hit_action", ENTITY_LOCATION_ACTION)
            .optional("relative_hit_action", ENTITY_LOCATION_ACTION)
            .add("relative_multiplier", number(), 1)
            .optional("hit_entity_action", ENTITY_ENTITY_ACTION)
            .optional("hit_entity_condition", ENTITY_ENTITY_CONDITION)
            .add("cast_type", enumeration(RaycastType.class), RaycastType.BOTH)
        ));

        register(new ActionBuilder<>("damage", (data, entity) -> {
            if (entity instanceof LivingEntity livingEntity)
                livingEntity.damage(data.getDouble("amount"), entity.getUniqueId() == livingEntity.getUniqueId() ? null : entity);
        }, new Options.Builder()
            .add("amount", number(), 1)
        ));

        register(new ActionBuilder<>("effect", (data, entity) -> {
            final PotionEffectType type = data.getUnsafe("effect");
            if (entity instanceof LivingEntity livingEntity) {
                if (data.getBoolean("remove"))
                    livingEntity.removePotionEffect(type);
                else
                    livingEntity.addPotionEffect(new PotionEffect(type, data.getInt("duration"), data.getInt("amplifier"), data.getBoolean("is_ambient"), data.getBoolean("show_particles"), data.getBoolean("show_icon")));
            }
        }, new Options.Builder()
            .add("effect", EFFECT_TYPE)
            .add("remove", bool(), false)
            .add("duration", integer(), 20)
            .add("amplifier", integer(), 0)
            .add("is_ambient", bool(), false)
            .add("show_particles", bool(), true)
            .add("show_icon", bool(), true)
        ));

        register(new ActionBuilder<>("dismount", (data, entity) -> {
            final Entity vehicle = entity.getVehicle();
            if (vehicle != null) {
                vehicle.removePassenger(entity);
                var action = data.<Consumer<Twin<Entity, Entity>>>getUnsafe("action", null);
                if (action != null) action.accept(Twin.of(entity, vehicle));
            }
        }, new Options.Builder().optional("action", ENTITY_ENTITY_ACTION)));

        register(new ActionBuilder<>("run_on_vehicle", (data, entity) -> {
            final Entity vehicle = entity.getVehicle();
            if (vehicle != null)
                data.<Consumer<Twin<Entity, Entity>>>getUnsafe("action").accept(Twin.of(entity, vehicle));
        }, new Options.Builder().add("action", ENTITY_ENTITY_ACTION)));

        register(new ActionBuilder<>("rotate", (data, entity) -> {
            float pitch = data.getFloat("pitch");
            float yaw = data.getFloat("yaw");
            if (data.getBoolean("relative")) {
                pitch += entity.getPitch();
                yaw += entity.getYaw();
            }
            entity.setRotation(pitch, yaw);
        }, new Options.Builder()
            .add("pitch", number())
            .add("yaw", number())
            .add("relative", bool(), false)
        ));
    }
}
