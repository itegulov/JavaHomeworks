package ru.ifmo.ctddev.itegulov.implementor;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.*;
import java.util.*;

/**
 * @author Daniyar Itegulov
 * @since 27.02.15
 */
public class Implementor {
    private static class WowSuchMethod {
        private final Method method;
        private final String methodName;
        private final Type[] types;
        private final Type returnType;

        public WowSuchMethod(Method method) {
            this.method = method;
            this.methodName = method.getName();
            this.types = method.getGenericParameterTypes();
            this.returnType = method.getGenericReturnType();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            WowSuchMethod that = (WowSuchMethod) o;

            return methodName.equals(that.methodName) && returnType.equals(that.returnType) && Arrays.equals(types, that.types);
        }

        @Override
        public int hashCode() {
            int result = methodName.hashCode();
            result = 31 * result + Arrays.hashCode(types);
            result = 31 * result + returnType.hashCode();
            return result;
        }

        public Method getMethod() {
            return method;
        }
    }
    
    private static final String TAB = "    ";

    private final Class clazz;
    private final String newClassName;
    private final List<Method> methodsToOverride;

    public Implementor(Class clazz, String newClassName) {
        //TODO: don't support some stupid type of classes (primitives?)
        this.clazz = clazz;
        this.newClassName = newClassName;
        Set<WowSuchMethod> set = new HashSet<>();
        getMethodsToOverride(set, clazz);
        this.methodsToOverride = new ArrayList<>();
        for (WowSuchMethod wowSuchMethod : set) {
            methodsToOverride.add(wowSuchMethod.getMethod());
        }
    }

    private static void getMethodsToOverride(Set<WowSuchMethod> set, Class<?> classToSearch) {
        for (Method method : classToSearch.getDeclaredMethods()) {
            if (Modifier.isAbstract(method.getModifiers())) {
                set.add(new WowSuchMethod(method));
            }
        }
        if (classToSearch.getSuperclass() != null && !classToSearch.getSuperclass().equals(Object.class)) {
            getMethodsToOverride(set, classToSearch.getSuperclass());
        }
        for (Class<?> anInterface : classToSearch.getInterfaces()) {
            getMethodsToOverride(set, anInterface);
        }
    }

    private static void addImport(Map<String, Class> toImport, Type type) {
        if (type instanceof TypeVariable) {
            throw new IllegalArgumentException();
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            addImport(toImport, parameterizedType.getRawType());
            for (Type t : parameterizedType.getActualTypeArguments()) {
                addImport(toImport, t);
            }
        } else if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            addImport(toImport, genericArrayType.getGenericComponentType());
        } else if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            for (Type t : wildcardType.getLowerBounds()) {
                addImport(toImport, t);
            }

