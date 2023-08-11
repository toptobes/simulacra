package org.datastax.simulacra.environment;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Item {
    @JsonProperty
    private String name;

    @JsonProperty
    private String status;

    public String name() {
        return name;
    }

    public String status() {
        return status;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Item{" +
            "name='" + name + '\'' +
            ", status='" + status + '\'' +
            '}';
    }
}
