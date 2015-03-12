package ru.ifmo.ctddev.itegulov.implementor;

import java.lang.reflect.*;


/**
 * Represents methods' equivalence by their generic signatures
 * <p>
 * Signatures are constants; their value cannot be changed in
 * any way after they instantiated.
 * <p>
 * Some examples of equivalent methods (by their signature):
 * <blockquote><pre>
 *     void foo(E e);
 *     void foo(Object o);
 *
 *     void bar(E[] e);
 *     void bar(Object[] o);
 * </pre></blockquote><p>
 *
 * @author Daniyar Itegulov
 * @see java.lang.reflect.Method
 * @see ru.ifmo.ctddev.itegulov.implementor.Implementor
 * @see ru.ifmo.ctddev.itegulov.implementor.ImplementHelper
 */
class MethodSignature {
    /** Method, which signature is represented. */
    private final Method method;

    /**
     * Class constructor, specifying what method's signature to represent
     *
     * @param method
     *        Source of method signature
     */
    public MethodSignature(Method method) {
        this.method = method;
    }

    /**
     * @param thisType
     *        first type to check
     *
     * @param thatType
     *        second type to check
     *
     * @return <code>true</code>, if two types differs only in type variable,
     *         <code>false</code> otherwise
     */
    @SuppressWarnings("RedundantIfStatement")
    private static boolean differsInTypeVariable(Type thisType, Type thatType) {
        if (thisType instanceof ParameterizedType) {
            return false;
        } else if (thisType instanceof GenericArrayType) {
            if (thatType instanceof GenericArrayType) {
                GenericArrayType thisGeneric = (GenericArrayType) thisType;
                GenericArrayType thatGeneric = (GenericArrayType) thatType;
                return differsInTypeVariable(thisGeneric.getGenericComponentType(), thatGeneric
                        .getGenericComponentType());
            } else {
                return false;
            }
        } else if (thisType instanceof Class) {
            Class thisClass = (Class) thisType;
            if (thisClass.isArray()) {
                if (thatType instanceof Class) {
                    Class thatClass = (Class) thatType;
                    return thatClass.isArray() 
                            && differsInTypeVariable(thisClass.getComponentType(), thatClass .getComponentType());
                } else if (thatType instanceof GenericArrayType) {
                    GenericArrayType thatGeneric = (GenericArrayType) thatType;
                    return differsInTypeVariable(thisClass.getComponentType(), thatGeneric
                            .getGenericComponentType());
                } else {
                    return false;
                }
            } else if (thisClass.equals(Object.class)) {
                return true;
            } else {
                return false;
            }
        } else if (thisType instanceof WildcardType) {
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

        MethodSignature that = (MethodSignature) o;

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

        return method.getReturnType().equals(that.method.getReturnType());
    }

    @Override
    public int hashCode() {
        return method.getName().hashCode();
    }

    /**
     * @return method Method, which signature is represented
     */
    public Method getMethod() {
        return method;
    }
}
