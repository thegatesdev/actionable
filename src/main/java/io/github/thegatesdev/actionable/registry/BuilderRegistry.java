package io.github.thegatesdev.actionable.registry;

import io.github.thegatesdev.maple.data.DataElement;
import io.github.thegatesdev.maple.data.DataMap;
import io.github.thegatesdev.maple.data.DataValue;
import io.github.thegatesdev.maple.data.Keyed;
import io.github.thegatesdev.maple.exception.ElementException;
import io.github.thegatesdev.maple.read.struct.AbstractDataType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/*
Copyright (C) 2022  Timar Karels

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
public abstract class BuilderRegistry<Data, B extends DataBuilder<? extends Data>> extends AbstractDataType<DataValue<Data>> implements Keyed {

    protected BuilderRegistry(String key) {
        super(key);
    }

    public abstract Collection<String> keys();

    public abstract B get(String key);

    @Override
    public DataValue<Data> read(DataElement element) {
        var data = element.requireOf(DataMap.class);
        var type = data.getString("type");
        var factory = get(type);
        if (factory == null) throw new ElementException(data, "Specified %s %s does not exist".formatted(key, type));
        return DataValue.of(factory.build(data));
    }


    public static class Static<Data, B extends DataBuilder<? extends Data> & Keyed> extends BuilderRegistry<Data, B> {
        private final Map<String, B> builders = new HashMap<>();

        protected Static(String key) {
            super(key);
        }

        // -- REGISTRATION

        public void registerStatic() {
        }

        public void register(B builder) {
            builders.putIfAbsent(builder.key(), builder);
        }

        // -- GET

        @Override
        public B get(String key) {
            return builders.get(key);
        }

        @Override
        public Collection<String> keys() {
            return builders.keySet();
        }
    }
}
