package org.datastax.simulacra.environment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.datastax.simulacra.agents.Agent;
import org.datastax.simulacra.utils.Map2ListSerializer;

import java.util.*;

import static java.util.stream.Collectors.toSet;
import static org.datastax.simulacra.utils.Utils.*;

public class SubArea {
    @JsonProperty
    private final String name;

    @JsonProperty
    @JsonSerialize(using = Map2ListSerializer.class)
    private final Map<String, Item> items;

    @JsonIgnore
    private final Map<String, Agent> agents;

    @JsonIgnore
    private Area area;

    public SubArea(String name, Map<String, Item> items, Map<String, Agent> agents) {
        this.name = name;
        this.items = items;
        this.agents = agents;
    }

    @JsonCreator
    public static SubArea createSubArea(@JsonProperty("name") String name, @JsonProperty("items") List<Item> _items) {
        var items = (_items == null) ? List.<Item>of() : _items;
        return new SubArea(name, associateBy(items, i -> normalizeName(i.name())), new HashMap<>());
    }

    public void register(Agent agent) {
        agents.put(normalizeName(agent.getName()), agent);
    }

    public void unregister(Agent agent) {
        agents.remove(normalizeName(agent.getName()));
    }

    public Item findItem(String name) {
        return items.get(normalizeName(name));
    }

    public void findAndUpdateItem(String name, String status) {
        var item = findItem(name);

        if (item != null) {
            item.setStatus(status);
        }
    }

    public Set<String> itemNames() {
        return items.keySet();
    }

    public Set<String> agentNamesExcluding(String excluding) {
        return agents.keySet().stream().filter(k -> !k.equals(excluding)).collect(toSet());
    }

    public String name() {
        return name;
    }

    public Map<String, Item> items() {
        return items;
    }

    public Agent findAgent(String name) {
        System.out.println("Finding " + normalizeName(name) + " in " + agents.keySet());
        return agents.get(normalizeName(name));
    }

    public Map<String, Agent> agents() {
        return agents;
    }

    public Area area() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
    }

    @Override
    public String toString() {
        return "SubArea{" +
            "name='" + name + '\'' +
            ", items=" + items.keySet() +
            ", agents=" + agents.keySet() +
            ", area=" + area.name() +
            '}';
    }
}
