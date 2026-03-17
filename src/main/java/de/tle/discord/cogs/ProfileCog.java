package de.tle.discord.cogs;

import de.tle.discord.db.Database;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ProfileCog extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        if (!event.getName().equals("profile")) return;

        long userId = event.getUser().getIdLong();

        try {
            PreparedStatement ps = Database.getConnection().prepareStatement(
                    "SELECT * FROM users WHERE id = ?"
            );
            ps.setLong(1, userId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int wins = rs.getInt("wins");
                int losses = rs.getInt("losses");

                event.reply("👤 Wins: " + wins + "\n❌ Losses: " + losses).queue();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}