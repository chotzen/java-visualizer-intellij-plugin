package com.aegamesi.java_visualizer.backend;

import com.aegamesi.java_visualizer.model.ExecutionTrace;
import com.aegamesi.java_visualizer.model.Frame;
import com.aegamesi.java_visualizer.model.HeapEntity;
import com.aegamesi.java_visualizer.model.HeapList;
import com.aegamesi.java_visualizer.model.HeapMap;
import com.aegamesi.java_visualizer.model.HeapObject;
import com.aegamesi.java_visualizer.model.HeapPrimitive;
import com.aegamesi.java_visualizer.model.Value;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ModuleRootManager;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.ByteValue;
import com.sun.jdi.CharValue;
import com.sun.jdi.DoubleValue;
import com.sun.jdi.Field;
import com.sun.jdi.FloatValue;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.InvalidStackFrameException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.LongValue;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ShortValue;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VoidValue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import static com.aegamesi.java_visualizer.backend.TracerUtils.displayNameForType;
import static com.aegamesi.java_visualizer.backend.TracerUtils.doesImplementInterface;
import static com.aegamesi.java_visualizer.backend.TracerUtils.getIterator;
import static com.aegamesi.java_visualizer.backend.TracerUtils.invokeSimple;

/**
 * Some code from traceprinter, written by David Pritchard (daveagp@gmail.com)
 */
public class Tracer {
	private static final String[] INTERNAL_PACKAGES = {
			"java.",
			"javax.",
			"sun.",
			"jdk.",
			"com.sun.",
			"com.intellij.",
			"com.aegamesi.java_visualizer.",
			"org.junit.",
			"jh61b.junit.",
			"jh61b.",
	};
	private static final List<String> BOXED_TYPES = Arrays.asList("Byte", "Short", "Integer", "Long", "Float", "Double", "Character", "Boolean");
	private static final boolean SHOW_ALL_FIELDS = false;
	private static final List<ReferenceType> STATIC_LISTABLE = new ArrayList<>();

	private ThreadReference thread;
	private ExecutionTrace model;

	/*
	Converting actual heap objects requires running code on the suspended VM thread.
	However, once we start running code on the thread, we can no longer read frame locals.
	Therefore, we have to convert all heap objects at the very end.
	*/
	private TreeMap<Long, ObjectReference> pendingConversion = new TreeMap<>();

	public Tracer(ThreadReference thread) {
		this.thread = thread;
	}

	public ExecutionTrace getModel() throws IncompatibleThreadStateException {
		model = new ExecutionTrace();

		Map<Frame, StackFrame> frameMap = new HashMap<>();

		for (StackFrame frame : thread.frames()) {
			if (shouldShowFrame(frame)) {
				Frame e = convertFrame(frame);
				frameMap.put(e, frame);
				model.frames.add(e);
			}
		}

		// Convert (some) statics
		for (ReferenceType rt : STATIC_LISTABLE) {
			if (rt.isInitialized() && !isInternalPackage(rt.name())) {
				for (Field f : rt.visibleFields()) {
					if (f.isStatic()) {
						String name = rt.name() + "." + f.name();
						model.statics.put(name, convertValue(rt.getValue(f)));
					}
				}
			}
		}

		// Convert heap
		Set<Long> heapDone = new HashSet<>();
		while (!pendingConversion.isEmpty()) {
			Map.Entry<Long, ObjectReference> first = pendingConversion.firstEntry();
			long id = first.getKey();
			ObjectReference obj = first.getValue();
			pendingConversion.remove(id);
			if (heapDone.contains(id))
				continue;
			heapDone.add(id);
			HeapEntity converted = convertObject(obj);
			converted.id = id;
			model.heap.put(id, converted);
		}

		// Deal with holes
		for (int i = model.frames.size() - 1; i > 0; i--) {
		    try {
				Frame fr = model.frames.get(i);
				StackFrame frame = frameMap.get(fr);
				String line = getCurrentLine(frame);
				String[] pieces = line.split("=");
				if (pieces.length == 2) {
					// it's an assignment. probably.
					// for now, ignore it if the left side has any parens, we don't want to deal with that
					// could either be method calls or casting. just yuck in general
					if (!pieces[0].contains("(")) {
						// now, we will assume that the "word" closest to the equals sign is the name of the variable.
						String[] leftPieces = pieces[0].trim().split(" ");
						String varName = leftPieces[leftPieces.length - 1].trim();
						List<String> path = new ArrayList<>(Arrays.asList(varName.split("\\.")));
						Value cur = fr.locals.get(path.get(0));
						if (path.size() > 1) { // not a top-level variable, i.e. is a reference & we need to iterate over the rest of the path
							for (int k = 1; k < path.size(); k++) {
								HeapEntity next = model.heap.get(cur.reference);
								if (next instanceof HeapObject) {
									if (((HeapObject) next).fields.keySet().contains(path.get(k).trim())) {
										cur = ((HeapObject) next).fields.get(path.get(k).trim());
									}
								} else {
									cur = null;
									break;
								}
							}
						} else if (cur == null) {
							// if the variable doesn't already exist, create it. only occurs for locals, since
							// indirect references need to already be declared.
							cur = new Value();
							fr.locals.put(varName, cur);
						}
						if (cur != null) {
							cur.type = Value.Type.HOLE;
							cur.holeDest = model.frames.get(i - 1); // last frame converted is in position 0
						}
					}
				}
			} catch (Exception e) {
				System.err.println("Error: frames not loaded. ");
			}
		}

		return model;
	}

