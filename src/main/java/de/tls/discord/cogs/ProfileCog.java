package de.tls.discord.cogs;

import de.tls.discord.db.Database;
import de.tls.discord.emojis.EmojiBalance;
import de.tls.discord.emojis.EmojiMatch;
import de.tls.discord.emojis.EmojiUser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.Color;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ProfileCog extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("profile")) return;

        long userId = event.getUser().getIdLong();
        User user = event.getUser();

        try {
            PreparedStatement ps = Database.getConnection().prepareStatement("SELECT * FROM users WHERE id = ?");
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                event.replyEmbeds(new EmbedBuilder()
                        .setColor(Color.RED)
                        .setTitle(EmojiMatch.match_lose + " Kein Profil gefunden!")
                        .setDescription("Spiele zuerst ein Spiel mit `/play` um dein Profil zu erstellen.")
                        .build()).setEphemeral(true).queue();
                return;
            }

            int cash = rs.getInt("cash");
            int bank = rs.getInt("bank");
            int wins = rs.getInt("wins");
            int losses = rs.getInt("losses");

            event.replyEmbeds(buildProfileEmbed(user, cash, bank, wins, losses).build())
                    .addComponents(ActionRow.of(
                            Button.primary("prof_stats", "📊 Detaillierte Stats"),
                            Button.secondary("prof_refresh", "🔄 Aktualisieren")
                    )).queue();

        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String compId = event.getComponentId();
        if (!compId.startsWith("prof_")) return;

        long userId = event.getUser().getIdLong();

        switch (compId) {
            case "prof_stats" -> showDetailedStats(event, userId);
            case "prof_refresh" -> refreshProfile(event, userId);
            case "prof_back" -> refreshProfile(event, userId);
        }
    }

    private void showDetailedStats(ButtonInteractionEvent event, long userId) {
        try {
            PreparedStatement ps = Database.getConnection().prepareStatement("SELECT * FROM users WHERE id = ?");
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return;

            int cash = rs.getInt("cash");
            int bank = rs.getInt("bank");
            int wins = rs.getInt("wins");
            int losses = rs.getInt("losses");
            int total = wins + losses;
            double winrate = total > 0 ? (wins / (double) total) * 100 : 0;
            int netProfit = (cash + bank) - 1000;

            PreparedStatement rankPs = Database.getConnection().prepareStatement(
                    "SELECT COUNT(*) + 1 AS rank FROM users WHERE (cash + bank) > (SELECT cash + bank FROM users WHERE id = ?)"
            );
            rankPs.setLong(1, userId);
            ResultSet rankRs = rankPs.executeQuery();
            int rank = rankRs.next() ? rankRs.getInt("rank") : 0;

            String rankEmoji = switch (rank) {
                case 1 -> "🥇";
                case 2 -> "🥈";
                case 3 -> "🥉";
                default -> "🏅";
            };

            EmbedBuilder eb = new EmbedBuilder()
                    .setColor(new Color(138, 43, 226))
                    .setTitle("📊 Detaillierte Stats - " + event.getUser().getName())
                    .setThumbnail(event.getUser().getEffectiveAvatarUrl())
                    .addField(EmojiMatch.match_win + " Siege", "**" + wins + "**", true)
                    .addField(EmojiMatch.match_lose + " Niederlagen", "**" + losses + "**", true)
                    .addField(EmojiMatch.matches + " Gesamt", "**" + total + "**", true)
                    .addField("📈 Winrate", "**" + String.format("%.1f", winrate) + "%**\n" + buildWinrateBar(winrate), false)
                    .addField(rankEmoji + " Server Rang", "**#" + rank + "**", true)
                    .addField(netProfit >= 0 ? "📈 Profit" : "📉 Verlust", "**" + (netProfit >= 0 ? "+" : "") + netProfit + "** " + EmojiBalance.coin, true)
                    .addField(EmojiBalance.coin + " Cash", "**" + cash + "**", true)
                    .addField(EmojiBalance.balance + " Bank", "**" + bank + "**", true);

            event.editMessageEmbeds(eb.build())
                    .setComponents(ActionRow.of(
                            Button.secondary("prof_back", "⬅️ Zurück")
                    )).queue();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void refreshProfile(ButtonInteractionEvent event, long userId) {
        try {
            PreparedStatement ps = Database.getConnection().prepareStatement("SELECT * FROM users WHERE id = ?");
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return;

            event.editMessageEmbeds(buildProfileEmbed(event.getUser(),
                    rs.getInt("cash"), rs.getInt("bank"),
                    rs.getInt("wins"), rs.getInt("losses")).build())
                    .setComponents(ActionRow.of(
                            Button.primary("prof_stats", "📊 Detaillierte Stats"),
                            Button.secondary("prof_refresh", "🔄 Aktualisieren")
                    )).queue();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private EmbedBuilder buildProfileEmbed(User user, int cash, int bank, int wins, int losses) {
        int total = wins + losses;
        double winrate = total > 0 ? (wins / (double) total) * 100 : 0;
        int totalMoney = cash + bank;

        return new EmbedBuilder()
                .setColor(new Color(0, 191, 255))
                .setTitle(EmojiUser.player + " Profil von " + user.getName())
                .setThumbnail(user.getEffectiveAvatarUrl())
                .addField(EmojiBalance.coin + " Cash", "**" + cash + "**", true)
                .addField(EmojiBalance.balance + " Bank", "**" + bank + "**", true)
                .addField("💎 Gesamt", "**" + totalMoney + "**", true)
                .addField(EmojiMatch.match_win + " Siege", "**" + wins + "**", true)
                .addField(EmojiMatch.match_lose + " Niederlagen", "**" + losses + "**", true)
                .addField("📈 Winrate", "**" + String.format("%.1f", winrate) + "%**", true)
                .addField(EmojiUser.user_id + " ID", "`" + user.getId() + "`", false);
    }

    private String buildWinrateBar(double winrate) {
        int filled = (int) Math.round(winrate / 10);
        return "🟩".repeat(Math.max(0, filled)) + "🟥".repeat(Math.max(0, 10 - filled)) + " " + String.format("%.1f", winrate) + "%";
    }
}