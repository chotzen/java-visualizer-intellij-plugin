package com.aegamesi.java_visualizer.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HeapSet extends HeapEntity {
    public Set<Value> items = new HashSet<>();

    @Override
    public boolean hasSameStructure(HeapEntity other) {
        if (other instanceof HeapSet) {
            return items.size() == ((HeapSet) other).items.size();
        }
        return false;
    }

    @Override
    JSONObject toJson() {
        JSONObject o = super.toJson();
        o.put("items", items.stream().map(Value::toJson).toArray());
        return o;
    }

    static HeapList fromJson(JSONObject o) {
        HeapList e = new HeapList();
        for (Object item : o.getJSONArray("items")) {
            e.items.add(Value.fromJson((JSONArray) item));
        }
        return e;
    }
}