	// TODO clean this up
	private Frame convertFrame(StackFrame sf) {
		Frame output = new Frame();
		output.name = sf.location().method().name() + ":" + sf.location().lineNumber();

		if (sf.thisObject() != null) {
			output.locals.put("this", convertValue(sf.thisObject()));
		}

		// list args first
		/* KNOWN ISSUE:
		   .arguments() gets the args which have names in LocalVariableTable,
           but if there are none, we get an IllegalArgExc, and can use .getArgumentValues()
           However, sometimes some args have names but not all. Such as within synthetic
           lambda methods like "lambda$inc$0". For an unknown reason, trying .arguments()
           causes a JDWP error in such frames. So sadly, those frames are incomplete. */

		boolean JDWPerror = false;
		try {
			sf.getArgumentValues();
		} catch (com.sun.jdi.InternalException e) {
			if (e.toString().contains("Unexpected JDWP Error:")) {
				// expect JDWP error 35
				JDWPerror = true;
			} else {
				throw e;
			}
		}

		List<LocalVariable> frame_vars, frame_args;
		boolean completed_args = false;
		try {
			// args make sense to show first
			frame_args = sf.location().method().arguments(); //throwing statement
			completed_args = !JDWPerror && frame_args.size() == sf.getArgumentValues().size();
			for (LocalVariable lv : frame_args) {
				if (lv.name().equals("args")) {
					com.sun.jdi.Value v = sf.getValue(lv);
					if (v instanceof ArrayReference && ((ArrayReference) v).length() == 0)
						continue;
				}

				try {
				    Value v = convertValue(sf.getValue(lv));
				    v.isArg = true;
					output.locals.put(lv.name(), v);
				} catch (IllegalArgumentException exc) {
					System.out.println("That shouldn't happen!");
				}
			}
		} catch (AbsentInformationException e) {
			// ok.
		}

		// args did not have names, like a functional interface call...
		// although hopefully a future Java version will give them names!
		if (!completed_args && !JDWPerror) {
			try {
				List<com.sun.jdi.Value> anon_args = sf.getArgumentValues();
				for (int i = 0; i < anon_args.size(); i++) {
					String name = "param#" + i;
					Value v = convertValue(anon_args.get(i));
					v.isArg = true;
					output.locals.put(name, v);
				}
			} catch (InvalidStackFrameException e) {
				// ok.
			}
		}

		// now non-args
		try {
            /* We're using the fact that the hashCode tells us something
               about the variable's position (which is subject to change)
               to compensate for that the natural order of variables()
               is often different from the declaration order (see LinkedList.java) */
			frame_vars = sf.location().method().variables();
			TreeMap<Integer, LocalVariable> orderByHash = null;
			int offset = 0;
			for (LocalVariable lv : frame_vars) {
				if (!lv.isArgument() && (SHOW_ALL_FIELDS || !lv.name().endsWith("$"))) {
					if (orderByHash == null) {
						offset = lv.hashCode();
						orderByHash = new TreeMap<>();
					}
					orderByHash.put(lv.hashCode() - offset, lv);
				}
			}
			if (orderByHash != null) {
				for (Map.Entry<Integer, LocalVariable> me : orderByHash.entrySet()) {
					try {
						LocalVariable lv = me.getValue();
						output.locals.put(lv.name(), convertValue(sf.getValue(lv)));
					} catch (IllegalArgumentException exc) {
						// variable not yet defined, don't list it
					}
				}
			}
		} catch (AbsentInformationException ex) {
			// ok.
		}

		return output;
	}

