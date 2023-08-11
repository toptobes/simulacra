package org.datastax.simulacra.environment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.datastax.simulacra.utils.Map2ValuesSerializer;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.datastax.simulacra.utils.Utils.*;

public record Area(
    String name,
    String description,
    String category,
    @JsonSerialize(using = Map2ValuesSerializer.class) Map<String, SubArea> subareas
) {
    @JsonCreator
    public static Area createArea(
        @JsonProperty("name") String name,
        @JsonProperty("subareas") List<SubArea> subareas,
        @JsonProperty("description") String description,
        @JsonProperty("category") String category
    ) {
        var area = new Area(name, description, category, associateBy(subareas, s -> normalizeName(s.name())));
        subareas.forEach(subarea -> subarea.setArea(area));
        return area;
    }

    public SubArea findSubarea(String name) {
        return subareas.get(normalizeName(name));
    }

    public Set<String> subareaNames() {
        return subareas.keySet();
    }

    public static class Category {
        public static final String PUBLIC_AREA = "public_area";
        public static final String RESTAURANT = "restaurant";
        public static final String HOSPITAL = "hospital";
        public static final String OFFICE = "office";
        public static final String SCHOOL = "school";
        public static final String HOUSE = "house";
        public static final String SHOP = "shop";
        public static final String ETC = "etc";

        public static List<String> all() {
            return List.of(PUBLIC_AREA, RESTAURANT, HOSPITAL, OFFICE, SCHOOL, HOUSE, SHOP, ETC);
        }

        public static List<String> excluding(String ...excluded) {
            return filterNot(all(), List.of(excluded)::contains);
        }
    }
}
