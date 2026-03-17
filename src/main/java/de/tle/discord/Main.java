package de.tle.discord;

import de.tle.discord.db.Database;
import de.tle.discord.loader.CogLoader;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;

public class Main {
    public static void main(String[] arguments) throws Exception {

        Dotenv dotenv = Dotenv.load();
        String token = dotenv.get("TOKEN");
        Database.connect();

        JDABuilder builder = JDABuilder.createDefault(token);

        List<ListenerAdapter> cogs = CogLoader.loadCogs("de.tle.discord.cogs");

        for (ListenerAdapter cog : cogs) {
            builder.addEventListeners(cog);
            System.out.println("Loaded cog: " + cog.getClass().getSimpleName());
        }

        JDA api = builder.build();
    }
}