package myau.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import myau.Myau;
import myau.access.AccessorMinecraft;
import myau.util.ChatUtil;
import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class FeatureConfigStore {
    private static final String FOLDER = "myau/FeatureConfigs";
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static List<String> names() {
        TreeSet<String> names = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        try {
            Enumeration<URL> roots = FeatureConfigStore.class.getClassLoader().getResources(FOLDER);
            while (roots.hasMoreElements()) {
                URL root = roots.nextElement();
                if ("file".equals(root.getProtocol())) {
                    File folder = new File(root.toURI());
                    File[] files = folder.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            String fileName = file.getName();
                            if (file.isFile() && fileName.toLowerCase().endsWith(".json")) {
                                names.add(fileName.substring(0, fileName.length() - 5));
                            }
                        }
                    }
                } else if ("jar".equals(root.getProtocol())) {
                    java.net.JarURLConnection connection = (java.net.JarURLConnection) root.openConnection();
                    JarFile jar = connection.getJarFile();
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (name.startsWith(FOLDER + "/") && name.toLowerCase().endsWith(".json")) {
                            String fileName = name.substring(FOLDER.length() + 1);
                            names.add(fileName.substring(0, fileName.length() - 5));
                        }
                    }
                    jar.close();
                }
            }
        } catch (URISyntaxException | java.io.IOException | ClassCastException e) {
            AccessorMinecraft.getLogger(mc).warn("Failed to list feature configs: " + e.getMessage());
        }
        return new ArrayList<String>(names);
    }

    public static void load(String name) {
        try (InputStream input = FeatureConfigStore.class.getResourceAsStream("/" + FOLDER + "/" + name + ".json")) {
            if (input == null) {
                ChatUtil.sendFormatted(String.format("%sFeature config not found (&c&o%s&r)&r", Myau.clientName, name));
                return;
            }
            JsonElement parsed = new JsonParser().parse(new BufferedReader(new InputStreamReader(input)));
            if (parsed == null || !parsed.isJsonObject()) {
                ChatUtil.sendFormatted(String.format("%sInvalid feature config format (&c&o%s&r)&r", Myau.clientName, name));
                return;
            }
            JsonObject object = parsed.getAsJsonObject();
            Config.apply(object);
            ChatUtil.sendFormatted(String.format("%sFeature config has been loaded (&a&o%s&r)&r", Myau.clientName, name));
        } catch (JsonSyntaxException e) {
            ChatUtil.sendFormatted(String.format("%sFeature config has invalid JSON syntax (&c&o%s&r)&r", Myau.clientName, name));
            AccessorMinecraft.getLogger(mc).error("JSON Syntax Error: " + e.getMessage());
        } catch (Exception e) {
            AccessorMinecraft.getLogger(mc).error("Error loading feature config: " + e.getMessage());
            ChatUtil.sendFormatted(String.format("%sFeature config couldn't be loaded (&c&o%s&r)&r", Myau.clientName, name));
        }
    }
}
