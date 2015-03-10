package ru.ifmo.ctddev.itegulov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

/**
 * @author Daniyar Itegulov
 */
public class ImplementHelper {
    /**
     * String value for expanded tab.
     */
    private static final String TAB = "    ";

    /** String value representing line separator of current OS. */
    private static final String LS = System.lineSeparator();

    /** The value is used for initial class storage. */
    private final Class<?> clazz;

    /** The value is used for new class' name storage. */
    private final String newClassName;

    /** The value is used for {@link #clazz} methods, which need to be overridden storage. */
    private final List<Method> methodsToOverride;

    /** The value is used for {@link #clazz} constructors, which need to be supered storage. */
    private final List<Constructor> constructorsToSuper;

    /** The value is used for place, where to write {@link #clazz} implementation storage. */
    private final Appendable out;

    /**
     * The value is used for generic type variables' deduction information storage.
     * {DeclaringClassName -> {NameOfTypeVariable -> RealType}} TODO: write more info
     */
    private final Map<String, Map<String, Type>> genericNamesTranslation;

    /**
     * Initializes a new {@code ImplementHelper} so that it will be used for generating specified class'
     * implementation, gives him specified name and writes to specified {@link java.io.PrintWriter}
     *
     * @param clazz        Class to implement
     * @param newClassName Name of class to be generated
     * @param out       {@link java.io.PrintWriter} where the implementation will be placed
     * @throws ImplerException If there is no correct implementation for specified class
     */
    public ImplementHelper(Class clazz, String newClassName, Appendable out) throws ImplerException {
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
        this.out = out;
    }

