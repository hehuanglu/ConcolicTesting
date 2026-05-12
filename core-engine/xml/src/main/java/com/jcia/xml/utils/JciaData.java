package com.jcia.xml.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class JciaData {

    private static final Logger logger = LogManager.getLogger(JciaData.class);

    /**
     * Path Ignore
     */
    public static final Integer TAG_IGNORE = 600;

    /**
     * Counters
     */
    public static final Integer DEPENDENCY_COUNTER = 100;
    public static final Integer NODE_COUNTER = 101;
    public static final Integer DFG_VERTEX_COUNTER = 102;

    /**
     * Node Lists
     */
    public static final Integer XML_FILE_NODES = 200;
    public static final Integer JSP_FILE_NODES = 201;
    public static final Integer JAVA_CLASS_NODES = 202;

    /**
     * Temp Data
     */
    public static final Integer TILES_DEFINITIONS_MAP = 400;

    public static final Integer JDT_KEY_NODE_ID = 401;

    /**
     * Package name set
     */
    public static final Integer PACKAGE_NAME_SET = 500;
    public static final Integer IMPORT_NAME_SET = 501;
    public static final Integer CLASS_NAME_SET = 502;

    /**
     * Paths
     */
    public static final Integer JAVA_SOURCE_PATH = 601;

    public static final Integer HIBERNATE_DATA_ACCESS_MANAGER = 700;


    private static final ThreadLocal<JciaData> THREAD_LOCAL = new ThreadLocal() {
        @Override
        protected Object initialValue() {
            JciaData jciaData = new JciaData();
            return jciaData;
        }

        @Override
        public void remove() {
            super.remove();
        }
    };

    private Map<Integer, Object> dataMap;

    private JciaData() {
        this.dataMap = new HashMap<>();
        this.resetCounters();
    }

    public void resetCounters() {
        // initialize default data
        this.dataMap.put(NODE_COUNTER, new Integer(1));
        this.dataMap.put(DEPENDENCY_COUNTER, new Integer(1));
        this.dataMap.put(DFG_VERTEX_COUNTER, new Integer(1));
    }

    public void resetDFGVertexCounter() {
        this.dataMap.put(DFG_VERTEX_COUNTER, new Integer(1));
    }

    public Integer generateNodeId() {
        Integer val = (Integer) this.dataMap.get(NODE_COUNTER);
        return (Integer) this.dataMap.put(NODE_COUNTER, new Integer(val + 1));
    }

    public Integer generateDependencyId() {
        Integer val = (Integer) this.dataMap.get(DEPENDENCY_COUNTER);
        return (Integer) this.dataMap.put(DEPENDENCY_COUNTER, new Integer(val + 1));
    }

    public Integer generateDFGVertexId() {
        Integer val = (Integer) this.dataMap.get(DFG_VERTEX_COUNTER);
        return (Integer) this.dataMap.put(DFG_VERTEX_COUNTER, new Integer(val + 1));
    }

    public static JciaData getInstance() {
        return THREAD_LOCAL.get();
    }

    public Object getData(Integer dataKey) {
        return dataMap.get(dataKey);
    }

    public Object putData(Integer dataKey, Object data) {
        return dataMap.put(dataKey, data);
    }

    public void removeData(Integer dataKey) {
        dataMap.remove(dataKey);
    }

    public static void clean() {
        THREAD_LOCAL.get().dataMap.clear();
        THREAD_LOCAL.get().resetCounters();
    }
}
