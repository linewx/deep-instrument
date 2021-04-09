package com.linewx.deep.instrumentation.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;

public class DeepInstrumentationAgent {
    private static Logger LOGGER = LoggerFactory.getLogger(DeepInstrumentationAgent.class);

    public static void premain(String agentArgs, Instrumentation inst) {
        LOGGER.info("[Deep Agent] In premain");

        String className = "com.hp.maas.platform.ems.impl.EntityManagementServiceImpl";
        transformClass(className,inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        LOGGER.info("[Deep Agent] In agentmain");
        LOGGER.info("[Deep Agent] In agentmain");

        String className = "com.hp.maas.platform.ems.impl.EntityManagementServiceImpl";
        transformClass(className,inst);
    }

    private static void transformClass(String className, Instrumentation instrumentation) {
        Class<?> targetCls = null;
        ClassLoader targetClassLoader = null;

        for(Class<?> clazz: instrumentation.getAllLoadedClasses()) {

            if(clazz.getName().startsWith("com.hp.maas.platform")) {
                LOGGER.info("injecting class {}", clazz.getName());
                targetCls = clazz;
                targetClassLoader = targetCls.getClassLoader();
                String logName = getLogName(clazz);
                if (logName != null) {
                    transform(targetCls, targetClassLoader, instrumentation, logName);
                }
            }
        }
        //throw new RuntimeException("Failed to find class [" + className + "]");
    }

    static Boolean hasLogger(Class<?> clazz) {
        LOGGER.info("finding logger in class {}", clazz.getName());
        try{
            Field logger = clazz.getField("LOGGER");
            LOGGER.info("found logger in class {}", clazz.getName());
            return true;
        }catch (Exception e){
            LOGGER.error("no logger found in class {}", clazz.getName(), e);
            return false;
        }
    }

    static String getLogName(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
//            if (Logger.class.isAssignableFrom(field.getType())) {
            if ("org.slf4j.Logger".equals(field.getType().getName())) {
                LOGGER.info("found logger {} in class {}", field.getName(), clazz.getName());
                return field.getName();
            }
        }
        return null;
    }

    private static void transform(Class<?> clazz, ClassLoader classLoader, Instrumentation instrumentation, String logName) {
        DeepTransformer dt = new DeepTransformer(clazz.getName(), classLoader, logName);
        instrumentation.addTransformer(dt, true);
        try {
            instrumentation.retransformClasses(clazz);
        } catch (Exception ex) {
            throw new RuntimeException("Transform failed for class: [" + clazz.getName() + "]", ex);
        }
    }

}
