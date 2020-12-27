package edu.caltech.cms.intelliviz.graph.logicalvisualization;

import com.aegamesi.java_visualizer.backend.Tracer;
import com.aegamesi.java_visualizer.backend.TracerUtils;
import com.aegamesi.java_visualizer.model.ExecutionTrace;
import com.aegamesi.java_visualizer.model.HeapEntity;
import com.aegamesi.java_visualizer.model.HeapObject;
import com.aegamesi.java_visualizer.model.Value;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StackFrame;
import edu.caltech.cms.intelliviz.graph.GraphEdge;
import edu.caltech.cms.intelliviz.graph.logicalvisualization.visualizers.ScannerVisualization;
import edu.caltech.cms.intelliviz.graph.logicalvisualization.visualizers.TrieMapVisualization;
import edu.caltech.cms.intelliviz.graph.logicalvisualization.actions.VisualizationToggler;
import edu.caltech.cms.intelliviz.graph.logicalvisualization.visualizers.TupleVisualization;
import edu.caltech.cms.intelliviz.graph.ui.ClassNode;
import edu.caltech.cms.intelliviz.graph.Node;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public abstract class LogicalVisualization {

    public enum VizPoint {
        ON_TRACE,
        ON_BUILD
    }

    private static List<LogicalVisualization> vizList = new ArrayList<>();
    private static List<LogicalVisualization> enabledVisualizations = new ArrayList<>();

    private Map<String, Map<String, String>> classes, interfaces;

    public LogicalVisualization(Map<String, Map<String, String>> classParams, Map<String, Map<String, String>> interfaceParams) {
        this.classes = classParams;
        this.interfaces = interfaceParams;
    }

    protected abstract String getDisplayName();
    protected abstract HeapEntity applyOnTrace(ObjectReference ref, Tracer tracer, Map<String, String> params);
    protected abstract Node applyOnBuild(Node ref, Map<Long, Node> nodes, List<GraphEdge> edges, Map<String, String> params);

    public static void main(String[] args) {
        try {
            LogicalVisualization.loadFromCfg();
        } catch (Exception e) {

        }
    }

    // TODO: make this not static?

    // loads the classes to which it shall be applied
    public static void loadFromCfg() {
        try {
            Scanner sc = new Scanner(LogicalVisualization.class.getResourceAsStream("/config.json"));
            String s = "";
            while (sc.hasNext()) {
                s += sc.next();
            }
            Object ob = new JSONParser().parse(s);
            JSONObject obj = (JSONObject) ob;

            String pkg = (String) obj.get("pkg"); // we know it's a list of strings

            Map visualizers = (Map) obj.get("visualizations"); // this definitely works
            for (Map.Entry<String, JSONObject> visualizer : (Set<Map.Entry<String, JSONObject>>)visualizers.entrySet()) {
                String className = visualizer.getKey();
                Map<String, Map<String, String>> classParams = new HashMap<>();
                if (visualizer.getValue().containsKey("classes")) {
                    JSONArray classes = (JSONArray) visualizer.getValue().get("classes");
                    classParams = getParams(classes);
                }

                Map<String, Map<String, String>> interfaceParams = new HashMap<>();
                if (visualizer.getValue().containsKey("interfaces")) {
                    JSONArray interfaces = (JSONArray) visualizer.getValue().get("interfaces");
                    interfaceParams = getParams(interfaces);
                }

                String fullName = pkg + '.' + className;
                Class vizClass = Class.forName(fullName);
                Constructor[] constructors = vizClass.getConstructors();
                LogicalVisualization viz = (LogicalVisualization)constructors[0].newInstance(classParams, interfaceParams);
                vizList.add(viz);
                enabledVisualizations.add(viz);
            }
        } catch (ParseException | ClassNotFoundException | ClassCastException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            System.err.println("Error: Could not parse config.json");
            e.printStackTrace();
        }
    }

    /**
     * Extracts the classes/interfaces data from a JSON array
     * @param arr the json array of classes or interfaces
     * @return a map linking the [fully qualified] class/interface name to a params map
     */
    static Map<String, Map<String, String>> getParams(JSONArray arr) {
        List<JSONObject> objList = (List<JSONObject>)arr;
        Map<String, Map<String, String>> result = new HashMap<>();
        for (JSONObject classObj : objList) {
            Map<String, String> params = (Map<String, String>)((Map)classObj).get("params"); // this may break
            result.put((String) classObj.get("name"), params);
        }

        return result;
    }

    /**
     * Applies the appropriate logical visualization to the given node, after the visualization tree has
     * already been built. Does nothing if the visualization is not appropriate.
     * @param ref the node to apply the reference to
     */
    public Node applyBuild(Node ref, Map<Long, Node> nodes, List<GraphEdge> edges) {
        if (ref instanceof ClassNode && enabledVisualizations.contains(this)) {
            ClassNode obj = (ClassNode) ref;
            if (this.classes.containsKey(obj.name)) {
                return this.applyOnBuild(obj, nodes, edges, this.classes.get(obj.name));
            }

            Optional<String> matched = this.interfaces.keySet().stream().filter(iface -> obj.implementedInterfaces.contains(iface)).findFirst();

            if (matched.isPresent()) {
                String iface = matched.get();
                return this.applyOnBuild(obj, nodes, edges, this.interfaces.get(iface));
            }
        }
        return null;
    }

    /**
     * Applies the appropriate logical visuazliation to the given heap entity, during the tracing on the thread.
     * This means we can actually reflect/call methods on the thing, which has its benefits for accessing interface methods
     * Does nothing if the entity is improper.
     * @param ref the heap entity the visualization can be applied to
     * @param tracer the tracer to search
     * @return a small "heap" to be merged with the heap model
     */
    public HeapEntity applyTrace(Collection<StackFrame> frames, ObjectReference ref, Tracer tracer) {
        if (this.classes.containsKey(ref.type().name()) && enabledVisualizations.contains(this)) {
            for (StackFrame fr : frames) {
                if (fr.location().declaringType().name().equals(ref.type().name())) {
                    return null;
                }
            }
            //if (frames.stream().anyMatch(frame -> frame.location().declaringType().name))
            return this.applyOnTrace(ref, tracer, this.classes.get(TracerUtils.displayNameForType(ref)));
        }

        Optional<String> matched = this.interfaces.keySet().stream().filter(iface -> TracerUtils.doesImplementInterface(ref, iface)).findFirst();

        if (matched.isPresent()) {
            String iface = matched.get();
            for (StackFrame fr : frames) {
                if (TracerUtils.doesImplementInterface(fr.thisObject(), iface)) {
                    return null;
                }
            }
            try {

//                TracerUtils.doesImplementInterface(List.copyOf(frames).get(1).thisObject(), "edu.caltech.cs2.interfaces.IStack");
                return this.applyOnTrace(ref, tracer, this.interfaces.get(iface));
            } catch (NullPointerException npe) {
                System.out.println("Error: Failed to visualize");
            }
        }

        return null;
    }

    protected static long getUniqueNegKey(ExecutionTrace model) {
        Random r = new Random();
        int c = - r.nextInt(10000);
        while (model.heap.containsKey((long)c)) {
            c = - r.nextInt(10000);
        }
        System.out.println(c);
        return c;
    }

    protected static HeapObject convertParent(ObjectReference ref, Tracer tracer, Map<String, String> params) {
        HeapObject parent = new HeapObject();
        parent.fields = new HashMap<>();
        com.sun.jdi.Value sizeValue = TracerUtils.invokeSimple(tracer.thread, ref, params.get("sizeMethod"));
        Value convSize = tracer.convertValue("", sizeValue);
        parent.fields.put("size", convSize);
        parent.interfaces = new HashSet<>();
        parent.id = ref.uniqueID();
        parent.type = HeapEntity.Type.OBJECT;
        parent.label = TracerUtils.displayNameForType(ref);

        return parent;
    }

    public static List<LogicalVisualization> getEnabledVisualizations() {
        String[] ACTIONS = new String[] {
                "JavaVisualizer.ToggleCollectionsAction",
                "JavaVisualizer.ToggleMapsAction",
                "JavaVisualizer.ToggleTrieAction"
        };

        Class[] ALWAYS_ENABLED = new Class[] {
                ScannerVisualization.class,
                TrieMapVisualization.class,
                TupleVisualization.class
        };

        List<LogicalVisualization> result = new ArrayList<>();

        for (String actionID : ACTIONS) {
            VisualizationToggler ta = (VisualizationToggler) ActionManagerEx.getInstance().getAction(actionID);
            if (!ta.toggled) {
                Class[] visualizerClasses = ta.getVisualizers();
                for (Class visualizerClass : visualizerClasses) {
                    vizList.forEach(viz -> {
                        if (visualizerClass.equals(viz.getClass())) {
                            result.add(viz);
                        }
                    });
                }
            }
        }

        for (Class a : ALWAYS_ENABLED) {
            vizList.forEach(viz -> {
                if (a.equals(viz.getClass())) {
                    result.add(viz);
                }
            });
        }

        return result;
    }
}

