package io.github.thegatesdev.actionable.registry;

import io.github.thegatesdev.maple.data.DataMap;
import io.github.thegatesdev.maple.data.Keyed;
import io.github.thegatesdev.maple.read.struct.ReadableOptionsHolder;

public interface Builder<Data> extends ReadableOptionsHolder, Keyed {

    Data build(DataMap data);
}
