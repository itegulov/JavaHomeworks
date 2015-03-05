package ru.ifmo.ctddev.itegulov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.*;
import java.util.*;

/**
 * @author Daniyar Itegulov
 * @since 05.03.15
 */
public class ImplementHelper {
    private static final String TAB = "    ";

    private final Class<?> clazz;
    private final String newClassName;
    private final List<Method> methodsToOverride;
    private final List<Constructor> constructorsToSuper;
    //{DeclaringClassName -> {NameOfTypeVariable -> RealType}}
    private final Map<String, Map<String, Type>> genericNamesTranslation;
    private final Map<String, Class<?>> imports;

    public ImplementHelper(Class clazz, String newClassName) throws ImplerException {
        if (clazz.isPrimitive()) {
            throw new ImplerException("Can't implement primitives");
        }

        if (clazz.isArray()) {
            throw new ImplerException("Can't implement arrays");
        }

        if (Modifier.isFinal(clazz.getModifiers())) {
            throw new ImplerException("Can't implement final class");
        }
        
        this.clazz = clazz;
        this.newClassName = newClassName;
        Map<MethodSignature, MethodSignature> map = new HashMap<>();
        getMethodsToOverride(map, new HashMap<>(), clazz);
        this.methodsToOverride = new ArrayList<>();
        for (MethodSignature methodSignature : map.values()) {
            this.methodsToOverride.add(methodSignature.getMethod());
        }
        this.constructorsToSuper = getConstructorsToSuper();
        this.genericNamesTranslation = createGenericNamesTranslation();
        this.imports = getImports();
    }

