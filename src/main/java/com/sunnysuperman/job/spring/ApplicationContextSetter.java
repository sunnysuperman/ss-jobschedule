package com.sunnysuperman.job.spring;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;

public class ApplicationContextSetter {

    private static List<Class<?>> getClassesIterators(Class<?> clazz) {
        List<Class<?>> classes = new ArrayList<>();
        classes.add(clazz);
        while (true) {
            Class<?> superClass = clazz.getSuperclass();
            if (superClass == null || superClass == Object.class) {
                break;
            }
            classes.add(superClass);
            clazz = superClass;
        }
        return classes;
    }

    public static void set(Object object, ApplicationContext applicationContext) {
        List<Class<?>> classes = getClassesIterators(object.getClass());
        for (Class<?> clazz : classes) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (field.getAnnotation(Autowired.class) == null) {
                    continue;
                }
                Object value;
                Qualifier qualifier = field.getAnnotation(Qualifier.class);
                if (qualifier != null && qualifier.value() != null && qualifier.value().length() > 0) {
                    value = applicationContext.getBean(qualifier.value());
                } else {
                    value = applicationContext.getBean(field.getType());
                }
                try {
                    field.setAccessible(true);
                    field.set(object, value);
                } catch (IllegalArgumentException e) {
                    throw e;
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
