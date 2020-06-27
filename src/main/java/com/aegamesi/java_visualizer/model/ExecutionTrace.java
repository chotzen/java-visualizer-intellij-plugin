package com.aegamesi.java_visualizer.model;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ExecutionTrace {
	public List<Frame> frames = new ArrayList<>();
	public Map<Long, HeapEntity> heap = new TreeMap<>();
	public Map<String, Value> statics = new TreeMap<>();

	public String toJsonString() {
		JSONObject obj = new JSONObject();
		obj.put("frames", frames.stream().map(Frame::toJson).toArray());
		obj.put("heap", heap.values().stream().map(HeapEntity::toJson).toArray());
		return obj.toString();
	}

	public void recursivelyPrint(int level, HeapEntity ent) {
	    String pref = "";
	    for (int i = 0; i < level; i++) {
	    	pref += " ";
		}
		System.out.println(pref + "ID: " + ent.id + " | Type: " + ent.type.toString());
		switch (ent.type) {
			case LIST:
			case SET:
				HeapList heapList = (HeapList)ent;
				for (Value v : heapList.items) {
					System.out.println(pref + "-" + v.toString());
					if (v.type == Value.Type.REFERENCE) {
						recursivelyPrint(level + 1, heap.get(v.reference));
					}
				}
			    break;
			case MAP:
			    HeapMap heapMap = (HeapMap)ent;
			    for (HeapMap.Pair p : heapMap.pairs) {
					System.out.println(pref + "-" + p.key.toString() + " : " + p.val.toString());
					if (p.val.type == Value.Type.REFERENCE) {
						recursivelyPrint(level + 1, heap.get(p.val.reference));
					}
				}
				break;
			case OBJECT:
				HeapObject obj = (HeapObject)ent;
				for (String key : obj.fields.keySet()) {
					System.out.println(pref + key + ": " + obj.fields.get(key).toString());
					if (obj.fields.get(key).type == Value.Type.REFERENCE) {
						recursivelyPrint(level + 1, heap.get(obj.fields.get(key).reference));
					}
				}
				break;
			case PRIMITIVE:
			    HeapPrimitive prim = (HeapPrimitive)ent;
				System.out.println(pref + prim.value.toString());
				if (prim.value.type == Value.Type.REFERENCE) {
					recursivelyPrint(level + 1, heap.get(prim.value.reference));
				}
				break;
		}
	}

	public static ExecutionTrace fromJsonString(String str) {
		JSONObject o = new JSONObject(str);
		ExecutionTrace trace = new ExecutionTrace();
		for (Object s : o.getJSONArray("frames")) {
			trace.frames.add(Frame.fromJson((JSONObject) s));
		}
		for (Object s : o.getJSONArray("heap")) {
			HeapEntity e = HeapEntity.fromJson((JSONObject) s);
			trace.heap.put(e.id, e);
		}
		return trace;
	}
}