    private List<Constructor> getConstructorsToSuper() {
        List<Constructor> constructors = new ArrayList<>();
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (Modifier.isPrivate(constructor.getModifiers())) {
                continue;
            }
            constructors.add(constructor);
        }
        return constructors;
    }

    private static void initGenericNamesTranslation(Map<String, Type> passedParams,
                                                    Map<String, Map<String, Type>> genericNamesTranslation,
                                                    Class<?> clazz) {
        Map<String, Type> realNameByCurName = new HashMap<>();
        Map<String, Type> oldPassedParams = passedParams;
        for (TypeVariable<?> variable : clazz.getTypeParameters()) {
            Type realValue = variable;
            while (passedParams.containsKey(realValue.getTypeName()) 
                   && !passedParams.get(realValue.getTypeName()).equals(realValue)) {
                realValue = passedParams.get(realValue.getTypeName());
                for (Map.Entry<String, Map<String, Type>> map : genericNamesTranslation.entrySet()) {
                    Map<String, Type> translation = map.getValue();
                    if (translation.containsKey(realValue.getTypeName())) {
                        passedParams = translation;
                    }
                }
            }
            passedParams = oldPassedParams;
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

    private static boolean isGeneric(Type type) {
        if (type instanceof ParameterizedType) {
            return true;
        } else if (type instanceof GenericArrayType) {
            return true;
        } else if (type instanceof Class) {
            return false;
        } else if (type instanceof WildcardType) {
            return true;
        } else if (type instanceof TypeVariable) {
            return true;
        } else {
            throw new IllegalStateException();
        }
    }

    private static void addMethod(Map<MethodSignature, MethodSignature> map, Method method) throws ImplerException {
        MethodSignature signature = new MethodSignature(method);
        MethodSignature oldSignature = map.get(signature);
        if (oldSignature == null) {
            map.put(signature, signature);
        } else if (oldSignature.equals(signature)) {
            Method oldMethod = oldSignature.getMethod();
            if (!oldMethod.getReturnType().equals(method.getReturnType())
                    && oldMethod.getReturnType().isAssignableFrom(method.getReturnType())) {
                map.put(signature, signature);
            } else {
                Type[] parameterTypes = method.getGenericParameterTypes();
                for (int i = 0; i < parameterTypes.length; i++) {
                    Class<?> thisP = method.getParameterTypes()[i];
                    Class<?> oldP = oldMethod.getParameterTypes()[i];
                    if (!thisP.equals(oldP)) {
                        Type newGP = parameterTypes[i];
                        if (isGeneric(newGP)) {
                            map.put(signature, signature);
                            break;
                        }
                    }
                }
            }
        }
    }

    private static void getMethodsToOverride(Map<MethodSignature, MethodSignature> toImplement,
                                             Map<MethodSignature, MethodSignature> implemented,
                                             Class<?> classToSearch) throws ImplerException {
        for (Method method : classToSearch.getDeclaredMethods()) {
            if (Modifier.isAbstract(method.getModifiers() & Modifier.methodModifiers())) {
                if (!implemented.containsKey(new MethodSignature(method))
                    && !Modifier.isPrivate(method.getModifiers()) && !Modifier.isFinal(method.getModifiers())) {
                    addMethod(toImplement, method);
                }
            } else {
                addMethod(implemented, method);
            }
        }
        
        for (Method method : classToSearch.getMethods()) {
            if (Modifier.isAbstract(method.getModifiers())) {
                if (!implemented.containsKey(new MethodSignature(method))
                    && !Modifier.isFinal(method.getModifiers())) {
                    addMethod(toImplement, method);
                }
            } else {
                addMethod(implemented, method);
            }
        }
        Class<?> superclass = classToSearch.getSuperclass();
        if (superclass != null) {
            getMethodsToOverride(toImplement, implemented, superclass);
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
            } else if (!clazz.isPrimitive() && !clazz.getPackage().equals(Package.getPackage("java.lang")) 
                    && !clazz.getPackage().equals(this.clazz.getPackage()) && !clazz.isLocalClass() && !clazz.isMemberClass()) {
                toImport.put(clazz.getSimpleName(), clazz);
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

        for (Constructor<?> constructor : constructorsToSuper) {
            for (Type type : constructor.getGenericParameterTypes()) {
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
        if (clazz.getPackage() != null) {
            writer.println("package " + clazz.getPackage().getName() + ";");
            writer.println();
        }
    }

    private void writeImports(PrintWriter writer) throws IOException {
        for (Map.Entry<String, Class<?>> entry : imports.entrySet()) {
            writer.println("import " + entry.getValue().getName() + ";");
        }
        writer.println();
    }

    private void writeClassDeclaration(PrintWriter writer) throws IOException {
        String modifiers;
        if (clazz.isInterface()) {
            modifiers = Modifier.toString(clazz.getModifiers() & Modifier.interfaceModifiers() & ~Modifier.ABSTRACT);
        } else {
            modifiers = Modifier.toString(clazz.getModifiers() & Modifier.classModifiers() & ~Modifier.ABSTRACT);
        }
        if (!modifiers.equals("")) {
            modifiers += " ";
        }
        writer.print(modifiers);
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
                result.append(getName(arg, setOfGenericParamsNames));
                Type[] bounds = arg.getBounds();
                if (bounds.length > 0 && !bounds[0].equals(Object.class)) {
                    result.append(" extends ");
                    for (int j = 0; j < bounds.length; j++) {
                        result.append(toStringGenericTypedClass(bounds[j], setOfGenericParamsNames));
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
        StringBuilder result = new StringBuilder();
        if (genericArguments.length != 0) {
            result.append("<");
            for (int i = 0; i < genericArguments.length; i++) {
                TypeVariable<?> arg = genericArguments[i];
                result.append(getName(arg, setOfGenericParamsNames));
                if (i != genericArguments.length - 1) {
                    result.append(", ");
                } else {
                    result.append(">");
                }
            }
        }
        return result.toString();
    }

    private String getName(TypeVariable<?> type, Set<String> exclusionNames) {
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
            return toStringGenericTypedClass(realType, null);
        }
    }

    private String toStringGenericTypedClass(Type type, Set<String> exclusionNames) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            StringBuilder result = new StringBuilder(((Class<?>) parameterizedType.getRawType()).getSimpleName());
            result.append("<");
            Type[] args = parameterizedType.getActualTypeArguments();
            for (int i = 0; i < args.length; i++) {
                result.append(toStringGenericTypedClass(args[i], exclusionNames));
                if (i != args.length - 1) {
                    result.append(", ");
                } else {
                    result.append(">");
                }
            }
            return result.toString();
        } else if (type instanceof TypeVariable) {
            return getName(((TypeVariable) type), exclusionNames);
        } else if (type instanceof GenericArrayType) {
            return toStringGenericTypedClass(((GenericArrayType) type).getGenericComponentType(), exclusionNames) + "[]";
        } else if (type instanceof Class) {
            Class<?> clazz = (Class<?>) type;
            if (imports.containsKey(clazz.getSimpleName()) && !imports.get(clazz.getSimpleName()).equals(clazz)) {
                return clazz.getName();
            } else {
                return clazz.getSimpleName();
            }
        } else if (type instanceof WildcardType) {
            return formatWildcard((WildcardType) type, exclusionNames);
        } else {
            throw new IllegalStateException();
        }
    }

    private String formatWildcard(WildcardType type, Set<String> exclusionNames) {
        //TODO: more general?
        if (type.getLowerBounds().length != 0) {
            if (type.getUpperBounds().length != 1 || !type.getUpperBounds()[0].equals(Object.class)) {
                throw new IllegalStateException();
            }
            StringBuilder result = new StringBuilder("? super ");
            Type[] bounds = type.getLowerBounds();
            for (int i = 0; i < bounds.length; i++) {
                Type bound = bounds[i];
                result.append(toStringGenericTypedClass(bound, exclusionNames));
                if (i != bounds.length - 1) {
                    result.append(", ");
                }
            }
            return result.toString();
        } else if (type.getUpperBounds().length != 0 && !type.getUpperBounds()[0].equals(Object.class)) {
            StringBuilder result = new StringBuilder("? extends ");
            Type[] bounds = type.getUpperBounds();
            for (int i = 0; i < bounds.length; i++) {
                Type bound = bounds[i];
                result.append(toStringGenericTypedClass(bound, exclusionNames));
                if (i != bounds.length - 1) {
                    result.append(", ");
                }
            }
            return result.toString();
        } else {
            return "?";
        }
    }
    
    private void writeMethodImplementations(PrintWriter writer) throws IOException {
        for (Method method : methodsToOverride) {
            if (method.getAnnotation(Deprecated.class) != null) {
                writer.println(TAB + "@Deprecated");
            }
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
            writer.print(toStringGenericTypedClass(method.getGenericReturnType(), set) + " ");
            writer.print(method.getName() + "(");
            Parameter[] parameters = method.getParameters();
            Type[] genericParameterTypes = method.getGenericParameterTypes();
            for (int i = 0; i < parameters.length; i++) {
                writer.print(toStringGenericTypedClass(genericParameterTypes[i], set));
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

    private void writeConstructors(PrintWriter writer) throws ImplerException {
        for (Constructor<?> constructor : constructorsToSuper) {
            if (Modifier.isPublic(constructor.getModifiers())) {
                writer.print(TAB + "public " + newClassName);
            } else if (Modifier.isProtected(constructor.getModifiers())) {
                writer.print(TAB + "protected " + newClassName);
            } else {
                writer.print(TAB + newClassName);
            }
            writer.print("(");
            Type[] genericParameterTypes = constructor.getGenericParameterTypes();
            Parameter[] parameters = constructor.getParameters();
            for (int i = 0; i < genericParameterTypes.length; i++) {
                writer.print(toStringGenericTypedClass(genericParameterTypes[i], new HashSet<>()) + " " + parameters[i].getName());
                if (i != genericParameterTypes.length - 1) {
                    writer.print(", ");
                }
            }
            writer.print(")");
            Type[] exceptions = constructor.getGenericExceptionTypes();
            if (exceptions.length > 0) {
                writer.print(" throws ");
                for (int i = 0; i < exceptions.length; i++) {
                    writer.print(toStringGenericTypedClass(exceptions[i], new HashSet<>()));
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

        if (!clazz.isInterface() && constructorsToSuper.size() == 0) {
            throw new ImplerException("Couldn't find non-private constructor");
        }
    }

    private Set<String> getSetOfNames(TypeVariable<?>[] typeParameters) {
        Set<String> names = new HashSet<>();
        for (TypeVariable<?> var : typeParameters) {
            names.add(var.getName());
        }
        return names;
    }

    public void implement(PrintWriter writer) throws IOException, ImplerException {
        writePackage(writer);
        writeImports(writer);
        writeClassDeclaration(writer);
        writeConstructors(writer);
        writeMethodImplementations(writer);
        writer.write("}");
    }
}
