package com.aegamesi.java_visualizer.backend;

import com.sun.jdi.BooleanValue;
import com.sun.jdi.ClassType;
import com.sun.jdi.InterfaceType;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import org.apache.xmlbeans.impl.xb.xmlconfig.Extensionconfig;

import java.util.*;

public class TracerUtils {
	public static com.sun.jdi.Value invokeSimple(ThreadReference thread, ObjectReference r, String name) {
		try {
			return r.invokeMethod(thread, r.referenceType().methodsByName(name).get(0), Collections.emptyList(), 0);
		} catch (Exception e) {
			return null;
		}
	}

	public static Iterator<Value> getIterator(ThreadReference thread, ObjectReference obj, String methodName) {
		ObjectReference i = (ObjectReference) invokeSimple(thread, obj, methodName);
		return new Iterator<com.sun.jdi.Value>() {
			@Override
			public boolean hasNext() {
				return ((BooleanValue) invokeSimple(thread, i, "hasNext")).value();
			}

			@Override
			public com.sun.jdi.Value next() {
				return invokeSimple(thread, i, "next");
			}
		};
	}

	public static Set<String> getImplementedInterfaces(ObjectReference obj) {
		Set<String> result = new HashSet<>();
		Queue<InterfaceType> queue = new LinkedList<>(((ClassType) obj.referenceType()).interfaces());
		while (!queue.isEmpty()) {
			InterfaceType next = queue.poll();
			result.add(next.name());
			queue.addAll(next.superinterfaces());
		}
		return result;
	}

	public static boolean doesImplementInterface(ObjectReference obj, String iface) {
		if (obj.referenceType() instanceof ClassType) {
			Queue<InterfaceType> queue = new LinkedList<>(((ClassType) obj.referenceType()).interfaces());
			while (!queue.isEmpty()) {
				InterfaceType t = queue.poll();
				if (t.name().equals(iface)) {
					return true;
				}
				queue.addAll(t.superinterfaces());
			}
		}
		return false;
	}

	// TODO clean up!!
	public static String displayNameForType(ObjectReference obj) {
		String fullName = obj.referenceType().name();
		return fullName;
	}
}
