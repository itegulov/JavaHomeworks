package ru.ifmo.ctddev.itegulov.implementor.test;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.examples.InterfaceWithDefaultMethod;
import info.kgeorgiy.java.advanced.implementor.examples.InterfaceWithStaticMethod;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;
import org.omg.DynamicAny.DynAny;
import ru.ifmo.ctddev.itegulov.implementor.Implementor;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleAction;
import javax.annotation.Generated;
import javax.management.Descriptor;
import javax.management.loading.PrivateClassLoader;
import javax.sql.rowset.CachedRowSet;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import javax.xml.bind.Element;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ImplementorTest {
    private String methodName;
    @Rule
    public TestWatcher watcher = new TestWatcher() {
        protected void starting(Description var1) {
            ImplementorTest.this.methodName = var1.getMethodName();
            System.out.println("== Running " + var1.getMethodName());
        }
    };

    public ImplementorTest() {
    }

    @Test
    public void test01_constructor() throws ClassNotFoundException, NoSuchMethodException {
        Class var1 = this.loadClass();
        this.assertImplements(var1, Impler.class);
        this.checkConstructor("public default constructor", var1, new Class[0]);
    }

    private void assertImplements(Class<?> var1, Class<?> var2) {
        Assert.assertTrue(var1.getName() + " should implement " + var2.getName() + " interface", var2.isAssignableFrom(var1));
    }

    @Test
    public void test02_standardMethodlessInterfaces() {
        this.test(false, new Class[]{Element.class, PrivateClassLoader.class});
    }

    @Test
    public void test03_standardInterfaces() {
        this.test(false, new Class[]{Accessible.class, AccessibleAction.class, Generated.class});
    }

    @Test
    public void test04_extendedInterfaces() {
        this.test(false, new Class[]{Descriptor.class, CachedRowSet.class, DynAny.class});
    }

    @Test
    public void test05_standardNonInterfaces() {
        this.test(true, new Class[]{Void.TYPE, String[].class, int[].class, String.class, Boolean.TYPE});
    }

    @Test
    public void test06_java8Interfaces() {
        this.test(false, new Class[]{InterfaceWithStaticMethod.class, InterfaceWithDefaultMethod.class});
    }

    protected void test(boolean var1, Class<?>... var2) {
        File var3 = this.getRoot();

        try {
            this.implement(var1, var3, Arrays.asList(var2));
            if(!var1) {
                this.compile(var3, Arrays.asList(var2));
                this.check((File)var3, (List)Arrays.asList(var2));
            }
        } finally {
            this.clean(var3);
        }

    }

    private File getRoot() {
        return new File(".", this.methodName);
    }

    private URLClassLoader getClassLoader(File var1) {
        try {
            return new URLClassLoader(new URL[]{var1.toURI().toURL()});
        } catch (MalformedURLException var3) {
            throw new AssertionError(var3);
        }
    }

    private void compile(File var1, List<Class<?>> var2) {
        ArrayList var3 = new ArrayList();
        Iterator var4 = var2.iterator();

        while(var4.hasNext()) {
            Class var5 = (Class)var4.next();
            var3.add(this.getFile(var1, var5).getPath());
        }

        this.compileFiles(var1, var3);
    }

    private void compileFiles(File var1, List<String> var2) {
        JavaCompiler var3 = ToolProvider.getSystemJavaCompiler();
        Assert.assertNotNull("Could not find java compiler, include tools.jar to classpath", var3);
        ArrayList var4 = new ArrayList();
        var4.addAll(var2);
        var4.add("-cp");
        var4.add(var1.getPath() + File.pathSeparator + System.getProperty("java.class.path"));
        int var5 = var3.run((InputStream)null, (OutputStream)null, (OutputStream)null, (String[])var4.toArray(new String[var4.size()]));
        Assert.assertEquals("Compiler exit code", 0L, (long)var5);
    }

    private void clean(File var1) {
        if(var1.isDirectory()) {
            File[] var2 = var1.listFiles();
            if(var2 != null) {
                File[] var3 = var2;
                int var4 = var2.length;

                for(int var5 = 0; var5 < var4; ++var5) {
                    File var6 = var3[var5];
                    this.clean(var6);
                }
            }
        }

        var1.delete();
    }

    private void checkConstructor(String var1, Class<?> var2, Class<?>... var3) {
        try {
            var2.getConstructor(var3);
        } catch (NoSuchMethodException var5) {
            Assert.fail(var2.getName() + " should have " + var1);
        }

    }

    private void implement(boolean var1, File var2, List<Class<?>> var3) {
        Impler var4;
        try {
            var4 = (Impler)this.loadClass().newInstance();
        } catch (Exception var8) {
            var8.printStackTrace();
            Assert.fail("Instantiation error");
            var4 = null;
        }

        for (final Object aVar3 : var3) {
            Class var6 = (Class) aVar3;

            try {
                var4.implement(var6, var2);
                Assert.assertTrue("You may not implement " + var6, !var1);
            } catch (ImplerException var9) {
                if (var1) {
                    return;
                }

                throw new AssertionError("Error implementing " + var6, var9);
            } catch (Throwable var10) {
                throw new AssertionError("Error implementing " + var6, var10);
            }

            File var7 = this.getFile(var2, var6);
            Assert.assertTrue("Error implementing clazz: File \'" + var7 + "\' not found", var7.exists());
        }

    }

    private File getFile(File var1, Class<?> var2) {
        String var3 = var2.getCanonicalName().replace(".", "/") + "Impl.java";
        return (new File(var1, var3)).getAbsoluteFile();
    }

    private Class<?> loadClass() throws ClassNotFoundException {
        return Implementor.class;
    }

    private void check(File var1, List<Class<?>> var2) {
        URLClassLoader var3 = this.getClassLoader(var1);

        for (final Object aVar2 : var2) {
            Class var5 = (Class) aVar2;
            this.check(var3, var5);
        }

    }

    private void check(URLClassLoader var1, Class<?> var2) {
        String var3 = var2.getCanonicalName() + "Impl";

        try {
            Class var4 = var1.loadClass(var3);
            if(var2.isInterface()) {
                Assert.assertTrue(var3 + " should implement " + var2, Arrays.asList(var4.getInterfaces()).contains(var2));
            } else {
                Assert.assertEquals(var3 + " should extend " + var2, var2, var4.getSuperclass());
            }

            Assert.assertFalse(var3 + " should not be abstract", Modifier.isAbstract(var4.getModifiers()));
            Assert.assertFalse(var3 + " should not be interface", Modifier.isInterface(var4.getModifiers()));
        } catch (ClassNotFoundException var5) {
            throw new AssertionError("Error loading class " + var3, var5);
        }
    }
}
