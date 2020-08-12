package com.aegamesi.java_visualizer.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeapCollection extends HeapEntity {

	public List<Value> items = new ArrayList<>();

	@Override
	public boolean hasSameStructure(HeapEntity other) {
		if (other instanceof HeapCollection) {
			return items.size() == ((HeapCollection) other).items.size();
		}
		return false;
	}

	@Override
	JSONObject toJson() {
		JSONObject o = super.toJson();
		o.put("items", items.stream().map(Value::toJson).toArray());
		return o;
	}

	static HeapCollection fromJson(JSONObject o) {
		HeapCollection e = new HeapCollection();
		for (Object item : o.getJSONArray("items")) {
			e.items.add(Value.fromJson((JSONArray) item));
		}
		return e;
	}
}
