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
    private static final String TAB = "    ";
    private final Class<?> clazz;
    private final String newClassName;
    private final List<Method> methodsToOverride;
    //{DeclaringClassName -> {NameOfTypeVariable -> RealType}}
    private final Map<String, Map<String, Type>> genericNamesTranslation;
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
        this.genericNamesTranslation = createGenericNamesTranslation();
    }

    private static void initGenericNamesTranslation(Map<String, Type> passedParams, Map<String, Map<String, Type>>
            genericNamesTranslation, Class<?> clazz) {
        Map<String, Type> realNameByCurName = new HashMap<>();
        for (TypeVariable<?> variable : clazz.getTypeParameters()) {
            Type realValue = passedParams.get(variable.getName());
            realNameByCurName.put(variable.getName(), realValue);
        }
        genericNamesTranslation.put(clazz.getCanonicalName(), realNameByCurName);

        for (Type type : clazz.getGenericInterfaces()) {
            parseClassParent(type, genericNamesTranslation);
        }
        parseClassParent(clazz.getGenericSuperclass(), genericNamesTranslation);
    }

    private static void parseClassParent(Type parent, Map<String, Map<String, Type>> genericNamesTranslation) {
        if (parent instanceof ParameterizedType) {
            Map<String, Type> parentArguments = new HashMap<>();
            ParameterizedType parameterizedParent = (ParameterizedType) parent;
            Class<?> parentClass = (Class<?>) parameterizedParent.getRawType();
            Type[] actualTypeArguments = parameterizedParent.getActualTypeArguments();
            for (int i = 0; i < actualTypeArguments.length; i++) {
                parentArguments.put(parentClass.getTypeParameters()[i].getName(), actualTypeArguments[i]);
            }
            initGenericNamesTranslation(parentArguments, genericNamesTranslation, parentClass);
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

    private static String getDefaultValue(Type type) {
        if (type instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type;
            if (clazz.isPrimitive()) {
                return clazz.equals(Boolean.TYPE) ? "false" : "0";
            } else {
                return "null";
            }
        } else {
            return "null";
        }
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

    private Map<String, Map<String, Type>> createGenericNamesTranslation() {
        Map<String, Map<String, Type>> genericNamesTranslation = new HashMap<>(); //Answer
        Map<String, Type> passedParams = new HashMap<>();
        for (TypeVariable<?> variable : clazz.getTypeParameters()) {
            passedParams.put(variable.getName(), variable);
        }
        initGenericNamesTranslation(passedParams, genericNamesTranslation, clazz);
        return genericNamesTranslation;
    }

    private void addImport(Map<String, Class<?>> toImport, Type type) {
        if (type instanceof ParameterizedType) {
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
                addImport(toImport, clazz.getComponentType());
            } else if (!clazz.isPrimitive() && !clazz.getPackage().equals(Package.getPackage("java.lang")) && !clazz
                    .getPackage().equals(this.clazz.getPackage())) {
                toImport.put(clazz.getName(), clazz);
            }
        } else if (type instanceof TypeVariable) {
            //TODO: do nothing?
        } else {
            throw new IllegalStateException();
        }
    }

    private Map<String, Class<?>> getImports() {
        Map<String, Class<?>> toImport = new HashMap<>();
        TypeVariable<? extends Class<?>>[] typeVariables = clazz.getTypeParameters();
        for (TypeVariable<?> typeVariable : typeVariables) {
            for (Type type : typeVariable.getBounds()) {
                addImport(toImport, type);
            }
        }

        for (Constructor<?> constructor : clazz.getConstructors()) {
            for (Type type : constructor.getParameterTypes()) {
                addImport(toImport, type);
            }

            for (Type type : constructor.getGenericExceptionTypes()) {
                addImport(toImport, type);
            }
        }

        for (Method method : methodsToOverride) {
            for (TypeVariable typeVariable : method.getTypeParameters()) {
                for (Type bound : typeVariable.getBounds()) {
                    addImport(toImport, bound);
                }
            }
            for (Type type : method.getGenericParameterTypes()) {
                addImport(toImport, type);
            }
            addImport(toImport, method.getGenericReturnType());
        }
        return toImport;
    }

    private void writePackage(PrintWriter writer) throws IOException {
        writer.println("package " + clazz.getPackage().getName() + ";");
        writer.println();
    }

    private void writeImports(PrintWriter writer) throws IOException {
        Map<String, Class<?>> imports = getImports();
        for (Map.Entry<String, Class<?>> entry : imports.entrySet()) {
            writer.println("import " + entry.getValue().getName() + ";");
        }
        writer.println();
    }

    private void writeClassDeclaration(PrintWriter writer) throws IOException {
        writer.print("class " + newClassName + generateGenericParamsPrefix(clazz.getTypeParameters()));
        writer.print(clazz.isInterface() ? " implements " : " extends ");
        writer.println(clazz.getSimpleName() + generateGenericParamsSuffix(clazz.getTypeParameters()) + " {");
    }

    private String generateGenericParamsPrefix(TypeVariable<?>[] genericArguments) {
        Set<String> setOfGenericParamsNames = new HashSet<>();
        for (TypeVariable<?> typeVariable : genericArguments) {
            setOfGenericParamsNames.add(typeVariable.getName());
        }
        StringBuilder result = new StringBuilder();
        if (genericArguments.length != 0) {
            result.append("<");
            for (int i = 0; i < genericArguments.length; i++) {
                TypeVariable<?> arg = genericArguments[i];
                result.append(getName(arg, genericNamesTranslation, setOfGenericParamsNames));
                Type[] bounds = arg.getBounds();
                if (bounds.length > 0 && !bounds[0].equals(Object.class)) {
                    result.append(" extends ");
                    for (int j = 0; j < bounds.length; j++) {
                        result.append(toStringGenericTypedClass(bounds[j], genericNamesTranslation,
                                setOfGenericParamsNames));
                        if (j != bounds.length - 1) {
                            result.append(" & ");
                        }
                    }
                }
                if (i != genericArguments.length - 1) {
                    result.append(", ");
                } else {
                    result.append(">");
                }
            }
        }
        return result.toString();
    }

    private String generateGenericParamsSuffix(TypeVariable<?>[] genericArguments) {
        Set<String> setOfGenericParamsNames = new HashSet<>();
        for (TypeVariable<?> var : genericArguments) {
            setOfGenericParamsNames.add(var.getName());
        }
        String result = "";
        if (genericArguments.length != 0) {
            result = "<";
            for (int i = 0; i < genericArguments.length; i++) {
                TypeVariable<?> arg = genericArguments[i];
                result += getName(arg, genericNamesTranslation, setOfGenericParamsNames);
                if (i != genericArguments.length - 1) {
                    result += ", ";
                } else {
                    result += ">";
                }
            }
        }
        return result;
    }

    private String getName(TypeVariable<?> type, Map<String, Map<String, Type>> genericNamesTranslation, Set<String>
            exclusionNames) {
        if (exclusionNames != null && exclusionNames.contains(type.getName())) {
            return type.getName();
        }
        Class<?> classOfDeclaration;
        if (type.getGenericDeclaration() instanceof Method) {
            classOfDeclaration = ((Method) type.getGenericDeclaration()).getDeclaringClass();
        } else if (type.getGenericDeclaration() instanceof Class<?>) {
            classOfDeclaration = ((Class) type.getGenericDeclaration());
        } else if (type.getGenericDeclaration() instanceof Constructor<?>) {
            classOfDeclaration = ((Constructor) type.getGenericDeclaration()).getDeclaringClass();
        } else {
            throw new IllegalStateException();
        }
        Map<String, Type> map = genericNamesTranslation.get(classOfDeclaration.getCanonicalName());
        if (map == null) {
            return "Object";
        }
        Type realType = map.get(type.getName());
        if (realType instanceof TypeVariable<?>) {
            return ((TypeVariable) realType).getName();
        } else {
            return toStringGenericTypedClass(realType, genericNamesTranslation, null);
        }
    }

    private String toStringGenericTypedClass(Type type, Map<String, Map<String, Type>> genericNamesTranslation,
                                             Set<String> exclusionNames) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            String result = ((Class<?>) parameterizedType.getRawType()).getSimpleName() + "<";
            Type[] args = parameterizedType.getActualTypeArguments();
            for (int i = 0; i < args.length; i++) {
                result += toStringGenericTypedClass(args[i], genericNamesTranslation, exclusionNames);
                if (i != args.length - 1) {
                    result += ", ";
                } else {
                    result += ">";
                }
            }
            return result;
        } else if (type instanceof TypeVariable) {
            return getName(((TypeVariable) type), genericNamesTranslation, exclusionNames);
        } else if (type instanceof GenericArrayType) {
            return toStringGenericTypedClass(((GenericArrayType) type).getGenericComponentType(),
                    genericNamesTranslation, exclusionNames) + "[]";
        } else if (type instanceof Class) {
            return ((Class) type).getSimpleName();
        } else if (type instanceof WildcardType) {
            return formatWildcard((WildcardType) type, genericNamesTranslation);
        } else {
            throw new IllegalStateException();
        }
    }

    private String formatWildcard(WildcardType type, Map<String, Map<String, Type>> genericNamesTranslation) {
        //TODO: more general?
        if (type.getLowerBounds().length != 0) {
            if (type.getUpperBounds().length != 1 || !type.getUpperBounds()[0].equals(Object.class)) {
                throw new IllegalStateException();
            }
            String result = "? super ";
            Type[] bounds = type.getLowerBounds();
            for (int i = 0; i < bounds.length; i++) {
                Type bound = bounds[i];
                result += toStringGenericTypedClass(bound, genericNamesTranslation, null);
                if (i != bounds.length - 1) {
                    result += ", ";
                }
            }
            return result;
        } else if (type.getUpperBounds().length != 0 && !type.getUpperBounds()[0].equals(Object.class)) {
            String result = "? extends ";
            Type[] bounds = type.getUpperBounds();
            for (int i = 0; i < bounds.length; i++) {
                Type bound = bounds[i];
                result += toStringGenericTypedClass(bound, genericNamesTranslation, null);
                if (i != bounds.length - 1) {
                    result += ", ";
                }
            }
            return result;
        } else {
            return "?";
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
                //Check for Object (don't need upperBound from Object)
                //TODO: better way? (maybe bugged)
                if (t instanceof Class) {
                    Class<?> classType = (Class<?>) t;
                    if (classType.equals(Object.class)) {
                        continue;
                    }
                }
                sb.append(" extends ").append(getGenericSimpleName(t));
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

            //TODO: check?
            String genericArgumentsPrefix = generateGenericParamsPrefix(method.getTypeParameters());
            if (!genericArgumentsPrefix.isEmpty()) {
                writer.print(genericArgumentsPrefix + " ");
            }

            Set<String> set = getSetOfNames(method.getTypeParameters());
            writer.print(toStringGenericTypedClass(method.getGenericReturnType(), genericNamesTranslation, set) + " ");
            writer.print(method.getName() + "(");
            Parameter[] parameters = method.getParameters();
            Type[] genericParameterTypes = method.getGenericParameterTypes();
            for (int i = 0; i < parameters.length; i++) {
                writer.print(toStringGenericTypedClass(genericParameterTypes[i], genericNamesTranslation, set));
                writer.print(" " + parameters[i].getName());
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
            Type[] genericParameterTypes = constructor.getGenericParameterTypes();
            Parameter[] parameters = constructor.getParameters();
            for (int i = 0; i < genericParameterTypes.length; i++) {
                writer.print(getGenericSimpleName(genericParameterTypes[i]) + " " + parameters[i].getName());
                if (i != genericParameterTypes.length - 1) {
                    writer.print(", ");
                }
            }
            writer.print(")");
            Type[] exceptions = constructor.getGenericExceptionTypes();
            if (exceptions.length > 0) {
                writer.print(" throws ");
                for (int i = 0; i < exceptions.length; i++) {
                    writer.print(toStringGenericTypedClass(exceptions[i], genericNamesTranslation, new HashSet<>()));
                    if (i != exceptions.length - 1) {
                        writer.print(", ");
                    }
                }
            }
            writer.println(" {");
            writer.print(TAB + TAB + "super(");
            for (int i = 0; i < genericParameterTypes.length; i++) {
                writer.print(parameters[i].getName());
                if (i != genericParameterTypes.length - 1) {
                    writer.print(", ");
                }
            }
            writer.println(");");
            writer.println(TAB + "}");
        }
    }

    private Set<String> getSetOfNames(TypeVariable<?>[] typeParameters) {
        Set<String> names = new HashSet<>();
        for (TypeVariable<?> var : typeParameters) {
            names.add(var.getName());
        }
        return names;
    }

    public void implement(PrintWriter writer) throws IOException {
        writePackage(writer);
        writeImports(writer);
        writeClassDeclaration(writer);
        writeConstructors(writer);
        writeMethodImplementations(writer);
        writer.write("}");
    }

    private static class WowSuchMethod {
        private final Method method;

        public WowSuchMethod(Method method) {
            this.method = method;
        }

        private static boolean differsInTypeVariable(Type thisType, Type thatType) {
            if (thisType instanceof ParameterizedType) {
                //hehlatex
                return false;
            } else if (thisType instanceof GenericArrayType) {
                if (thatType instanceof GenericArrayType) {
                    GenericArrayType thisGeneric = (GenericArrayType) thisType;
                    GenericArrayType thatGeneric = (GenericArrayType) thatType;
                    return differsInTypeVariable(thisGeneric.getGenericComponentType(), thatGeneric
                            .getGenericComponentType());
                } else {
                    //hehlatex
                    return false;
                }
            } else if (thisType instanceof Class) {
                Class thisClass = (Class) thisType;
                if (thisClass.isArray()) {
                    if (thatType instanceof Class) {
                        Class thatClass = (Class) thatType;
                        if (thatClass.isArray()) {
                            return differsInTypeVariable(thisClass.getComponentType(), thatClass.getComponentType());
                        } else {
                            //hehlatex
                            return false;
                        }
                    } else if (thatType instanceof GenericArrayType) {
                        GenericArrayType thatGeneric = (GenericArrayType) thatType;
                        return differsInTypeVariable(thisClass.getComponentType(), thatGeneric
                                .getGenericComponentType());
                    } else {
                        //hehlatex
                        return false;
                    }
                } else if (thisClass.equals(Object.class)) {
                    //hehlatex
                    return true;
                } else {
                    //hehlatex
                    return false;
                }
            } else if (thisType instanceof WildcardType) {
                //hehlatex
                return false;
            } else if (thisType instanceof TypeVariable) {
                return thatType.equals(Object.class);
            } else {
                throw new IllegalStateException();
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            WowSuchMethod that = (WowSuchMethod) o;

            if (!method.getName().equals(that.method.getName())) {
                return false;
            }

            Class<?>[] parameterTypes = method.getParameterTypes();
            Class<?>[] thatParameterTypes = that.method.getParameterTypes();
            Type[] genericParams = method.getGenericParameterTypes();
            Type[] thatGenericParams = that.method.getGenericParameterTypes();

            if (parameterTypes.length != thatParameterTypes.length) {
                return false;
            }

            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> thisT = parameterTypes[i];
                Class<?> thatT = thatParameterTypes[i];
                if (!thisT.equals(thatT)) {
                    Type thisType = genericParams[i];
                    Type thatType = thatGenericParams[i];
                    if (differsInTypeVariable(thisType, thatType)) {
                        continue;
                    }
                    return false;
                }
            }

            return true;
        }

        @Override
        public int hashCode() {
            return method.getName().hashCode();
        }

        public Method getMethod() {
            return method;
        }
    }
}
