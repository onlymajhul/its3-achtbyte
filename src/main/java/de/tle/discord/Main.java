package de.tle.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import io.github.cdimascio.dotenv.Dotenv;

public class Main {
    public static void main(String[] arguments) throws Exception {
        Dotenv dotenv = Dotenv.load();
        String token = dotenv.get("TOKEN");
        JDA api = JDABuilder.createDefault(token).build();
    }
}