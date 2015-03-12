package ru.ifmo.ctddev.itegulov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

/**
 * @author Daniyar Itegulov
 * @version 1.3
 * @see ru.ifmo.ctddev.itegulov.implementor.Implementor
 */
class ImplementHelper {
    /**
     * Expanded tab.
     */
    private static final String TAB = "    ";

    /**
     * Line separator of current OS.
     * <p>
     * Equivalent to <code>System.lineSeparator()</code>
     * */
    private static final String LS = System.lineSeparator();

    /** Initial class. */
    private final Class<?> clazz;

    /** New class' name. */
    private final String newClassName;

    /** Initial class' abstract methods, which need to be overridden. */
    private final List<Method> methodsToOverride;

    /** Initial class' non-private constructors, which need to be supered. */
    private final List<Constructor> constructorsToSuper;

    /** Place, where to write new class. */
    private final Appendable out;

    /**
     * Type variables' deduction information.
     * <p>
     * Keys in this <code>Map</code> stand for declaring class name. Whereas values stand for <code>Map</code>,
     * which contains keys as type variable names and values as their real types.
     *
     * For example if you have <code>class MyClass implements Comparable<String></code>, then this
     * <code>Map</code> will contain {"Comparable", {"T", "String"}}.
     */
    private final Map<String, Map<String, Type>> genericNamesTranslation;

    /**
     * Class constructor, specifying which class to implement (<code>clazz</code>), name to give to the implementation
     * (<code>newClassName</code> and place, where to write it (<code>out</code>).
     *
     * @param clazz
     *        class to implement
     *
     * @param newClassName
     *        name of class to be generated
     *
     * @param out
     *        where the implementation will be placed
     *
     * @throws ImplerException
     *         If any of the following is true:
     *         <ul>
     *           <li> {@code clazz} is primitive.
     *           <li> {@code clazz} is array.
     *           <li> {@code clazz} is final.
     *         </ul>
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

    /**
     * Deducts all type variables for <code>clazz</code> and all it's ancestors, supposing, that
     * <code>genericNamesTranslation</code> contains all deduct information about descendant classes.
     *
     * @param passedParams
     *        Map, which contains keys as type variable names and values as their real types for
     *        <code>clazz</code>.
     *
     * @param genericNamesTranslation
     *        Map, that contains info about type variable deduction. Keys in this <code>Map</code>
     *        stand for declaring class name. Whereas values stand for <code>Map</code>, which
     *        contains keys as type variable names and values as their real types.
     *
     * @param clazz
     *        class, which will be deducted (all it ancestors also will be deducted)
     *
     * @see #parseClassParent(java.lang.reflect.Type, java.util.Map) 
     */
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

    /**
     * Deducts type variable in <code>parent</code> and it's ancestors, if it is necessary
     * <p>
     * If <code>parent instanceof ParameterizedType</code>, then it's type variables will
     * be deducted and added to <code>genericNamesTranslation</code>. Assumes, that all
     * information about descendant classes is provided in <code>genericNamesTranslation</code>
     *
     * @param parent
     *        class, which will be deducted if necessary
     *
     * @param genericNamesTranslation
     *        Map, that contains info about type variable deduction. Keys in this <code>Map</code>
     *        stand for declaring class name. Whereas values stand for <code>Map</code>, which
     *        contains keys as type variable names and values as their real types.
     *
     *  @see #initGenericNamesTranslation(java.util.Map, java.util.Map, Class)
     */
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
     * Checks if <code>type</code> contains generics
     *
     * @param type
     *        type to check
     *
     * @return <code>true</code> if specified type contains generics, <code>false</code> otherwise
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

