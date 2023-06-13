package io.github.thegatesdev.actionable.factory.action;

import io.github.thegatesdev.actionable.factory.ActionFactory;
import io.github.thegatesdev.actionable.util.RaycastType;
import io.github.thegatesdev.actionable.util.twin.MutableTwin;
import io.github.thegatesdev.actionable.util.twin.Twin;
import io.github.thegatesdev.maple.data.DataPrimitive;
import io.github.thegatesdev.mapletree.data.Readable;
import io.github.thegatesdev.mapletree.data.ReadableOptions;
import io.github.thegatesdev.mapletree.registry.StaticFactoryRegistry;
import io.github.thegatesdev.threshold.Threshold;
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

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static io.github.thegatesdev.actionable.Actionable.*;
import static io.github.thegatesdev.actionable.Factories.*;

public final class EntityActions extends StaticFactoryRegistry<Consumer<Entity>, ActionFactory<Entity>> {
    public EntityActions(String id) {
        super(id, ActionFactory::id);
        info().description("An action executed on a single entity.");
    }

    @Override
    public void registerStatic() {
        register(ActionFactory.multipleFactory(ENTITY_ACTION));
        register(ActionFactory.ifElseFactory(ENTITY_CONDITION, ENTITY_ACTION));
        register(ActionFactory.loopFactory(ENTITY_ACTION));
        register(ActionFactory.loopWhileFactory(ENTITY_ACTION, ENTITY_CONDITION));

        register(new ActionFactory<>("run_here", (data, entity) -> {
            Vector offset = data.getUnsafe("offset");
            var loc = entity.getLocation();
            if (data.getBoolean("relative")) offset = loc.getDirection().multiply(offset);
            data.<Consumer<Twin<Entity, Location>>>getUnsafe("action").accept(Twin.of(entity, loc.add(offset)));
        }, new ReadableOptions()
                .add("offset", VECTOR, new Vector(0, 0, 0))
                .add("action", ENTITY_LOCATION_ACTION)
                .add("relative", Readable.bool(), false)
        ));

        register(new ActionFactory<>("run_in_world", (data, entity) -> data.<Consumer<World>>getUnsafe("action").accept(entity.getLocation().getWorld()), new ReadableOptions()
                .add("action", WORLD_ACTION)
        ));

        register(new ActionFactory<>("send_message", (data, entity) -> entity.sendMessage(data.<Component>getUnsafe("message")), new ReadableOptions()
                .add("message", COLORED_STRING)
        ));

        register(new ActionFactory<>("run_command", (data, entity) -> {
            if (entity instanceof Player player) player.performCommand(data.getString("command"));
        }, new ReadableOptions()
                .add("command", Readable.string())
        ));

        register(new ActionFactory<>("swing_hand", (data, entity) -> {
            if (entity instanceof LivingEntity livingEntity) {
                EquipmentSlot slot = data.getUnsafe("hand");
                if (slot == EquipmentSlot.HAND) livingEntity.swingMainHand();
                else if (slot == EquipmentSlot.OFF_HAND) livingEntity.swingOffHand();
            }
        }, new ReadableOptions()
                .add("hand", Readable.enumeration(EquipmentSlot.class))
        ));

        register(new ActionFactory<>("drop_slot", (data, entity) -> {
            if (entity instanceof HumanEntity humanEntity) {
                final int slot = data.getInt("slot");
                final ItemStack stack = humanEntity.getInventory().getItem(slot);
                if (stack == null) return;
                final Consumer<Twin<Entity, Entity>> droppedItemAction = data.getUnsafe("drop_action", null);
                humanEntity.getInventory().clear(slot);
                final Item item = humanEntity.getWorld().dropItemNaturally(humanEntity.getLocation(), stack);
                if (droppedItemAction != null) droppedItemAction.accept(Twin.of(entity, item));
            }
        }, new ReadableOptions()
                .add("slot", Readable.integer())
                .add("drop_action", ENTITY_ENTITY_ACTION, null)
        ));

        register(new ActionFactory<>("velocity", (data, entity) -> {
            Vector dir = data.getUnsafe("direction");
            if (data.getBoolean("relative")) dir = entity.getLocation().getDirection().multiply(dir);
            if (data.getBoolean("set")) entity.setVelocity(dir);
            else entity.setVelocity(entity.getVelocity().add(dir));
        }, new ReadableOptions()
                .add("direction", VECTOR)
                .add("set", Readable.bool(), true)
                .add("relative", Readable.bool(), false)
        ));

        register(new ActionFactory<>("run_in_area", (data, entity) -> {
            final Location location = entity.getLocation();
            final MutableTwin<Entity, Entity> twinCache = new MutableTwin<>(entity, null);
            final List<Entity> nearbyEntities;
            {
                final Vector range = data.getUnsafe("range");
                final Predicate<Twin<Entity, Entity>> entityPredicate = data.getUnsafe("_pred");
                nearbyEntities = new ArrayList<>(entity.getWorld().getNearbyEntities(location, range.getX(), range.getY(), range.getZ(), entity1 -> entityPredicate.test(twinCache.setTarget(entity1))));
            }
            if (nearbyEntities.isEmpty()) return;
            nearbyEntities.sort(Comparator.comparingDouble(o -> location.distanceSquared(o.getLocation())));

            final double maxEntities = data.getDouble("max_entities");
            final Consumer<Twin<Entity, Entity>> hitAction = data.getUnsafe("action");
            int i = 0;
            for (Entity nearbyEntity : nearbyEntities) {
                hitAction.accept(twinCache.setTarget(nearbyEntity));
                if (++i > maxEntities) return;
            }
        }, new ReadableOptions()
                .add("range", VECTOR, new Vector(10, 10, 10))
                .add("include_self", Readable.bool(), false)
                .add("max_entities", Readable.integer(), 10)
                .add("condition", ENTITY_ENTITY_CONDITION, null)
                .add("action", ENTITY_ENTITY_ACTION)
                .after("_pred", data -> {
                    Predicate<Twin<Entity, Entity>> out = twin -> twin.actor().isValid() && twin.target().isValid();
                    if (!data.getBoolean("include_self")) out = out.and(twin -> !twin.areEqual());
                    final Predicate<Twin<Entity, Entity>> entityCondition = data.getUnsafe("condition", null);
                    if (entityCondition != null) out = out.and(entityCondition);
                    return new DataPrimitive(out);
                })
        ));

        register(new ActionFactory<>("teleport", (data, entity) -> {
            final Vector where = data.getUnsafe("where");
            if (data.getBoolean("relative")) entity.teleport(entity.getLocation().add(where));
            else entity.teleport(where.toLocation(entity.getWorld()));
        }, new ReadableOptions()
                .add("where", VECTOR)
                .add("relative", Readable.bool(), false)
        ));

        register(new ActionFactory<>("set_fire", (data, entity) -> {
            final int fireTicks = data.getInt("ticks");
            if (data.getBoolean("force") || entity.getFireTicks() < fireTicks) entity.setFireTicks(fireTicks);
        }, new ReadableOptions()
                .add("ticks", Readable.integer(), 1000)
                .add("force", Readable.bool(), false)
        ));

        register(new ActionFactory<>("raycast", (data, entity) -> {
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
                case ENTITY -> world.rayTraceEntities(location, location.getDirection(), maxDistance, data.getDouble("ray_size"), entityPredicate);
                case BLOCK -> world.rayTraceBlocks(location, location.getDirection(), maxDistance, FluidCollisionMode.SOURCE_ONLY, false);
                case BOTH -> world.rayTrace(location, location.getDirection(), maxDistance, FluidCollisionMode.SOURCE_ONLY, false, data.getDouble("ray_size"), entityPredicate);
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
                        relativeHitAction.accept(Twin.of(entity, Objects.requireNonNull(rayResult.getHitBlock()).getLocation().add(hitFace.getDirection())));
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
        }, new ReadableOptions()
                .add(Readable.number(), Map.of("max_distance", 100, "step_distance", 1, "ray_size", 1))
                .add("offset", VECTOR, null)
                .add("ray_action", ENTITY_LOCATION_ACTION, null)
                .add("hit_action", ENTITY_LOCATION_ACTION, null)
                .add("relative_hit_action", ENTITY_LOCATION_ACTION, null)
                .add("hit_entity_action", ENTITY_ENTITY_ACTION, null)
                .add("hit_entity_condition", ENTITY_ENTITY_CONDITION, null)
                .add("cast_type", Readable.enumeration(RaycastType.class), RaycastType.BOTH)
        ));

        register(new ActionFactory<>("damage", (data, entity) -> {
            if (entity instanceof LivingEntity livingEntity)
                livingEntity.damage(data.getDouble("amount"), entity.getUniqueId() == livingEntity.getUniqueId() ? null : entity);
        }, new ReadableOptions()
                .add("amount", Readable.number(), 1)
        ));

        register(new ActionFactory<>("effect", (data, entity) -> {
            final PotionEffectType type = data.getUnsafe("effect");
            if (entity instanceof LivingEntity livingEntity) {
                if (data.getBoolean("remove"))
                    livingEntity.removePotionEffect(type);
                else
                    livingEntity.addPotionEffect(new PotionEffect(type, data.getInt("duration"), data.getInt("amplifier"), data.getBoolean("is_ambient"), data.getBoolean("show_particles"), data.getBoolean("show_icon")));
            }
        }, new ReadableOptions()
                .add("effect", EFFECT_TYPE)
                .add("remove", Readable.bool(), false)
                .add(Readable.integer(), Map.of("duration", 20, "amplifier", 0))
                .add(Readable.bool(), Map.of("remove", false, "is_ambient", false, "show_particles", true, "show_icon", true))
        ));

        register(new ActionFactory<>("dismount", (data, entity) -> {
            final Entity vehicle = entity.getVehicle();
            if (vehicle != null) {
                vehicle.removePassenger(entity);
                var action = data.<Consumer<Twin<Entity, Entity>>>getUnsafe("action", null);
                if (action != null) action.accept(Twin.of(entity, vehicle));
            }
        }, new ReadableOptions().add("action", ENTITY_ENTITY_ACTION, null)));

        register(new ActionFactory<>("run_on_vehicle", (data, entity) -> {
            final Entity vehicle = entity.getVehicle();
            if (vehicle != null)
                data.<Consumer<Twin<Entity, Entity>>>getUnsafe("action").accept(Twin.of(entity, vehicle));
        }, new ReadableOptions().add("action", ENTITY_ENTITY_ACTION)));
    }
}