	private Value convertReference(ObjectReference obj) {
		// Special handling for boxed types
		if (obj.referenceType().name().startsWith("java.lang.")
				&& BOXED_TYPES.contains(obj.referenceType().name().substring(10))) {
			return convertValue(obj.getValue(obj.referenceType().fieldByName("value")));
		}

		long key = obj.uniqueID();
		pendingConversion.put(key, obj);

		// Actually create and return the reference
		Value out = new Value();
		out.referenceType = obj.type().name();
		out.type = Value.Type.REFERENCE;
		out.reference = key;
		return out;
	}

	private HeapEntity convertObject(ObjectReference obj) {
		if (obj instanceof ArrayReference) {
			ArrayReference ao = (ArrayReference) obj;
			int length = ao.length();

			HeapList out = new HeapList();
			out.type = HeapEntity.Type.LIST;
			out.label = ao.type().name();
			for (int i = 0; i < length; i++) {
				// TODO: optional feature, skip runs of zeros
				out.items.add(convertValue(ao.getValue(i)));
			}
			return out;
		} else if (obj instanceof StringReference) {
			HeapPrimitive out = new HeapPrimitive();
			out.type = HeapEntity.Type.PRIMITIVE;
			out.label = "String";
			out.value = new Value();
			out.value.type = Value.Type.STRING;
			out.value.stringValue = ((StringReference) obj).value();
			return out;
		}

		String typeName = obj.referenceType().name();
		/*if ((doesImplementInterface(obj, "java.util.List")
				|| doesImplementInterface(obj, "java.util.Set"))
				&& isInternalPackage(typeName)) {
			HeapList out = new HeapList();
			out.type = HeapEntity.Type.LIST; // XXX: or SET
			out.label = displayNameForType(obj);
			Iterator<com.sun.jdi.Value> i = getIterator(thread, obj);
			while (i.hasNext()) {
				out.items.add(convertValue(i.next()));
			}
			return out;
		} */

		if (doesImplementInterface(obj, "java.util.Map") && isInternalPackage(typeName)) {
			HeapMap out = new HeapMap();
			out.type = HeapEntity.Type.MAP;
			out.label = displayNameForType(obj);

			ObjectReference entrySet = (ObjectReference) invokeSimple(thread, obj, "entrySet");
			Iterator<com.sun.jdi.Value> i = getIterator(thread, entrySet);
			while (i.hasNext()) {
				ObjectReference entry = (ObjectReference) i.next();
				HeapMap.Pair pair = new HeapMap.Pair();
				pair.key = convertValue(invokeSimple(thread, entry, "getKey"));
				pair.val = convertValue(invokeSimple(thread, entry, "getValue"));
				out.pairs.add(pair);
			}
			return out;
		}

		// now, arbitrary objects
		HeapObject out = new HeapObject();
		out.type = HeapEntity.Type.OBJECT;
		out.label = displayNameForType(obj);

		ReferenceType refType = obj.referenceType();

		if (shouldShowDetails(refType) || doesImplementInterface(obj, "java.util.Set") || doesImplementInterface(obj, "java.util.List")) {
			// fields: -inherited -hidden +synthetic
			// visibleFields: +inherited -hidden +synthetic
			// allFields: +inherited +hidden +repeated_synthetic
			Map<Field, com.sun.jdi.Value> fields = obj.getValues(
					SHOW_ALL_FIELDS ? refType.allFields() : refType.visibleFields()
			);
			for (Map.Entry<Field, com.sun.jdi.Value> me : fields.entrySet()) {
				if (!me.getKey().isStatic() && (SHOW_ALL_FIELDS || !me.getKey().isSynthetic())) {
					String name = SHOW_ALL_FIELDS ? me.getKey().declaringType().name() + "." : "";
					name += me.getKey().name();
					Value value = convertValue(me.getValue());
					value.referenceType = me.getKey().typeName();
					out.fields.put(name, value);
				}
			}
		}
		return out;
	}

