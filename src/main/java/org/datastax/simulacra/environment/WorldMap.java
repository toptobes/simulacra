package org.datastax.simulacra.environment;

import java.util.*;

import static org.datastax.simulacra.utils.Utils.*;

public class WorldMap {
    public static final WorldMap GLOBAL = new WorldMap();

    private final Map<String, Area> areas = new HashMap<>();

    public Area findArea(String name) {
        return areas.get(normalizeName(name));
    }

    public Set<String> areaNames() {
        return areas.keySet();
    }

    public int areaCount() {
        return areas.size();
    }

    public void addAreas(Collection<Area> areas) {
        System.out.println("Addding areas " + map(areas, Area::name));
        this.areas.putAll(associateBy(areas, a -> normalizeName(a.name())));
    }
}
