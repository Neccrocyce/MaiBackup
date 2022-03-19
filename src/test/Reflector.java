package test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.fail;

public class Reflector {
    /**
     * Sets the value of a variable of an object {@code obj} with private access
     * @param obj The object of which a variable should be changed
     * @param var The variable that should be changed
     * @param value The value the variable should get
     */
    public static void setField (Object obj, String var, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = obj.getClass().getDeclaredField(var);
        field.setAccessible(true);
        field.set(obj,value);
    }

    /**
     * Sets the value of a static variable of a class {@code clas} with private access
     * @param clas The class of which a static variable should be changed
     * @param var The variable that should be changed
     * @param value The value the variable should get
     */
    public static void setStaticField (Class clas, String var, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = clas.getDeclaredField(var);
        field.setAccessible(true);
        field.set(null,value);
    }

    public static Object getStaticField (Class clas, String var) throws NoSuchFieldException, IllegalAccessException {
        Field field = clas.getDeclaredField(var);
        field.setAccessible(true);
        return field.get(var);
    }

    /**
     * This method calls a method with private access and the name {@code name} and hand over the arguments {@code args}
     * @param obj The object of which the method should be called
     * @param method The name of the method which should be called
     * @param args The arguments which should handed over
     * @return the return of the called method
     * @throws InvocationTargetException
     */
    public static Object callMethod (Object obj, String method, Object... args) throws InvocationTargetException {
        List<Class> argTypes = new ArrayList<>();
        for (Object arg : args) {
            argTypes.add(arg.getClass());
        }
        try {
            Method m = obj.getClass().getDeclaredMethod(method, argTypes.toArray(new Class[argTypes.size()]));
            m.setAccessible(true);
            return m.invoke(obj,args);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * This method calls a method with private access and the name {@code name} and hand over the arguments {@code args}
     * It takes one of the methods with the given name. It doesn't compare the type of the given arguments with these of the took method.
     * - if there are more methods with this name, this method will not work. In this case use callMethod (Object, String, Object...)
     * @param obj
     * @param method
     * @param args
     * @see #callMethod(Object, String, Object...)
     * @return
     * @throws InvocationTargetException
     */
    public static Object callMethod2 (Object obj, String method, Object... args) throws InvocationTargetException {
        Method[] methods = obj.getClass().getDeclaredMethods();
        for (Method m : methods) {
            if (m.getName().equals(method)) {
                m.setAccessible(true);
                try {
                    return m.invoke(obj,args);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }
        return null;
    }
}
