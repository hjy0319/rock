package com.hujy.rock.common.extension;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

@Slf4j
public class ExtensionLoader<T> {

    /**
     * 扩展配置文件加载路径
     */
    private static final String CRM_DIRECTORY = "META-INF/rock/";

    /**
     * 分隔符
     */
    private static final Pattern NAME_SEPARATOR = Pattern.compile("\\s*[,]+\\s*");

    /**
     * 缓存ExtensionLoader
     * (Class -> loader)
     */
    private static final Map<Class<?>, ExtensionLoader<?>> EXTENSION_LOADER = new ConcurrentHashMap<>();

    /**
     * 缓存扩展点实现类的实例对象
     * (Class -> instance)
     */
    private static final ConcurrentMap<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<>();


    /**
     * 缓存实现的name与class
     * (name -> Class)
     */
    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<>();

    /**
     * 缓存实现类的class与name
     * (Class -> name)
     */
    private final ConcurrentMap<Class<?>, String> cachedNames = new ConcurrentHashMap<>();

    /**
     * 缓存name与Holder，Holder封装了实现类实例
     * (name -> holder)
     */
    private final ConcurrentMap<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<>();

    /**
     * 缓存wrapperClasses
     */
    private Set<Class<?>> cachedWrapperClasses;

    /**
     * 默认扩展点
     * loadExtensionClasses 加载扩展文件的时候赋值
     */
    private String cachedDefaultName;

    private final Class<?> extension;

    public ExtensionLoader(Class<?> extension) {
        this.extension = extension;
        loadExtensionClasses();
    }

    /**
     * 获取ExtensionLoader实例
     *
     * @param extension
     * @return com.ziroom.crm.common.core.extension.ExtensionLoader
     * @author hujy
     * @date 2019-10-20 21:17
     */
    public static ExtensionLoader getExtensionLoader(Class extension) {
        // 扩展点校验
        check(extension);
        // 非首次加载，可以从缓存容器中获取
        ExtensionLoader extensionLoader = EXTENSION_LOADER.get(extension);
        if (extensionLoader != null) {
            return extensionLoader;
        }
        // 通过double check方式创建ExtensionLoader实例
        synchronized (EXTENSION_LOADER) {
            extensionLoader = EXTENSION_LOADER.get(extension);
            if (extensionLoader == null) {
                extensionLoader = new ExtensionLoader(extension);
                EXTENSION_LOADER.put(extension, extensionLoader);
            }
        }
        return extensionLoader;
    }


    /**
     * 获取扩展点实例
     *
     * @param name
     * @return T
     * @author hujy
     * @date 2019-10-20 22:52
     */
    public T getExtension(String name) {
        if (name == null || name.length() == 0 || "true".equals(name)) {
            return getDefaultExtension();
        }

        Holder<Object> holder = cachedInstances.get(name);
        if (holder == null) {
            cachedInstances.putIfAbsent(name, new Holder<>());
            holder = cachedInstances.get(name);
        }
        Object instance = holder.get();
        if (instance == null) {
            synchronized (holder) {
                instance = holder.get();
                if (instance == null) {
                    instance = createExtension(name);
                    holder.set(instance);
                }
            }
        }
        return (T) instance;
    }

