package de.tle.discord.cogs;

import de.tle.discord.db.Database;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.ResultSet;
import java.sql.Statement;

public class LeaderboardCog extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        if (!event.getName().equals("leaderboard")) return;

        try {
            Statement stmt = Database.getConnection().createStatement();

            ResultSet rs = stmt.executeQuery("""
                SELECT id, cash FROM users ORDER BY cash DESC LIMIT 10
            """);

            StringBuilder sb = new StringBuilder("🏆 Leaderboard:\n");

            while (rs.next()) {
                sb.append(rs.getLong("id"))
                        .append(" - ")
                        .append(rs.getInt("cash"))
                        .append("\n");
            }

            event.reply(sb.toString()).queue();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}