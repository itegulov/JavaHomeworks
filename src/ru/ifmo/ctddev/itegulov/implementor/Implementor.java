package ru.ifmo.ctddev.itegulov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import org.omg.CORBA_2_3.ORB;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author Daniyar Itegulov
 * @since 27.02.15
 */
public class Implementor implements Impler {
    private static final String TAB = "    ";
    private final Class<?> clazz;
    private final String newClassName;
    private final List<Method> methodsToOverride;
    //{DeclaringClassName -> {NameOfTypeVariable -> RealType}}
    private final Map<String, Map<String, Type>> genericNamesTranslation;
    private final Map<String, Class<?>> imports;

    public Implementor() {
        clazz = null;
        newClassName = null;
        methodsToOverride = null;
        genericNamesTranslation = null;
        imports = null;
    }

    public Implementor(Class clazz, String newClassName) throws ImplerException {
        if (clazz.isPrimitive()) {
            throw new ImplerException("Can't implement primitives");
        }
        
        if (clazz.isArray()) {
            throw new ImplerException("Can't implement arrays");
        }
        
        if (Modifier.isFinal(clazz.getModifiers())) {
            throw new ImplerException("Can't implement final class");
        }
        
        //if (clazz.isAnnotation()) {
        //    throw new ImplerException("Couldn't implement annotations");
        //}
        //TODO: don't support some stupid type of classes (primitives?)
        this.clazz = clazz;
        this.newClassName = newClassName;
        methodsToOverride = getMethodsToOverride(clazz);
        this.genericNamesTranslation = createGenericNamesTranslation();
        this.imports = getImports();
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

    private static void addMethod(Map<WowSuchMethod, WowSuchMethod> map, Method method) throws ImplerException {
        WowSuchMethod signature = new WowSuchMethod(method);
        WowSuchMethod oldSignature = map.get(signature);
        if (oldSignature == null) {
            map.put(signature, signature);
        } else if (oldSignature.equals(signature)) {
            Method oldMethod = oldSignature.method;
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

    private static void addAllOverrideMethodsFromParents(HashMap<WowSuchMethod, WowSuchMethod> methodsSignatures, 
                                                         Class<?> clazz, String packageOfImpl) throws ImplerException {
        for (Method method : clazz.getDeclaredMethods()) {
            //Only for package local not static and not final methods
            if (!Modifier.isProtected(method.getModifiers()) && !Modifier.isPublic(method.getModifiers()) && !Modifier.isPrivate(method.getModifiers())
                    && !Modifier.isFinal(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())
                    && Modifier.isAbstract(method.getModifiers()) && clazz.getPackage().getName().equals(packageOfImpl)) {
                addMethod(methodsSignatures, method);
            }
        }

        for (Method method : clazz.getDeclaredMethods()) {
            //Only for package local not static and not final methods
            if (Modifier.isProtected(method.getModifiers())
                    && !Modifier.isFinal(method.getModifiers())
                    && Modifier.isAbstract(method.getModifiers())) {
                addMethod(methodsSignatures, method);
            }
        }

        for (Class<?> parent : clazz.getInterfaces()) {
            addAllOverrideMethodsFromParents(methodsSignatures, parent, packageOfImpl);
        }
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            addAllOverrideMethodsFromParents(methodsSignatures, superclass, packageOfImpl);
        }
    }

    private static List<Method> getMethodsToOverride(Class<?> classToSearch) throws ImplerException {
        HashMap<WowSuchMethod, WowSuchMethod> map = new HashMap<>();
        for (Method method : classToSearch.getDeclaredMethods()) {
            //Only for protected not static and not final methods (declared in this class)
            if (Modifier.isProtected(method.getModifiers())
                    && !Modifier.isFinal(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())
                    && Modifier.isAbstract(method.getModifiers())) {
                addMethod(map, method);
            }
        }
        for (Method method : classToSearch.getMethods()) {
            //Only for public not static and not final methods (visible in this class)
            if (Modifier.isPublic(method.getModifiers())
                    && !Modifier.isFinal(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())
                    && Modifier.isAbstract(method.getModifiers())) {
                addMethod(map, method);
            }
        }
        addAllOverrideMethodsFromParents(map, classToSearch, classToSearch.getPackage().getName());
        List<Method> methods = new ArrayList<>();
        for (WowSuchMethod methodSignature : map.values()) {
            methods.add(methodSignature.method);
        }
        return methods;
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

    public static void main(String[] args) throws ImplerException {
        Implementor implementor = new Implementor();
        implementor.implement(ORB.class, new File("src"));
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

        for (Constructor<?> constructor : clazz.getConstructors()) {
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
        StringBuilder result = new StringBuilder();
        if (genericArguments.length != 0) {
            result.append("<");
            for (int i = 0; i < genericArguments.length; i++) {
                TypeVariable<?> arg = genericArguments[i];
                result.append(getName(arg, genericNamesTranslation, setOfGenericParamsNames));
                if (i != genericArguments.length - 1) {
                    result.append(", ");
                } else {
                    result.append(">");
                }
            }
        }
        return result.toString();
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
            StringBuilder result = new StringBuilder(((Class<?>) parameterizedType.getRawType()).getSimpleName());
            result.append("<");
            Type[] args = parameterizedType.getActualTypeArguments();
            for (int i = 0; i < args.length; i++) {
                result.append(toStringGenericTypedClass(args[i], genericNamesTranslation, exclusionNames));
                if (i != args.length - 1) {
                    result.append(", ");
                } else {
                    result.append(">");
                }
            }
            return result.toString();
        } else if (type instanceof TypeVariable) {
            return getName(((TypeVariable) type), genericNamesTranslation, exclusionNames);
        } else if (type instanceof GenericArrayType) {
            return toStringGenericTypedClass(((GenericArrayType) type).getGenericComponentType(),
                    genericNamesTranslation, exclusionNames) + "[]";
        } else if (type instanceof Class) {
            Class<?> clazz = (Class<?>) type;
            if (imports.containsKey(clazz.getSimpleName()) && !imports.get(clazz.getSimpleName()).equals(clazz)) {
                return clazz.getName();
            } else {
                return clazz.getSimpleName();
            }
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
            StringBuilder result = new StringBuilder("? super ");
            Type[] bounds = type.getLowerBounds();
            for (int i = 0; i < bounds.length; i++) {
                Type bound = bounds[i];
                result.append(toStringGenericTypedClass(bound, genericNamesTranslation, null));
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
                result.append(toStringGenericTypedClass(bound, genericNamesTranslation, null));
                if (i != bounds.length - 1) {
                    result.append(", ");
                }
            }
            return result.toString();
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

    private void writeConstructors(PrintWriter writer) throws ImplerException {
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
        
        boolean foundNonPrivateConstructor = false;
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (!Modifier.isPrivate(constructor.getModifiers())) {
                foundNonPrivateConstructor = true;
            }
        }
        
        if (!clazz.isInterface() && !foundNonPrivateConstructor) {
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

    @Override
    public void implement(final Class<?> token, final File root) throws ImplerException {
        if (token == null || root == null) {
            throw new ImplerException("Null arguments");
        }
        File resultFile = new File(new File(root, 
                token.getPackage() != null ? token.getPackage().getName().replace(".", File.separator) : ""),
                token.getSimpleName() + "Impl.java");
        if (!resultFile.getParentFile().exists() && !resultFile.getParentFile().mkdirs()) {
            throw new ImplerException("Couldn't create dirs");
        }
        Implementor implementor = new Implementor(token, token.getSimpleName() + "Impl");
        try(PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(resultFile), StandardCharsets.UTF_8))) {
            try {
                implementor.implement(printWriter);
            } catch (IOException e) {
                throw new ImplerException("Couldn't write to output file");
            }
        } catch (FileNotFoundException e) {
            throw new ImplerException("Root file not found");
        }
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
    }
}