    /**
     * 创建扩展点实例
     *
     * @param name
     * @return T
     * @author hujy
     * @date 2019-10-20 22:55
     */
    private T createExtension(String name) {
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null) {
            throw new IllegalStateException();
        }
        try {
            T instance = (T) EXTENSION_INSTANCES.get(clazz);
            if (instance == null) {
                EXTENSION_INSTANCES.putIfAbsent(clazz, clazz.newInstance());
                instance = (T) EXTENSION_INSTANCES.get(clazz);
            }
            Set<Class<?>> wrapperClasses = cachedWrapperClasses;
            if (wrapperClasses != null && wrapperClasses.size() > 0) {
                for (Class<?> wrapperClass : wrapperClasses) {
                    instance = (T) wrapperClass.getConstructor(extension).newInstance(instance);
                }
            }
            return instance;
        } catch (Throwable t) {
            log.error(t.getMessage(), t);
            throw new IllegalStateException("Extension instance(name: " + name + ", class: " +
                    extension + ")  could not be instantiated: " + t.getMessage(), t);
        }
    }


    /**
     * 加载默认的扩展点实现
     *
     * @param
     * @return T
     * @author hujy
     * @date 2019-10-21 09:56
     */
    private T getDefaultExtension() {
        getExtensionClasses();
        if (null == cachedDefaultName || cachedDefaultName.length() == 0
                || "true".equals(cachedDefaultName)) {
            return null;
        }
        return getExtension(cachedDefaultName);
    }


    private Map<String, Class<?>> getExtensionClasses() {
        Map<String, Class<?>> classes = cachedClasses.get();
        if (classes == null) {
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if (classes == null) {
                    try {
                        classes = loadExtensionClasses();
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }


    /**
     * 加载扩展点的实现
     *
     * @param
     * @return java.util.Map<java.lang.String, java.lang.Class < ?>>
     * @author hujy
     * @date 2019-10-20 21:38
     */
    private Map<String, Class<?>> loadExtensionClasses() {
        // 首先获取默认的实现策略，即解析@SPI的value
        final SPI defaultAnnotation = extension.getAnnotation(SPI.class);
        if (defaultAnnotation != null) {
            String value = defaultAnnotation.value();
            if ((value = value.trim()).length() > 0) {
                // 默认实现只允许一个
                String[] names = NAME_SEPARATOR.split(value);
                if (names.length > 1) {
                    throw new IllegalStateException("more than 1 default extension name on extension " + extension.getName()
                            + ": " + Arrays.toString(names));
                }
                // 将@SPI的value赋值给cachedDefaultName
                if (names.length == 1) {
                    cachedDefaultName = names[0];
                }
            }
        }
        // 从约定路径下加载扩展实现的信息
        Map<String, Class<?>> extensionClasses = new HashMap<>();
        try {
            loadFile(extensionClasses, CRM_DIRECTORY);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return extensionClasses;
    }

    /**
     * 加载并解析文件
     *
     * @param extensionClasses
     * @param dir
     * @return void
     * @author hujy
     * @date 2019-10-20 21:54
     */
    private void loadFile(Map<String, Class<?>> extensionClasses, String dir) throws IOException {
        //这里拼凑了一个需要加载的文件路径
        String fileName = dir + extension.getName();
        Enumeration<URL> urls;
        //获取类加载器（应用类加载器）
        ClassLoader classLoader = findClassLoader();
        if (classLoader != null) {
            urls = classLoader.getResources(fileName);
        } else {
            urls = ClassLoader.getSystemResources(fileName);
        }

        if (urls != null) {
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {

                    String line;
                    while ((line = reader.readLine()) != null) {

                        final int ci = line.indexOf('#');
                        if (ci >= 0) line = line.substring(0, ci);
                        line = line.trim();
                        if (line.length() > 0) {

                            String name = null;
                            int i = line.indexOf('=');
                            if (i > 0) {
                                name = line.substring(0, i).trim();
                                line = line.substring(i + 1).trim();
                            }
                            if (line.length() > 0) {
                                // 加载扩展类
                                Class<?> clazz;
                                try {
                                    clazz = Class.forName(line, true, classLoader);
                                } catch (Throwable e) {
                                    log.error(line + " could not be loaded:" + e.getCause());
                                    continue;
                                }

                                // 判断加载类的类型是否是扩展点的类型
                                if (!extension.isAssignableFrom(clazz)) {
                                    throw new IllegalStateException("Error when load extension class(interface: " +
                                            extension + ", class line: " + clazz.getName() + "), class "
                                            + clazz.getName() + "is not subtype of interface.");
                                }
                                if (isWrapperClass(clazz)) {
                                    // 以扩展点为构造器参数的为wrapper类
                                    if (cachedWrapperClasses == null) {
                                        cachedWrapperClasses = new ConcurrentHashSet<>();
                                    }
                                    cachedWrapperClasses.add(clazz);
                                } else {
                                    if (name == null || name.length() == 0) {
                                        throw new IllegalStateException("No such extension name for the class " + clazz.getName() + " in the config " + url);
                                    }
                                    // name 按都逗号分隔，可以配置多个
                                    String[] names = NAME_SEPARATOR.split(name);
                                    if (names != null && names.length > 0) {
                                        for (String n : names) {
                                            if (!cachedNames.containsKey(clazz)) {
                                                cachedNames.put(clazz, n);
                                            }
                                            Class<?> c = extensionClasses.get(n);
                                            if (c == null) {
                                                extensionClasses.put(n, clazz);
                                            } else if (c != clazz) {
                                                throw new IllegalStateException("Duplicate extension " + extension.getName() + " name " + n + " on " + c.getName() + " and " + clazz.getName());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                }

            }
        }
    }

    private boolean isWrapperClass(Class<?> clazz) {
        try {
            clazz.getConstructor(extension);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }


    private static ClassLoader findClassLoader() {
        return ExtensionLoader.class.getClassLoader();
    }

    private static void check(Class type) {
        if (type == null)
            throw new IllegalArgumentException("Extension extension == null");
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension extension(" + type + ") is not interface!");
        }
        if (!withExtensionAnnotation(type)) {
            throw new IllegalArgumentException("Extension extension(" + type +
                    ") is not extension, because WITHOUT @" + SPI.class.getSimpleName() + " Annotation!");
        }
    }

    private static <T> boolean withExtensionAnnotation(Class<T> type) {
        return type.isAnnotationPresent(SPI.class);
    }
}