    /**
     * Converts {@link java.lang.reflect.Method} to {@link ru.ifmo.ctddev.itegulov.implementor.MethodSignature}
     * and puts it to <code>Map</code>, if there is no similar <code>MethodSignature</code>.
     *
     * @param map
     *        map, where method signatures are stored
     *
     * @param method
     *        method, which needs to be stored
     *
     * @see MethodSignature#equals(Object)
     */
    private static void addMethod(Map<MethodSignature, MethodSignature> map, Method method) {
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

    /**
     * Fetches all methods, which need to be overridden in specified class and also all it's
     * ancestors.
     * <p>
     * It will provide you only public and protected methods, which have no implementation in
     * <code>classToSearch</code> and it's ancestors.
     *
     * @param toImplement
     *        map, used as storage for methods, which need implementation
     *
     * @param implemented
     *        map, used as storage for methods, which have implementation
     *
     * @param classToSearch
     *        class, which must be observed
     *
     * @see #addMethod(java.util.Map, java.lang.reflect.Method)
     */
    private static void getMethodsToOverride(Map<MethodSignature, MethodSignature> toImplement,
                                             Map<MethodSignature, MethodSignature> implemented,
                                             Class<?> classToSearch) {
        for (Method method : classToSearch.getDeclaredMethods()) {
            if (Modifier.isAbstract(method.getModifiers() & Modifier.methodModifiers())) {
                if (!implemented.containsKey(new MethodSignature(method))
                        && !Modifier.isPrivate(method.getModifiers())
                        && !Modifier.isFinal(method.getModifiers())) {
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
     * Gets some correct value for <code>type</code>
     * <p>
     * It returns only <code>0</code>, <code>false</code> and <code>null</code> really.
     *
     * @param type
     *        type, which default value to get
     *
     * @return string, representing some correct value for specified type
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
     * Gets only unique type variables from <code>typeParameters</code>
     *
     * @param typeParameters
     *        an array of type variables to process
     *
     * @return set, containing names of specified type variables
     */
    private static Set<String> getSetOfNames(TypeVariable<?>[] typeParameters) {
        Set<String> names = new HashSet<>();
        for (TypeVariable<?> var : typeParameters) {
            names.add(var.getName());
        }
        return names;
    }

    /**
     * @return List of non-private constructors from initial class, which can be supered.
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

    /**
     * Deducts generic type variable information for initial class.
     *
     * @return  Map, satisfying {@link #genericNamesTranslation} conditions
     *
     * @see #initGenericNamesTranslation(java.util.Map, java.util.Map, Class)
     */
    private Map<String, Map<String, Type>> createGenericNamesTranslation() {
        Map<String, Map<String, Type>> genericNamesTranslation = new HashMap<>();
        Map<String, Type> passedParams = new HashMap<>();
        for (TypeVariable<?> variable : clazz.getTypeParameters()) {
            passedParams.put(variable.getName(), variable);
        }
        initGenericNamesTranslation(passedParams, genericNamesTranslation, clazz);
        return genericNamesTranslation;
    }

    /**
     * Gets <code>String</code>, containing representation of new class' type variables with
     * bounds.
     * <p>
     * It will be empty string if no type variables present, otherwise it will be given
     * in this form: "<" + all type variables written and separated by comma maybe with
     * bounds + ">".
     *
     * For example if you have <code>class MyClass<E extends Exception, T extends Stream>
     * implements Comparable<String></code>, then this method will give you "<E extends
     * Exception, T extends Stream>"
     *
     * @param genericArguments
     *        an array of type variables, which stands for initial class' type parameters
     *
     * @return string, representing type variables with bounds, used in this class
     *
     * @see #generateGenericParamsSuffix(java.lang.reflect.TypeVariable[])
     * @see #getName(java.lang.reflect.TypeVariable, java.util.Set)
     */
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

    /**
     * Gets <code>String</code>, containing representation of new class' type variables
     * without bounds.
     * <p>
     * It will be empty string if no type variables present, otherwise it will be given
     * in this form: "<" + all type variables written and separated by comma without
     * bounds + ">".
     *
     * For example if you have <code>class MyClass<E extends Exception, T extends Stream>
     * implements Comparable<String></code>, then this method will give you "<E, T>"
     *
     * @param genericArguments
     *        an array of type variables, which stands for initial class' type parameters
     *
     * @return string, representing type variables without bounds, used in this class
     *
     * @see #generateGenericParamsPrefix(java.lang.reflect.TypeVariable[])
     * @see #getName(java.lang.reflect.TypeVariable, java.util.Set)
     */
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

    /**
     * Generates string representation of type variable, using deduction
     * bug ignoring some type variables
     * <p>
     * Assumes, that {@link #genericNamesTranslation} is filled correctly
     *
     * @param type
     *        type to represent
     *
     * @param exclusionNames
     *        set of type variables' names, which mustn't be deducted
     *
     * @return string, representing specified type variable in canonical form
     *
     * @see #toStringGenericTypedClass(java.lang.reflect.Type, java.util.Set)
     * @see #formatWildcard(java.lang.reflect.WildcardType, java.util.Set)
     */
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
        Type realType = map.get(type.getName());
        if (realType instanceof TypeVariable<?>) {
            return ((TypeVariable) realType).getName();
        } else {
            return toStringGenericTypedClass(realType, null);
        }
    }

    /**
     * Generates string representation of generic type using deduction
     * bug ignoring some type variables
     *
     * @param type
     *        type to represent
     *
     * @param exclusionNames
     *        set of type variables' names, which mustn't be deducted
     *
     * @return string, representing specified generic type in canonical form
     *
     * @see #getName(java.lang.reflect.TypeVariable, java.util.Set)
     * @see #formatWildcard(java.lang.reflect.WildcardType, java.util.Set)
     */
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

    /**
     * Generates string representation of wildcard type using deduction
     * bug ignoring some type variables
     *
     * @param type
     *        wildcard to represent
     *
     * @param exclusionNames
     *        set of type variable names, which mustn't be deducted
     *
     * @return string, representing specified wildcard type in canonical form
     *
     * @see #getName(java.lang.reflect.TypeVariable, java.util.Set)
     * @see #toStringGenericTypedClass(java.lang.reflect.Type, java.util.Set)
     */
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
     *         if couldn't write to <code>out</code>
     */
    private void writePackage() throws IOException {
        if (clazz.getPackage() != null) {
            out.append("package ").append(clazz.getPackage().getName()).append(";").append(LS).append(LS);
        }
    }

    /**
     * Writes implementation's class declaration to {@link #out}
     * <p>
     * Copies all parents modifiers excluding <code>abstract</code>. Also correctly
     * works with generic classes
     *
     * @throws IOException
     *         if couldn't write to <code>out</code>
     *
     * @see Class#getModifiers()
     * @see #generateGenericParamsSuffix(java.lang.reflect.TypeVariable[])
     * @see #generateGenericParamsPrefix(java.lang.reflect.TypeVariable[])
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
     * <p>
     * Supers all non-private constructors, declared in parent. Copies all parent
     * constructor exceptions and passes default values to it.
     *
     * @throws IOException
     *         if couldn't write to <code>out</code>
     *
     * @throws ImplerException
     *         if there is no non-private constructor in {@link #clazz} and it's
     *         not an interface
     *
     * @see #getConstructorsToSuper()
     * @see #getDefaultValue(java.lang.reflect.Type)
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
     * <p>
     * Overrides all abstract methods from ancestors. Copies access modifier
     * from ancestor method and returns default value for return type. Ignores
     * all exceptions, declared in ancestor method
     *
     * @throws IOException
     *         if couldn't write to <code>out</code>
     *
     * @see #getMethodsToOverride(java.util.Map, java.util.Map, Class)
     * @see #getDefaultValue(java.lang.reflect.Type)
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
     * Produces code implementing current class or interface {@link #clazz} and
     * places it to {@link #out}.
     * <p>
     * Produced code will be correct Java code and will be successfully compiled
     * by Java compiler. New class' package will be equal to initial class' package
     * It will super all available constructors from parent, or, if there is no any
     * constructor (all parents are interfaces), it will leave default constructor.
     * All abstract methods will be overridden. No fields or new methods will be
     * generated.
     *
     * @throws IOException
     *         if couldn't write to <code>out</code>
     *
     * @throws ImplerException
     *         if there is no correct implementation for <code>clazz</code>
     */
    public void implement() throws ImplerException, IOException {
        writePackage();
        writeClassDeclaration();
        writeConstructors();
        writeMethodImplementations();
        out.append("}");
    }
}
