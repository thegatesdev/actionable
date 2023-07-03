package io.github.thegatesdev.actionable.registry;

import io.github.thegatesdev.maple.data.DataElement;
import io.github.thegatesdev.maple.data.DataMap;
import io.github.thegatesdev.maple.data.DataValue;
import io.github.thegatesdev.maple.data.Keyed;
import io.github.thegatesdev.maple.exception.ElementException;
import io.github.thegatesdev.maple.read.struct.DataType;

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
public abstract class BuilderRegistry<Data, Fac extends Builder<Data>> implements Keyed, DataType<DataValue<Data>> {
    protected final String key;
    protected Info info;

    protected BuilderRegistry(String key) {
        this.key = key;
    }

    public abstract Collection<String> keys();

    public abstract Fac get(String key);

    @Override
    public DataValue<Data> read(DataElement element) {
        var data = element.requireOf(DataMap.class);
        var type = data.getString("type");
        var factory = get(type);
        if (factory == null) throw new ElementException(data, "Specified %s %s does not exist".formatted(key, type));
        return DataValue.of(factory.build(data));
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public Info info() {
        if (info == null) info = new Info(key);
        return info;
    }

    public static class Static<Data, Fac extends Builder<Data>> extends BuilderRegistry<Data, Fac> {
        private final Map<String, Fac> factories = new HashMap<>();

        protected Static(String key) {
            super(key);
        }

        // -- REGISTRATION

        public void registerStatic() {
        }

        public void register(Fac factory) {
            factories.putIfAbsent(factory.key(), factory);
        }

        // -- GET

        @Override
        public Fac get(String key) {
            return factories.get(key);
        }

        @Override
        public Collection<String> keys() {
            return factories.keySet();
        }
    }
}