    private static void initGenericNamesTranslation(Map<String, Type> passedParams,
                                                    Map<String, Map<String, Type>> genericNamesTranslation,
                                                    Class<?> clazz) {
        Map<String, Type> realNameByCurName = new HashMap<>();
        Map<String, Type> oldPassedParams = passedParams;
        for (TypeVariable<?> variable : clazz.getTypeParameters()) {
            Type realValue = variable;
            while (passedParams.containsKey(realValue.getTypeName()) && !passedParams.get(realValue.getTypeName()).equals(realValue)) {
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

    /**
     * @param type {@link Type} to check
     * @return {@code true} if specified type contains generics, {@code false} otherwise
     */
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
                if (!implemented.containsKey(new MethodSignature(method)) && !Modifier.isPrivate(method.getModifiers()) && !Modifier.isFinal(method.getModifiers())) {
                    addMethod(toImplement, method);
                }
            } else {
                addMethod(implemented, method);
            }
        }

        for (Method method : classToSearch.getMethods()) {
            if (Modifier.isAbstract(method.getModifiers())) {
                if (!implemented.containsKey(new MethodSignature(method)) && !Modifier.isFinal(method.getModifiers())) {
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

    /**
     * @param type Type, which default value to get
     * @return Some {@code String} represented correct value for specified type
     */
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

    /**
     * @param typeParameters Array of type variables to process
     * @return Set, containing names of specified type variables
     */
    private static Set<String> getSetOfNames(TypeVariable<?>[] typeParameters) {
        Set<String> names = new HashSet<>();
        for (TypeVariable<?> var : typeParameters) {
            names.add(var.getName());
        }
        return names;
    }

    /**
     * @return List of non-private constructors from {@link #clazz}, which can be supered
     */
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

    private Map<String, Map<String, Type>> createGenericNamesTranslation() {
        Map<String, Map<String, Type>> genericNamesTranslation = new HashMap<>();
        Map<String, Type> passedParams = new HashMap<>();
        for (TypeVariable<?> variable : clazz.getTypeParameters()) {
            passedParams.put(variable.getName(), variable);
        }
        initGenericNamesTranslation(passedParams, genericNamesTranslation, clazz);
        return genericNamesTranslation;
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
            StringBuilder result = new StringBuilder(((Class<?>) parameterizedType.getRawType()).getCanonicalName());
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
            return clazz.getCanonicalName();
        } else if (type instanceof WildcardType) {
            return formatWildcard((WildcardType) type, exclusionNames);
        } else {
            throw new IllegalStateException();
        }
    }

    private String formatWildcard(WildcardType type, Set<String> exclusionNames) {
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

    /**
     * Writes implementation's package name to {@link #out}
     *
     * @throws IOException
     *         If couldn't write to {@link #out}
     */
    private void writePackage() throws IOException {
        if (clazz.getPackage() != null) {
            out.append("package ").append(clazz.getPackage().getName()).append(";").append(LS).append(LS);
        }
    }

    /**
     * Writes implementation's class declaration to {@link #out}
     *
     * @throws IOException
     *         If couldn't write to {@link #out}
     */
    private void writeClassDeclaration() throws IOException {
        String modifiers;
        if (clazz.isInterface()) {
            modifiers = Modifier.toString(clazz.getModifiers() & Modifier.interfaceModifiers() & ~Modifier.ABSTRACT);
        } else {
            modifiers = Modifier.toString(clazz.getModifiers() & Modifier.classModifiers() & ~Modifier.ABSTRACT);
        }
        if (!modifiers.equals("")) {
            modifiers += " ";
        }
        out.append(modifiers);
        out.append("class ").append(newClassName).append(generateGenericParamsPrefix(clazz.getTypeParameters()));
        out.append(clazz.isInterface() ? " implements " : " extends ");
        out.append(clazz.getCanonicalName()).append(generateGenericParamsSuffix(clazz.getTypeParameters()));
        out.append(" {").append(LS);
    }

    /**
     * Writes implementation's class constructors to {@link #out}
     *
     * @throws IOException
     *         If couldn't write to {@link #out}
     *
     * @throws ImplerException
     *         If there is no non-private constructor in {@link #clazz}
     */
    private void writeConstructors() throws ImplerException, IOException {
        for (Constructor<?> constructor : constructorsToSuper) {
            if (Modifier.isPublic(constructor.getModifiers())) {
                out.append(TAB + "public ").append(newClassName);
            } else if (Modifier.isProtected(constructor.getModifiers())) {
                out.append(TAB + "protected ").append(newClassName);
            } else {
                out.append(TAB).append(newClassName);
            }
            out.append("(");
            Type[] genericParameterTypes = constructor.getGenericParameterTypes();
            Parameter[] parameters = constructor.getParameters();
            for (int i = 0; i < genericParameterTypes.length; i++) {
                out.append(toStringGenericTypedClass(genericParameterTypes[i], new HashSet<>()));
                out.append(" ").append(parameters[i].getName());
                if (i != genericParameterTypes.length - 1) {
                    out.append(", ");
                }
            }
            out.append(")");
            Type[] exceptions = constructor.getGenericExceptionTypes();
            if (exceptions.length > 0) {
                out.append(" throws ");
                for (int i = 0; i < exceptions.length; i++) {
                    out.append(toStringGenericTypedClass(exceptions[i], new HashSet<>()));
                    if (i != exceptions.length - 1) {
                        out.append(", ");
                    }
                }
            }
            out.append(" {").append(LS);
            out.append(TAB + TAB + "super(");
            for (int i = 0; i < genericParameterTypes.length; i++) {
                out.append(parameters[i].getName());
                if (i != genericParameterTypes.length - 1) {
                    out.append(", ");
                }
            }
            out.append(");").append(LS);
            out.append(TAB + "}").append(LS);
        }

        if (!clazz.isInterface() && constructorsToSuper.size() == 0) {
            throw new ImplerException("Couldn't find non-private constructor");
        }
    }

    /**
     * Writes implementation class' declaration to {@link #out}
     *
     * @throws IOException
     *         If couldn't write to {@link #out}
     */
    private void writeMethodImplementations() throws IOException {
        for (Method method : methodsToOverride) {
            if (method.getAnnotation(Deprecated.class) != null) {
                out.append(TAB + "@Deprecated").append(LS);
            }
            out.append(TAB + "@Override").append(LS);
            if (Modifier.isProtected(method.getModifiers())) {
                out.append(TAB + "protected ");
            } else if (Modifier.isPublic(method.getModifiers())) {
                out.append(TAB + "public ");
            } else if (Modifier.isPrivate(method.getModifiers())) {
                throw new IllegalStateException("Can't override private methods");
            } else {
                out.append(TAB);
            }

            String genericArgumentsPrefix = generateGenericParamsPrefix(method.getTypeParameters());
            if (!genericArgumentsPrefix.isEmpty()) {
                out.append(genericArgumentsPrefix).append(" ");
            }

            Set<String> set = getSetOfNames(method.getTypeParameters());
            out.append(toStringGenericTypedClass(method.getGenericReturnType(), set)).append(" ");
            out.append(method.getName()).append("(");
            Parameter[] parameters = method.getParameters();
            Type[] genericParameterTypes = method.getGenericParameterTypes();
            for (int i = 0; i < parameters.length; i++) {
                out.append(toStringGenericTypedClass(genericParameterTypes[i], set));
                out.append(" ").append(parameters[i].getName());
                if (i != parameters.length - 1) {
                    out.append(", ");
                }
            }
            out.append(") {").append(LS);
            if (!method.getReturnType().equals(Void.TYPE)) {
                out.append(TAB + TAB + "return ").append(getDefaultValue(method.getReturnType())).append(";").append(LS);
            }
            out.append(TAB + "}").append(LS);
            out.append(LS);
        }
    }

    /**
     * Produces code implementing current class or interface {@link #clazz}.
     *
     * Generated source code implementing {@link #clazz} will be placed in
     * the file specified by {@link java.io.PrintWriter} argument.
     *
     * @throws IOException
     *         If couldn't write to {@link #out}
     *
     * @throws ImplerException
     *         If there is no correct implementation for {@link #clazz}
     */
    public void implement() throws ImplerException, IOException {
        writePackage();
        writeClassDeclaration();
        writeConstructors();
        writeMethodImplementations();
        out.append("}");
    }
}
