
package Configuration;

import Action.Action;
import Configuration.Config.PropertyConfig;
import Configuration.Config.ReadOnlyPropertyConfig;
import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.WritableValue;
import main.App;
import org.atteo.classindex.ClassIndex;
import util.File.FileUtil;
import static util.Util.getAllFields;
import util.dev.Log;
import static util.dev.Util.forbidFinal;
import static util.dev.Util.requireFinal;

/**
 * Provides methods to access configs of the application.
 * 
 * @author uranium
 */
public class Configuration {
    
    private static final Map<String,Config> configs = new HashMap();
    private static final Lookup methodLookup = MethodHandles.lookup();
    
    static {
        // for all discovered classes
        ClassIndex.getAnnotated(IsConfigurable.class).forEach( c -> {
            // add class fields
            discoverConfigFieldsOf(c);
            // add methods in the end to avoid incorrect initialization
            discoverMethodsOf(c);        
        });
    }
    
    private static void discoverConfigFieldsOf(Class clazz) {
        configs.putAll(configsOf(clazz, null, true, false));
    }
    private static void discoverMethodsOf(Class cls) {
        for (Method m : cls.getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers())) {
                for(AppliesConfig a : m.getAnnotationsByType(AppliesConfig.class)) {
                    if (a != null) {
                        String name = a.value();
                        if(!name.isEmpty() && configs.containsKey(name)) {
                            Config c = configs.get(name);
                            if(c instanceof FieldConfig) {
                                try {
                                    m.setAccessible(true);
                                    ((FieldConfig)c).applier = methodLookup.unreflect(m);
                                    Log.deb("Adding method as applier method: " + m.getName() + " for " + name + ".");
                                } catch (IllegalAccessException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    private static String getGroup(Class<?> c) {
        IsConfigurable a = c.getAnnotation(IsConfigurable.class);
        return a==null || a.value().isEmpty() ? c.getSimpleName() : a.value();
    }
    
    static Map<String,Config> configsOf(Class clazz, Object instnc, boolean include_static, boolean include_instance) {
        // check arguments
        if(include_instance && instnc==null)
            throw new IllegalArgumentException("Instance must not be null if instance fields flag is true");
        
        Map<String,Config> out = new HashMap();
        
        for (Field f : getAllFields(clazz)) {
            Config c = createConfig(clazz, f, instnc, include_static, include_instance);
            if(c!=null) out.put(c.getName(), c);
        }
        return out;
    }
    
    static Config createConfig(Class cl, Field f, Object instnc, boolean include_static, boolean include_instance) {
        // that are annotated
        Config c = null;
        IsConfig a = f.getAnnotation(IsConfig.class);
        if (a != null) {
            String group = a.group().isEmpty() ? getGroup(cl) : a.group();
            String name = f.getName();
            int modifiers = f.getModifiers();
            if (include_static && Modifier.isStatic(modifiers))
                c = createConfig(f, instnc, name, a, group);
            
            if (include_instance && !Modifier.isStatic(modifiers))
                c = createConfig(f, instnc, name, a, group);
            
        }
        return c;
    }
    
    private static Config createConfig(Field f, Object instance, String name, IsConfig anotation, String group) {
        Class c = f.getType();
        if(Config.class.isAssignableFrom(c)) {
            return newFromConfig(f, instance);
        }
        else if(WritableValue.class.isAssignableFrom(c) || ReadOnlyProperty.class.isAssignableFrom(c))
            return newFromProperty(f, instance, name, anotation, group);
        else {
            try {
                forbidFinal(f);            // make sure the field is not final
                f.setAccessible(true);     // make sure the field is accessible
                MethodHandle getter = methodLookup.unreflectGetter(f);
                MethodHandle setter = methodLookup.unreflectSetter(f);
                return new FieldConfig(name, anotation, instance, group, getter, setter);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Unreflecting field " + f.getName() + " failed. " + e.getMessage());
            }
            
            
        }
    }
    
    private static Config newFromProperty(Field f, Object instance, String name, IsConfig anotation, String group) {
        try {
            requireFinal(f);            // make sure the field is final
            f.setAccessible(true);      // make sure the field is accessible
            if(WritableValue.class.isAssignableFrom(f.getType()))
                return new PropertyConfig(name, anotation, (WritableValue)f.get(instance), group);
            if(ReadOnlyProperty.class.isAssignableFrom(f.getType()))
                return new ReadOnlyPropertyConfig(name, anotation, (ReadOnlyProperty)f.get(instance), group);
            throw new IllegalArgumentException("Wrong class");
        } catch (IllegalAccessException | SecurityException e) {
            throw new RuntimeException("Can not access field: " + f.getName() + " for class: " + f.getDeclaringClass());
        }
    }
    
    private static Config newFromConfig(Field f, Object instance) {
        try {
            requireFinal(f);            // make sure the field is final
            f.setAccessible(true);      // make sure the field is accessible
            return (Config)f.get(instance);
        } catch (IllegalAccessException | SecurityException ex) {
            throw new RuntimeException("Can not access field: " + f.getName() + " for class: " + f.getDeclaringClass());
        }
    }
    
/******************************* public api ***********************************/
    
    /**
     * @param name
     * @return config with given name or null if such config does not exists
     */
    public static Config getField(String name) {
        Config c = configs.get(name);
        if (c==null) c = Action.getAction(name);
        return c;
    }
    
    public static List<Config> getFields() {
        List<Config> cs = new ArrayList(configs.values());
                     cs.addAll(Action.getActions());
        return cs;
    }
    
    public static List<Config> getFields(Predicate<Config> condition) {
        List<Config> cs = new ArrayList(getFields());
                     cs.removeIf(condition.negate());
        return cs;
    }
    
    /**
     * Changes all config fields to their default value and applies them
     */
    public static void toDefault() {
        getFields().forEach(Config::setNapplyDefaultValue);
    }
    
    /**
     * Saves configuration to the file. The file is created if it does not exist,
     * otherwise it is completely overwritten.
     * Loops through Configuration fields and stores them all into file.
     */
    public static void save() {     
        Log.info("Saving configuration");
        
        String content="";
               content += "# " + App.getAppName() + " configuration file.\n";
               content += "# " + java.time.LocalTime.now() + "\n";
        
        for (Config f: getFields())
            content += f.getName() + " : " + f.getValueS() + "\n";
        
        FileUtil.writeFile("Settings.cfg", content);
    }
    
    /**
     * Loads previously saved configuration file and set its values for this.
     * <p>
     * Attempts to load all configuration fields from file. Fields might not be 
     * read either through I/O error or parsing errors. Parsing errors are
     * recoverable, meaning corrupted fields will be ignored.
     * Default values will be used for all unread fields.
     * <p>
     * If field of given name does not exist it will be ignored as well.
     */
    public static void load() {
        Log.info("Loading configuration");
        
        File file= new File("Settings.cfg").getAbsoluteFile();
        Map<String,String> lines = FileUtil.readFileKeyValues(file);
        if(lines.isEmpty())
            Log.info("Configuration couldnt be loaded. No content found. Using "
                    + "default settings.");
        
        
        lines.forEach((name,value) -> {
            Config c = getField(name);
            if (c!=null)
                c.setValueS(value);
            else
                Log.info("Config field " + name + " not available. Possible"
                    + " error in the configuration file.");
        });
    }
    
}