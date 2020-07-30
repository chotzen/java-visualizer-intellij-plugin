package edu.caltech.cms.intelliviz.graph.logicalvisualization;

import com.aegamesi.java_visualizer.backend.TracerUtils;
import com.aegamesi.java_visualizer.model.ExecutionTrace;
import com.aegamesi.java_visualizer.model.HeapEntity;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ThreadReference;
import com.thoughtworks.qdox.model.expression.LogicalAnd;
import edu.caltech.cms.intelliviz.graph.ui.ClassNode;
import edu.caltech.cms.intelliviz.graph.INode;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Stream;

public abstract class LogicalVisualization {

    public enum VizPoint {
        ON_TRACE,
        ON_BUILD
    }

    public static List<LogicalVisualization> vizList = new ArrayList<>();

    private Map<String, Map<String, String>> classes, interfaces;

    public LogicalVisualization(Map<String, Map<String, String>> classParams, Map<String, Map<String, String>> interfaceParams) {
        this.classes = classParams;
        this.interfaces = interfaceParams;
    }

    protected abstract String getDisplayName();
    protected abstract HeapEntity applyOnTrace(ObjectReference ref, ThreadReference thread, ExecutionTrace trace, Map<String, String> params);
    protected abstract GraphStruct applyOnBuild(INode ref, Map<String, String> params);

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
    public GraphStruct applyBuild(INode ref) {
        if (ref instanceof ClassNode) {
            ClassNode obj = (ClassNode) ref;
            if (this.classes.containsKey(obj.name)) {
                return this.applyOnBuild(obj, this.classes.get(obj.name));
            }

            Optional<String> matched = this.interfaces.keySet().stream().filter(iface -> obj.implementedInterfaces.contains(iface)).findFirst();

            if (matched.isPresent()) {
                String iface = matched.get();
                return this.applyOnBuild(obj, this.interfaces.get(iface));
            }
        }
        return null;
    }

    /**
     * Applies the appropriate logical visuazliation to the given heap entity, during the tracing on the thread.
     * This means we can actually reflect/call methods on the thing, which has its benefits for accessing interface methods
     * Does nothing if the entity is improper.
     * @param ref the heap entity the visualization can be applied to
     * @param thread the thread to search
     * @return a small "heap" to be merged with the heap model
     */
    public HeapEntity applyTrace(ObjectReference ref, ThreadReference thread, ExecutionTrace model) {
        if (this.classes.containsKey(TracerUtils.displayNameForType(ref))) {
            return this.applyOnTrace(ref, thread, model, this.classes.get(TracerUtils.displayNameForType(ref)));
        }

        Optional<String> matched = this.interfaces.keySet().stream().filter(iface -> TracerUtils.doesImplementInterface(ref, iface)).findFirst();

        if (matched.isPresent()) {
            String iface = matched.get();
            return this.applyOnTrace(ref, thread, model, this.interfaces.get(iface));
        }

        return null;
    }

    protected static long getUniqueNegKey(ExecutionTrace model) {
        Random r = new Random();
        int c = - r.nextInt(10000);
        while (model.heap.containsKey((long)c)) {
            c = - r.nextInt(10000);
        }
        return c;
    }

}

