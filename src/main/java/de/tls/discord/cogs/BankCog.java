package de.tls.discord.cogs;

import de.tls.discord.db.Database;
import de.tls.discord.emojis.EmojiBalance;
import de.tls.discord.emojis.EmojiMatch;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.Color;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class BankCog extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("deposit") && !event.getName().equals("withdraw")) return;

        long userId = event.getUser().getIdLong();
        int amount = event.getOption("amount").getAsInt();

        try {
            PreparedStatement ps = Database.getConnection().prepareStatement("SELECT * FROM users WHERE id = ?");
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                event.replyEmbeds(new EmbedBuilder()
                        .setColor(Color.RED)
                        .setTitle(EmojiMatch.match_lose + " Kein Profil!")
                        .setDescription("Spiele zuerst ein Spiel mit `/play`.")
                        .build()).setEphemeral(true).queue();
                return;
            }

            int cash = rs.getInt("cash");
            int bank = rs.getInt("bank");

            if (event.getName().equals("deposit")) {
                if (amount > cash) {
                    event.replyEmbeds(new EmbedBuilder()
                            .setColor(Color.RED)
                            .setTitle(EmojiMatch.match_lose + " Nicht genug Cash!")
                            .setDescription("Du hast nur **" + cash + "** " + EmojiBalance.coin + " Cash.")
                            .build()).setEphemeral(true).queue();
                    return;
                }
                transfer(userId, -amount, amount);
                event.replyEmbeds(new EmbedBuilder()
                        .setColor(Color.GREEN)
                        .setTitle("🏦 Einzahlung erfolgreich!")
                        .setDescription("**" + amount + "** " + EmojiBalance.coin + " wurden in die Bank eingezahlt.")
                        .addField(EmojiBalance.coin + " Cash", "**" + (cash - amount) + "**", true)
                        .addField(EmojiBalance.balance + " Bank", "**" + (bank + amount) + "**", true)
                        .addField("💎 Gesamt", "**" + (cash + bank) + "**", true)
                        .build()).queue();
            } else {
                if (amount > bank) {
                    event.replyEmbeds(new EmbedBuilder()
                            .setColor(Color.RED)
                            .setTitle(EmojiMatch.match_lose + " Nicht genug in der Bank!")
                            .setDescription("Du hast nur **" + bank + "** " + EmojiBalance.balance + " in der Bank.")
                            .build()).setEphemeral(true).queue();
                    return;
                }
                transfer(userId, amount, -amount);
                event.replyEmbeds(new EmbedBuilder()
                        .setColor(Color.GREEN)
                        .setTitle("💸 Abhebung erfolgreich!")
                        .setDescription("**" + amount + "** " + EmojiBalance.coin + " wurden von der Bank abgehoben.")
                        .addField(EmojiBalance.coin + " Cash", "**" + (cash + amount) + "**", true)
                        .addField(EmojiBalance.balance + " Bank", "**" + (bank - amount) + "**", true)
                        .addField("💎 Gesamt", "**" + (cash + bank) + "**", true)
                        .build()).queue();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void transfer(long userId, int cashDelta, int bankDelta) {
        try {
            PreparedStatement ps = Database.getConnection().prepareStatement("UPDATE users SET cash = cash + ?, bank = bank + ? WHERE id = ?");
            ps.setInt(1, cashDelta);
            ps.setInt(2, bankDelta);
            ps.setLong(3, userId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
