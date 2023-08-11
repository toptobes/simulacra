package org.datastax.simulacra.environment;

import java.util.*;

import static org.datastax.simulacra.Utils.*;

public class WorldMap {
    public static final WorldMap GLOBAL = new WorldMap();

    private final Map<String, Area> areas = new HashMap<>();

    public Area findArea(String name) {
        return areas.get(normalizeName(name));
    }

    public Set<String> areaNames() {
        return areas.keySet();
    }

    public void addAreas(Collection<Area> areas) {
        System.out.println(map(areas, Area::name));
        this.areas.putAll(associateBy(areas, a -> normalizeName(a.name())));
    }
}