	private Value convertValue(com.sun.jdi.Value v) {
		Value out = new Value();
		if (v instanceof BooleanValue) {
			out.type = Value.Type.BOOLEAN;
			out.booleanValue = ((BooleanValue) v).value();
		} else if (v instanceof ByteValue) {
			out.type = Value.Type.LONG;
			out.longValue = ((ByteValue) v).value();
		} else if (v instanceof ShortValue) {
			out.type = Value.Type.LONG;
			out.longValue = ((ShortValue) v).value();
		} else if (v instanceof IntegerValue) {
			out.type = Value.Type.LONG;
			out.longValue = ((IntegerValue) v).value();
		} else if (v instanceof LongValue) {
			out.type = Value.Type.LONG;
			out.longValue = ((LongValue) v).value();
		} else if (v instanceof FloatValue) {
			out.type = Value.Type.DOUBLE;
			out.doubleValue = ((FloatValue) v).value();
		} else if (v instanceof DoubleValue) {
			out.type = Value.Type.DOUBLE;
			out.doubleValue = ((DoubleValue) v).value();
		} else if (v instanceof CharValue) {
            out.type = Value.Type.CHAR;
			out.charValue = ((CharValue) v).value();
		} else if (v instanceof VoidValue) {
			out.type = Value.Type.VOID;
		} else if (!(v instanceof ObjectReference)) {
			out.type = Value.Type.NULL;
		} else if (v instanceof StringReference) {
			out.type = Value.Type.STRING;
			out.stringValue = ((StringReference) v).value();
		} else {
			ObjectReference obj = (ObjectReference) v;
			out = convertReference(obj);
		}
		return out;
	}

	public String getCurrentLine(StackFrame frame) {
		int lineNumber = frame.location().lineNumber();
		String cPath = ModuleRootManager.getInstance(ModuleManager.getInstance(ProjectManager.getInstance().getOpenProjects()[0])
				.getModules()[0]).getContentRoots()[0].toString();
		int lnN = 0;
		String line = null;
		Scanner in = null;
		try {
			// for some reason we get backslashes here.
			String fPath = frame.location().sourcePath().replaceAll("\\\\", "/");

			// Assume (incorrectly?) that we're working in src/
			in = new Scanner(new File(cPath.substring(7) + "/src/" + fPath));
			while (in.hasNextLine() && lnN < lineNumber) {
				line = in.nextLine();
				lnN++;
			}
		} catch (AbsentInformationException | FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			in.close();
		}

		if (lnN == lineNumber) {
			return line.replaceAll("\\s\\s*", " ").trim();
		}
		return null;
	}

	// input format: [package.]ClassName:lineno or [package.]ClassName
	private static boolean isInternalPackage(final String name) {
		return Arrays.stream(INTERNAL_PACKAGES).anyMatch(name::startsWith);
	}

	private static boolean shouldShowFrame(StackFrame frame) {
		Location loc = frame.location();
		return !isInternalPackage(loc.toString()) && !loc.method().name().contains("$access");
	}

	private static boolean shouldShowDetails(ReferenceType type) {
		return !isInternalPackage(type.name());
	}
}
