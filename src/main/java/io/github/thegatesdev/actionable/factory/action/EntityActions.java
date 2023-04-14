package io.github.thegatesdev.actionable.factory.action;

import io.github.thegatesdev.actionable.Actionable;
import io.github.thegatesdev.actionable.Factories;
import io.github.thegatesdev.actionable.factory.ActionFactory;
import io.github.thegatesdev.actionable.util.RaycastType;
import io.github.thegatesdev.actionable.util.twin.MutableTwin;
import io.github.thegatesdev.actionable.util.twin.Twin;
import io.github.thegatesdev.maple.data.DataPrimitive;
import io.github.thegatesdev.mapletree.data.Readable;
import io.github.thegatesdev.mapletree.data.ReadableOptions;
import io.github.thegatesdev.mapletree.registry.FactoryRegistry;
import io.github.thegatesdev.threshold.Threshold;
import org.bukkit.ChatColor;
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

public final class EntityActions extends FactoryRegistry<Consumer<Entity>, ActionFactory<Entity>> {
    public EntityActions(String id) {
        super(id, ActionFactory::id);
    }

    @Override
    public void registerStatic() {
        register(ActionFactory.multipleFactory(Factories.ENTITY_ACTION));
        register(ActionFactory.ifElseFactory(Factories.ENTITY_CONDITION, Factories.ENTITY_ACTION));
        register(ActionFactory.loopFactory(Factories.ENTITY_ACTION));
        register(ActionFactory.loopWhileFactory(Factories.ENTITY_ACTION, Factories.ENTITY_CONDITION));

        register(new ActionFactory<>("run_location_action", (data, entity) -> {
            Vector offset = data.getUnsafe("offset");
            if (data.getBoolean("relative")) offset = entity.getLocation().getDirection().multiply(offset);
            data.<Consumer<Location>>getUnsafe("action").accept(entity.getLocation().add(offset));
        }, new ReadableOptions()
                .add("offset", Actionable.VECTOR, new Vector(0, 0, 0))
                .add("action", Factories.LOCATION_ACTION)
                .add("relative", Readable.bool(), false)
        ));

        register(new ActionFactory<>("run_world_action", (data, entity) -> data.<Consumer<World>>getUnsafe("action").accept(entity.getLocation().getWorld()), new ReadableOptions()
                .add("action", Factories.WORLD_ACTION)
        ));

        register(new ActionFactory<>("run_entity_location_action", (data, entity) -> data.<Consumer<Twin<Entity, Location>>>getUnsafe("action").accept(Twin.of(entity, entity.getLocation().add(data.<Vector>getUnsafe("offset")))), new ReadableOptions()
                .add("offset", Actionable.VECTOR, new Vector(0, 0, 0))
                .add("action", Factories.ENTITY_LOCATION_ACTION)
        ));

        register(new ActionFactory<>("send_message", (data, entity) -> entity.sendMessage(ChatColor.translateAlternateColorCodes('&', data.getString("message"))), new ReadableOptions()
                .add("message", Readable.string())
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
                final Consumer<Twin<Entity, Entity>> droppedItemAction = data.getUnsafe("dropped_item_action", null);
                humanEntity.getInventory().clear(slot);
                final Item item = humanEntity.getWorld().dropItemNaturally(humanEntity.getLocation(), stack);
                if (droppedItemAction != null) droppedItemAction.accept(Twin.of(entity, item));
            }
        }, new ReadableOptions()
                .add("slot", Readable.integer())
                .add("dropped_item_action", Factories.ENTITY_ENTITY_ACTION, null)
        ));

        register(new ActionFactory<>("velocity", (data, entity) -> {
            Vector dir = data.getUnsafe("direction");
            if (data.getBoolean("relative")) dir = entity.getLocation().getDirection().multiply(dir);
            if (!data.getBoolean("add")) entity.setVelocity(dir);
            else entity.setVelocity(entity.getVelocity().add(dir));
        }, new ReadableOptions()
                .add("direction", Actionable.VECTOR)
                .add("add", Readable.bool(), false)
                .add("relative", Readable.bool(), false)
        ));

        register(new ActionFactory<>("area_entity_action", (data, entity) -> {
            final Location location = entity.getLocation();
            final MutableTwin<Entity, Entity> twinCache = new MutableTwin<>(entity, null);
            final List<Entity> nearbyEntities;
            {
                final Vector range = data.getUnsafe("range");
                final Predicate<Twin<Entity, Entity>> entityPredicate = data.getUnsafe("entity_predicate");
                nearbyEntities = new ArrayList<>(entity.getWorld().getNearbyEntities(location, range.getX(), range.getY(), range.getZ(), entity1 -> entityPredicate.test(twinCache.setTarget(entity1))));
            }
            if (nearbyEntities.isEmpty()) return;
            nearbyEntities.sort(Comparator.comparingDouble(o -> location.distanceSquared(o.getLocation())));

            final double maxEntities = data.getDouble("max_entities");
            final Consumer<Twin<Entity, Entity>> hitAction = data.getUnsafe("entity_action");
            int i = 0;
            for (Entity nearbyEntity : nearbyEntities) {
                hitAction.accept(twinCache.setTarget(nearbyEntity));
                if (++i > maxEntities) break;
            }
        }, new ReadableOptions()
                .add("range", Actionable.VECTOR, new Vector(10, 10, 10))
                .add("include_self", Readable.bool(), false)
                .add("max_entities", Readable.integer(), 10)
                .add("entity_condition", Factories.ENTITY_ENTITY_CONDITION, null)
                .add("entity_action", Factories.ENTITY_ENTITY_ACTION)
                .after("entity_predicate", data -> {
                    Predicate<Twin<Entity, Entity>> out = twin -> twin.actor().isValid() && twin.target().isValid();
                    if (!data.getBoolean("include_self")) out = out.and(twin -> !twin.areEqual());
                    final Predicate<Twin<Entity, Entity>> entityCondition = data.getUnsafe("entity_condition", null);
                    if (entityCondition != null) out = out.and(entityCondition);
                    return new DataPrimitive(out);
                })
        ));

        register(new ActionFactory<>("teleport", (data, entity) -> {
            final Vector where = data.getUnsafe("where");
            if (data.getBoolean("relative")) entity.teleport(entity.getLocation().add(where));
            else entity.teleport(where.toLocation(entity.getWorld()));
        }, new ReadableOptions()
                .add("relative", Readable.bool(), false)
                .add("where", Actionable.VECTOR)
        ));

        register(new ActionFactory<>("set_on_fire", (data, entity) -> {
            final int fireTicks = data.getInt("ticks");
            if (data.getBoolean("force") || entity.getFireTicks() < fireTicks) entity.setFireTicks(fireTicks);
        }, new ReadableOptions()
                .add("ticks", Readable.integer(), 1000)
                .add("force", Readable.bool(), false)
        ));

        register(new ActionFactory<>("raycast", (data, entity) -> {
            final Consumer<Location> rayAction = data.getUnsafe("ray_action", null);
            final Consumer<Twin<Entity, Location>> hitAction = data.getUnsafe("hit_action", null);
            final Consumer<Twin<Entity, Location>> relativeHitAction = data.getUnsafe("relative_hit_action", null);
            final Consumer<Twin<Entity, Entity>> hitEntityAction = data.getUnsafe("hit_entity_action", null);
            final Predicate<Twin<Entity, Entity>> hitEntityCondition = data.getUnsafe("hit_entity_condition", null);
            if (rayAction == null && hitAction == null && hitEntityAction == null && hitEntityCondition == null && relativeHitAction == null)
                return;
            final RaycastType rayType = data.getUnsafe("ray_type");
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
                default -> null;
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
                for (Vector position : positions) {
                    l.setX(position.getX());
                    l.setY(position.getY());
                    l.setZ(position.getZ());
                    rayAction.accept(l);
                }
            }
        }, new ReadableOptions()
                .add(Readable.number(), Map.of("max_distance", 100d, "step_distance", 1d, "ray_size", 1d))
                .add("offset", Actionable.VECTOR, null)
                .add("ray_action", Factories.LOCATION_ACTION, null)
                .add("hit_action", Factories.ENTITY_LOCATION_ACTION, null)
                .add("relative_hit_action", Factories.ENTITY_LOCATION_ACTION, null)
                .add("hit_entity_action", Factories.ENTITY_ENTITY_ACTION, null)
                .add("hit_entity_condition", Factories.ENTITY_ENTITY_CONDITION, null)
                .add("ray_type", Readable.enumeration(RaycastType.class), RaycastType.BOTH)
        ));

        register(new ActionFactory<>("damage", (data, entity) -> {
            if (entity instanceof LivingEntity livingEntity)
                livingEntity.damage(data.getDouble("amount"), entity.getUniqueId() == livingEntity.getUniqueId() ? null : entity);
        }, new ReadableOptions()
                .add("amount", Readable.number(), 1)
        ));

        register(new ActionFactory<>("apply_effect", (data, entity) -> {
            final PotionEffectType type = data.getUnsafe("effect");
            if (entity instanceof LivingEntity livingEntity) {
                if (data.getBoolean("remove"))
                    livingEntity.removePotionEffect(type);
                else
                    livingEntity.addPotionEffect(new PotionEffect(type, data.getInt("duration"), data.getInt("amplifier"), data.getBoolean("is_ambient"), data.getBoolean("show_particles"), data.getBoolean("show_icon")));
            }
        }, new ReadableOptions()
                .add("effect", Actionable.EFFECT_TYPE)
                .add("remove", Readable.bool(), false)
                .add(Readable.integer(), Map.of("duration", 20, "amplifier", 0))
                .add(Readable.bool(), Map.of("remove", false, "is_ambient", false, "show_particles", true, "show_icon", true))
        ));

        register(new ActionFactory<>("dismount", (data, entity) -> {
            final Entity vehicle = entity.getVehicle();
            if (vehicle != null) {
                vehicle.removePassenger(entity);
                var action = data.<Consumer<Entity>>getUnsafe("vehicle_action", null);
                if (action != null) action.accept(vehicle);
            }
        }, new ReadableOptions().add("vehicle_action", Factories.ENTITY_ACTION, null)));

        register(new ActionFactory<>("vehicle_action", (data, entity) -> {
            final Entity vehicle = entity.getVehicle();
            if (vehicle != null)
                data.<Consumer<Twin<Entity, Entity>>>getUnsafe("action").accept(Twin.of(entity, vehicle));
        }, new ReadableOptions().add("action", Factories.ENTITY_ENTITY_ACTION)));
    }
}
