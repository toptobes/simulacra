package org.datastax.simulacra.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Map2ListSerializer extends JsonSerializer<Map<?, ?>> {
    @Override
    public void serialize(Map<?, ?> itemsMap, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        List<?> itemsList = new ArrayList<>(itemsMap.values());
        jsonGenerator.writeObject(itemsList);
    }
}
