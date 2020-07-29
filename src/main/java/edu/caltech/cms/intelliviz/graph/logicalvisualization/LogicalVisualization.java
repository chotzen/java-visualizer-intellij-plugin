package edu.caltech.cms.intelliviz.graph.logicalvisualization;

import com.aegamesi.java_visualizer.backend.TracerUtils;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ThreadReference;
import edu.caltech.cms.intelliviz.graph.ClassNode;
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

public abstract class LogicalVisualization {

    public enum VizPoint {
        ON_TRACE,
        ON_BUILD
    }

    static List<LogicalVisualization> vizList = new ArrayList<>();

    private Map<String, Map<String, String>> classes, interfaces;

    public LogicalVisualization(Map<String, Map<String, String>> classParams, Map<String, Map<String, String>> interfaceParams) {
        this.classes = classParams;
        this.interfaces = interfaceParams;
    }

    protected abstract String getDisplayName();
    protected abstract void applyOnTrace(ObjectReference ref, ThreadReference thread);
    protected abstract void applyOnBuild(INode ref);

    public static void main(String[] args) {
        try {
            LogicalVisualization.loadFromCfg();
        } catch (Exception e) {

        }
    }

    static {
        loadFromCfg();
    }

    // loads the classes to which it shall be applied
    static void loadFromCfg() {
        try {
            Object ob = new JSONParser().parse(new FileReader(new File(
                    LogicalVisualization.class.getClassLoader().getResource("config.json").getFile()
            )));
            JSONObject obj = (JSONObject) ob;

            String pkg = (String) obj.get("pkg"); // we know it's a list of strings

            Map visualizers = (Map) obj.get("visualizations"); // this definitely works
            for (Map.Entry<String, JSONObject> visualizer : (Set<Map.Entry<String, JSONObject>>)visualizers.entrySet()) {
                String className = visualizer.getKey();
                Map<String, Map<String, String>> classParams = null;
                if (visualizer.getValue().containsKey("classes")) {
                    JSONArray classes = (JSONArray) visualizer.getValue().get("classes");
                    classParams = getParams(classes);
                }

                Map<String, Map<String, String>> interfaceParams = null;
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
        } catch (ParseException | ClassNotFoundException | ClassCastException | FileNotFoundException e) {
            System.err.println("Error: Could not parse config.json");
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the
     * @param arr
     * @return
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
     * Applies the appropriate logical visualization to the given node. Does nothing
     * if the visualization is not appropriate.
     * @param ref the node to apply the reference to
     */
    public void applyBuild(INode ref, VizPoint vizPoint) {
        if (ref instanceof ClassNode) {
            ClassNode obj = (ClassNode) ref;
            if (this.classes.containsKey(obj.name) || this.interfaces.keySet().stream().anyMatch(iface -> obj.implementedInterfaces.contains(iface))) {
                this.applyOnBuild(obj);
            }
        }
    }

    public void applyTrace(ObjectReference ref, ThreadReference thread, Map<String, String> params) {
        if (this.classes.containsKey(TracerUtils.displayNameForType(ref)) ||
               this.interfaces.keySet().stream().anyMatch(iface -> TracerUtils.doesImplementInterface(ref, iface))) {
            this.applyOnTrace(ref, thread);
        }
    }
}