            for (Type t : wildcardType.getUpperBounds()) {
                addImport(toImport, t);
            }
        } else if (type instanceof Class) {
            Class<?> clazz = (Class<?>) type;
            if (clazz.isArray()) {
                Class<?> component = clazz.getComponentType();
                addImport(toImport, component);
            } else {
                if (!clazz.isPrimitive()) {
                    if (!clazz.getPackage().equals(Package.getPackage("java.lang"))) {
                        toImport.put(clazz.getName(), clazz);
                    }
                }
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    private Map<String, Class> getImports() {
        Map<String, Class> toImport = new HashMap<>();
        for (Method method : methodsToOverride) {
            for (Parameter parameter : method.getParameters()) {
                addImport(toImport, parameter.getParameterizedType());
            }
            addImport(toImport, method.getGenericReturnType());
        }
        
        for (Constructor<?> constructor : clazz.getConstructors()) {
            for (Parameter parameter : constructor.getParameters()) {
                addImport(toImport, parameter.getParameterizedType());
            }
            
            for (Class<?> type : constructor.getExceptionTypes()) {
                addImport(toImport, type);
            }
        }
        return toImport;
    }

    private void writePackage(PrintWriter writer) throws IOException {
        writer.println("package " + clazz.getPackage().getName() + ";");
    }

    private void writeImports(PrintWriter writer) throws IOException {
        Map<String, Class> imports = getImports();
        for (Map.Entry<String, Class> entry : imports.entrySet()) {
            writer.println("import " + entry.getValue().getName() + ";");
        }
    }

    private void writeClassDeclaration(PrintWriter writer) throws IOException {
        writer.print("class " + newClassName);
        writer.print(clazz.isInterface() ? " implements " : " extends ");
        writer.println(clazz.getSimpleName() + " {");
    }

    private static String getDefaultValue(Class clazz) {
        if (clazz.isPrimitive()) {
            return clazz.equals(Boolean.TYPE) ? "false" : "0";
        } else {
            return "null";
        }
    }

    private String getGenericSimpleName(Type type) {
        if (type instanceof TypeVariable) {
            throw new IllegalArgumentException();
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type[] arguments = parameterizedType.getActualTypeArguments();
            StringBuilder sb = new StringBuilder();
            sb.append(((Class<?>) parameterizedType.getRawType()).getSimpleName());
            sb.append("<");
            for (int i = 0; i < arguments.length; i++) {
                sb.append(getGenericSimpleName(arguments[i]));
                if (i != arguments.length - 1) {
                    sb.append(", ");
                }
            }
            sb.append(">");
            return sb.toString();
        } else if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            Type component = genericArrayType.getGenericComponentType();
            return getGenericSimpleName(component) + "[]";
        } else if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            StringBuilder sb = new StringBuilder();
            sb.append("?");
            for (Type t : wildcardType.getLowerBounds()) {
                sb.append(" super ").append(getGenericSimpleName(t));
            }

            for (Type t : wildcardType.getUpperBounds()) {
                //TODO: check better
                if (!t.getTypeName().equals("java.lang.Object")) {
                    sb.append(" extends ").append(getGenericSimpleName(t));
                }
            }
            return sb.toString();
        } else if (type instanceof Class) {
            return ((Class<?>) type).getSimpleName();
        } else {
            throw new IllegalArgumentException();
        }
    }

    private void writeMethodImplementations(PrintWriter writer) throws IOException {
        for (Method method : methodsToOverride) {
            writer.println(TAB + "@Override");
            if (Modifier.isProtected(method.getModifiers())) {
                writer.print(TAB + "protected ");
            } else if (Modifier.isPublic(method.getModifiers())) {
                writer.print(TAB + "public ");
            } else if (Modifier.isPrivate(method.getModifiers())) {
                throw new IllegalArgumentException("Don't support overriding private methods");
            } else {
                //Package-local
                writer.print(TAB);
            }
            writer.print(getGenericSimpleName(method.getGenericReturnType()) + " ");
            writer.print(method.getName() + "(");
            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                writer.print(parameters[i].getType().getSimpleName() + " " + parameters[i].getName());
                if (i != parameters.length - 1) {
                    writer.print(", ");
                }
            }
            writer.println(") {");
            if (!method.getReturnType().equals(Void.TYPE)) {
                writer.println(TAB + TAB + "return " + getDefaultValue(method.getReturnType()) + ";");
            }
            writer.println(TAB + "}");
            writer.println();
        }
    }

    private void writeConstructors(PrintWriter writer) {
        for (Constructor<?> constructor : clazz.getConstructors()) {
            writer.print(TAB + "public " + newClassName);
            writer.print("(");
            Parameter[] parameters = constructor.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                writer.print(parameters[i].getType().getSimpleName() + " " + parameters[i].getName());
                if (i != parameters.length - 1) {
                    writer.print(", ");
                }
            }
            writer.print(")");
            Class<?>[] exceptions = constructor.getExceptionTypes();
            if (exceptions.length > 0) {
                writer.print(" throws ");
                for (int i = 0; i < exceptions.length; i++) {
                    writer.print(exceptions[i].getSimpleName());
                    if (i != exceptions.length - 1) {
                        writer.print(", ");
                    }
                }
            }
            writer.println(" {");
            writer.print(TAB + TAB + "super(");
            for (int i = 0; i < parameters.length; i++) {
                writer.print(parameters[i].getName());
                if (i != parameters.length - 1) {
                    writer.print(", ");
                }
            }
            writer.println(");");
            writer.println(TAB + "}");
        }
    }

    public void implement(PrintWriter writer) throws IOException {
        writePackage(writer);
        writer.println();
        writeImports(writer);
        writer.println();
        writeClassDeclaration(writer);
        writeConstructors(writer);
        writer.println();
        writeMethodImplementations(writer);
        writer.write("}");
    }

    public static void main(String[] args) {
        if (args == null || args.length != 1 || args[0] == null) {
            System.err.println("Usage: java Implementor [className]");
            return;
        }

        try {
            Class clazz = Class.forName(args[0]);
            Implementor implementor = new Implementor(clazz, clazz.getSimpleName() + "Impl");
            PrintWriter printWriter = new PrintWriter(System.out);
            try {
                implementor.implement(printWriter);
            } catch (IOException e) {
                System.err.println("Error writing new java class");
            }
            printWriter.close();
        } catch (ClassNotFoundException e) {
            System.err.println("Class " + args[0] + " not found");
        }
    }
}
