package de.tle.discord.cogs;

import de.tle.discord.db.Database;
import de.tle.discord.emojis.EmojiBalance;
import de.tle.discord.emojis.EmojiMatch;
import de.tle.discord.emojis.EmojiUser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.Color;
import java.sql.ResultSet;
import java.sql.Statement;

public class LeaderboardCog extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("leaderboard")) return;

        String type = event.getOption("type") != null ? event.getOption("type").getAsString() : "leaderboard";

        event.replyEmbeds(buildLeaderboard(type, event.getJDA()).build())
                .addComponents(ActionRow.of(
                        Button.primary("lb_leaderboard", "💰 Gesamt"),
                        Button.success("lb_alltime-wins-lb", EmojiMatch.match_win + " Siege"),
                        Button.danger("lb_alltime-lb", "📊 Balance"),
                        Button.secondary("lb_stash-lb", "🏦 Bank")
                )).queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String compId = event.getComponentId();
        if (!compId.startsWith("lb_")) return;

        String type = compId.replace("lb_", "");

        event.editMessageEmbeds(buildLeaderboard(type, event.getJDA()).build())
                .setComponents(ActionRow.of(
                        Button.primary("lb_leaderboard", "💰 Gesamt"),
                        Button.success("lb_alltime-wins-lb", EmojiMatch.match_win + " Siege"),
                        Button.danger("lb_alltime-lb", "📊 Balance"),
                        Button.secondary("lb_stash-lb", "🏦 Bank")
                )).queue();
    }

    private EmbedBuilder buildLeaderboard(String type, net.dv8tion.jda.api.JDA jda) {
        String query;
        String title;
        String valueColumn;
        String emoji;
        Color color;

        switch (type) {
            case "alltime-lb" -> {
                query = "SELECT id, cash FROM users ORDER BY cash DESC LIMIT 10";
                title = "📊 Cash Leaderboard";
                valueColumn = "cash";
                emoji = EmojiBalance.coin;
                color = new Color(255, 215, 0);
            }
            case "alltime-wins-lb" -> {
                query = "SELECT id, wins FROM users ORDER BY wins DESC LIMIT 10";
                title = EmojiMatch.match_win + " Wins Leaderboard";
                valueColumn = "wins";
                emoji = EmojiMatch.match_win;
                color = new Color(0, 200, 0);
            }
            case "stash-lb" -> {
                query = "SELECT id, bank FROM users ORDER BY bank DESC LIMIT 10";
                title = "🏦 Bank Leaderboard";
                valueColumn = "bank";
                emoji = EmojiBalance.balance;
                color = new Color(0, 100, 200);
            }
            default -> {
                query = "SELECT id, (cash + bank) AS total FROM users ORDER BY total DESC LIMIT 10";
                title = EmojiUser.owner_crown + " Gesamt Leaderboard";
                valueColumn = "total";
                emoji = "💎";
                color = new Color(255, 165, 0);
            }
        }

        EmbedBuilder eb = new EmbedBuilder()
                .setColor(color)
                .setTitle(title);

        try {
            Statement stmt = Database.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(query);

            StringBuilder sb = new StringBuilder();
            int rank = 0;

            while (rs.next()) {
                rank++;
                long id = rs.getLong("id");
                int value = rs.getInt(valueColumn);

                String rankEmoji = switch (rank) {
                    case 1 -> "🥇";
                    case 2 -> "🥈";
                    case 3 -> "🥉";
                    default -> "**#" + rank + "**";
                };

                String userName;
                try {
                    userName = jda.retrieveUserById(id).complete().getName();
                } catch (Exception e) {
                    userName = "User " + id;
                }

                sb.append(rankEmoji).append(" ")
                        .append(EmojiUser.player).append(" **").append(userName).append("** — ")
                        .append(emoji).append(" **").append(value).append("**\n");
            }

            if (rank == 0) {
                sb.append("*Noch keine Einträge vorhanden.*");
            }

            eb.setDescription(sb.toString());
            eb.setFooter("Top " + rank + " Spieler");

        } catch (Exception e) { e.printStackTrace(); }

        return eb;
    }
}