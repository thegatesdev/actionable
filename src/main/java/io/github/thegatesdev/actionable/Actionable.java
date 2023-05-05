package io.github.thegatesdev.actionable;

import io.github.thegatesdev.maple.data.DataMap;
import io.github.thegatesdev.maple.exception.ElementException;
import io.github.thegatesdev.mapletree.data.DataType;
import io.github.thegatesdev.mapletree.data.Readable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;

public class Actionable extends JavaPlugin {
    @Override
    public void onLoad() {
        Factories.registerAll();
        Factories.lock();
    }

    public static final Readable<PotionEffectType> EFFECT_TYPE = Readable.single("potion_effect", PotionEffectType.class, primitive -> {
                final String effectName = primitive.requireValue(String.class);
                final PotionEffectType byName = PotionEffectType.getByName(effectName);
                if (byName == null)
                    throw new ElementException(primitive, "effect '%s' does not exist!".formatted(effectName));
                return byName;
            })
            .info(info -> info.possibleValues(Arrays.stream(PotionEffectType.values()).map(PotionEffectType::getName).toList()));
    public static final Readable<Vector> VECTOR = new Readable<>("vector", Vector.class, element -> {
        if (element.isMap()) {
            final DataMap map = element.asMap();
            return new Vector(map.getDouble("x", 0), map.getDouble("y", 0), map.getDouble("z", 0));
        } else if (element.isPrimitive()) {
            final float v = element.asPrimitive().floatValue();
            return new Vector(v, v, v);
        } else if (element.isList()) {
            final List<Number> list = element.asList().primitiveList(Number.class);
            if (list.isEmpty()) throw new ElementException(element, "Vector list cannot be empty!");
            if (list.size() > 3)
                throw new ElementException(element, "Cannot have more than 3 values for a Vector! (x,y,z)");
            final float[] out = new float[3];
            float last = 0;// When last is used it will have always been assigned, since the list must have 1 value
            for (int i = 0, size = list.size(); i < 3; i++) {
                if (size <= i) out[i] = last;// We have passed the last item in the supplied list, use last value
                else out[i] = last = list.get(i).floatValue();
            }
            return new Vector(out[0], out[1], out[2]);
        } else {
            throw new ElementException(element, "Expected a map with x y z, a list with 3 or less elements, or a number");
        }
    }).info(info -> info.description("A vector represents a 3 dimensional point, with x, y and z number values.").representation("A map with x y and z values, a list with 1 or more values or a single value."));

    public static final DataType<Component> COLORED_STRING = Readable.single("colored_text", Component.class, primitive -> MiniMessage.miniMessage().deserialize(primitive.stringValue()))
            .info(info -> info.description("Text that can be formatted with MiniMessage formatting"));
}
