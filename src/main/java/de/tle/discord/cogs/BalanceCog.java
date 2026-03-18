package de.tle.discord.cogs;

import de.tle.discord.db.Database;
import de.tle.discord.emojis.EmojiBalance;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.ResultSet;
import java.sql.PreparedStatement;

public class BalanceCog extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        if (!event.getName().equals("balance")) return;

        long userId = event.getUser().getIdLong();

        try {
            PreparedStatement ps = Database.getConnection().prepareStatement(
                    "SELECT * FROM users WHERE id = ?"
            );
            ps.setLong(1, userId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int cash = rs.getInt("cash");
                int bank = rs.getInt("bank");

                event.reply(EmojiBalance.coin + " Cash: " + cash + "\n" + EmojiBalance.balance + " Bank: " + bank).queue();
            } else {
                event.reply("No data found.").queue();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}