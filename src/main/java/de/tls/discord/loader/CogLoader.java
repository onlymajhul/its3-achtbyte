package de.tls.discord.loader;

import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class CogLoader {

    public static List<ListenerAdapter> loadCogs(String packageName) {
        List<ListenerAdapter> cogs = new ArrayList<>();

        try {
            String path = packageName.replace('.', '/');
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL resource = classLoader.getResource(path);

            if (resource == null) return cogs;

            File folder = new File(resource.toURI());

            for (File file : folder.listFiles()) {
                if (file.getName().endsWith(".class")) {

                    String className = packageName + "." + file.getName().replace(".class", "");
                    Class<?> clazz = Class.forName(className);

                    if (ListenerAdapter.class.isAssignableFrom(clazz)) {
                        ListenerAdapter cog = (ListenerAdapter) clazz.getDeclaredConstructor().newInstance();
                        cogs.add(cog);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return cogs;
    }
}