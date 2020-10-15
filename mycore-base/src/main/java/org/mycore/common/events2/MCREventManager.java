package org.mycore.common.events2;

import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.tools.MCRTopologicalSort2;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MCREventManager {

    private static class MCREventManagerInstanceHolder {
        private static final MCREventManager INSTANCE = new MCREventManager();
    }

    protected static final String EVENT_HANDLER_PROPERTY_PREFIX = "MCR.Event2.";

    private final List<Object> eventHandler = new ArrayList<>();

    private final Map<Object, List<Class>> beforeMap = new HashMap<>();

    private final Map<Object, List<Method>> objectMethodMap = new HashMap<>();

    private final Map<Class, Object> classObjectMap = new HashMap<>();

    private final List<Object> sortedEventHandler = new ArrayList<>();

    public static MCREventManager getInstance() {
        return MCREventManagerInstanceHolder.INSTANCE;
    }

    private MCREventManager() {
        final Map<String, String> eventHandler = MCRConfiguration2.getSubPropertiesMap(EVENT_HANDLER_PROPERTY_PREFIX);
        final Map<Object, List<Class>> afterMap = new HashMap<>();

        eventHandler.forEach((clazz, enabled) -> {
            if (Boolean.parseBoolean(enabled)) {

                try {
                    final Class<?> aClass = MCRClassTools.forName(clazz);
                    final Constructor<?> constructor = aClass.getConstructor();
                    final Object o = constructor.newInstance();
                    classObjectMap.put(aClass, o);

                    final MCREventHandler declaredAnnotation = aClass.getAnnotation(MCREventHandler.class);
                    if (declaredAnnotation != null) {

                        beforeMap.computeIfAbsent(o, (xx) -> new ArrayList<>())
                            .addAll(Arrays.asList(declaredAnnotation.before()));
                        afterMap.computeIfAbsent(o, (xx) -> new ArrayList<>())
                            .addAll(Arrays.asList(declaredAnnotation.after()));
                    } else {
                        throw new MCRConfigurationException(
                            "The configured EventHandler " + EVENT_HANDLER_PROPERTY_PREFIX
                                + clazz + " needs the Annotation MCREventHandler!");
                    }

                    this.eventHandler.add(o);
                    final Method[] methods = aClass.getMethods();
                    for (Method method : methods) {
                        final MCRHandlerMethod annotation = method.getDeclaredAnnotation(MCRHandlerMethod.class);
                        if (annotation != null) {
                            final int parameterCount = method.getParameterCount();

                            if (parameterCount != 1) {
                                throw new MCRException(
                                    "The method " + method.getName() + " of " + clazz
                                        + " needs to have one parameter!");
                            }

                            if (method.isVarArgs()) {
                                throw new MCRException(
                                    "The method " + method.getName() + " of " + clazz + " should not have varargs!");
                            }

                            if (!method.canAccess(o)) {
                                throw new MCRException(
                                    "The method " + method.getName() + " of " + clazz + " is not accessible!");
                            }

                            final Class<?> eventToHandle = method.getParameterTypes()[0];
                            if (!MCREvent.class.isAssignableFrom(eventToHandle)) {
                                throw new MCRException("The parameter of the method " + method.getName()
                                    + " is not assignable to " + MCREvent.class.getName());
                            }

                            objectMethodMap.computeIfAbsent(o, (k) -> new ArrayList<>()).add(method);
                        }
                    }
                } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException
                    | IllegalAccessException
                    | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        });

        afterMap.forEach((o1, afterList) -> {
            afterList.forEach(o2 -> {
                final Object o = classObjectMap.get(o2);
                final Class<?> aClass = o1.getClass();
                beforeMap.computeIfAbsent(o, (key) -> new ArrayList()).add(aClass);
            });
        });

        final MCRTopologicalSort2<Class> sorter = new MCRTopologicalSort2<Class>();

        for (Object o : this.eventHandler) {
            sorter.addNode(o.getClass());
        }

        beforeMap.forEach((before, afterList) -> {
            final Integer beforeID = sorter.getNodeID(before.getClass());
            if (beforeID != null) {
                afterList.forEach(afterClass -> {
                    sorter.addNode(afterClass);
                    final Integer afterID = sorter.getNodeID(afterClass);
                    sorter.addEdge(afterID,beforeID);
                });
            }
        });

        final int[] newOrder = sorter.doTopoSort();
        if(newOrder == null){
            throw new MCRException("Circular dependencies in EventHandlers!");
        }
        for (final int nodeID : newOrder) {
            final Class nodeClass = sorter.getNodeName(nodeID);
            if (classObjectMap.containsKey(nodeClass)) {
                final Object o = classObjectMap.get(nodeClass);
                sortedEventHandler.add(o);
            }

        }
    }

    public void trigger(MCREvent event) {
        this.sortedEventHandler.forEach(handler -> {
            final List<Method> methods = this.objectMethodMap.get(handler);
            for (Method method : methods) {
                if (method.getParameterTypes()[0].isAssignableFrom(event.getClass())) {
                    try {
                        method.invoke(handler, event);
                        break;
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}
