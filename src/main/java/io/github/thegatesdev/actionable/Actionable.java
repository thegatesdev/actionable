package io.github.thegatesdev.actionable;

import io.github.thegatesdev.actionable.registry.Registries;
import io.github.thegatesdev.maple.data.DataValue;
import io.github.thegatesdev.maple.exception.ElementException;
import io.github.thegatesdev.maple.read.Readable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class Actionable extends JavaPlugin {
    @Override
    public void onLoad() {
        Registries.registerAll();
        Registries.lock();
    }

    public static final Readable<DataValue<PotionEffectType>> EFFECT_TYPE = Readable.value("effect", value ->
        value.requireType(String.class).then(PotionEffectType.class, effectName -> {
            PotionEffectType effectType = PotionEffectType.getByName(effectName);
            if (effectType == null)
                throw new ElementException(value, "this effect type does not exist: " + effectName);
            return effectType;
        })
    );

    public static final Readable<DataValue<Component>> COLORED_STRING = Readable.value("colored_text", value ->
        value.requireType(String.class).then(Component.class, text -> MiniMessage.miniMessage().deserialize(text))
    );

    public static final Readable<DataValue<Vector>> VECTOR = Readable.any("vector", element -> {
        final DataValue<Number> xVal, yVal, zVal;
        if (element.isMap()) {
            var map = element.asMap();
            xVal = map.getValueOf("x", Number.class);
            yVal = map.getValueOf("y", Number.class);
            zVal = map.getValueOf("z", Number.class);
        } else if (element.isValue()) {
            xVal = yVal = zVal = element.asValue().requireType(Number.class);
        } else throw new ElementException(element, "Could not read a vector from this element");
        final var vec = new Vector();

        return DataValue.of(Vector.class, () -> vec
            .setX(xVal.doubleValue())
            .setY(yVal.doubleValue())
            .setZ(zVal.doubleValue())
        );
    });
}
