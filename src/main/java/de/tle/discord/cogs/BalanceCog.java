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
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class BalanceCog extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("balance")) return;

        long userId = event.getUser().getIdLong();
        String type = event.getOption("type") != null ? event.getOption("type").getAsString() : "all";

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
            int total = cash + bank;

            EmbedBuilder eb = new EmbedBuilder()
                    .setColor(new Color(255, 215, 0))
                    .setTitle(EmojiBalance.balance + " Balance von " + event.getUser().getName())
                    .setThumbnail(event.getUser().getEffectiveAvatarUrl());

            switch (type) {
                case "cash" -> eb.addField(EmojiBalance.coin + " Cash", "**" + cash + "**", false);
                case "bank" -> eb.addField(EmojiBalance.balance + " Bank", "**" + bank + "**", false);
                default -> {
                    eb.addField(EmojiBalance.coin + " Cash", "**" + cash + "**", true);
                    eb.addField(EmojiBalance.balance + " Bank", "**" + bank + "**", true);
                    eb.addField("💎 Gesamt", "**" + total + "**", true);

                    String bar = buildProgressBar(cash, total);
                    eb.addField("Verteilung", bar + "\n" + EmojiBalance.coin + " Cash: **" + pct(cash, total) + "%** | " + EmojiBalance.balance + " Bank: **" + pct(bank, total) + "%**", false);
                }
            }

            event.replyEmbeds(eb.build())
                    .addComponents(ActionRow.of(
                            Button.success("bal_deposit", "💰 Einzahlen"),
                            Button.danger("bal_withdraw", "💸 Abheben"),
                            Button.secondary("bal_refresh", "🔄 Aktualisieren")
                    )).queue();

        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String compId = event.getComponentId();
        if (!compId.startsWith("bal_")) return;

        long userId = event.getUser().getIdLong();

        switch (compId) {
            case "bal_deposit" -> handleTransfer(event, userId, "deposit");
            case "bal_withdraw" -> handleTransfer(event, userId, "withdraw");
            case "bal_refresh" -> handleRefresh(event, userId);
        }
    }

    private void handleTransfer(ButtonInteractionEvent event, long userId, String direction) {
        try {
            PreparedStatement ps = Database.getConnection().prepareStatement("SELECT * FROM users WHERE id = ?");
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return;

            int cash = rs.getInt("cash");
            int bank = rs.getInt("bank");

            if (direction.equals("deposit")) {
                int amount = (int) (cash * 0.5);
                if (amount < 1) {
                    event.reply("Nicht genug Cash zum Einzahlen!").setEphemeral(true).queue();
                    return;
                }
                transfer(userId, -amount, amount);
                event.editMessageEmbeds(buildBalanceEmbed(event.getUser().getName(), event.getUser().getEffectiveAvatarUrl(),
                        cash - amount, bank + amount,
                        EmojiBalance.balance + " **" + amount + "** eingezahlt!").build())
                        .setComponents(ActionRow.of(
                                Button.success("bal_deposit", "💰 Einzahlen"),
                                Button.danger("bal_withdraw", "💸 Abheben"),
                                Button.secondary("bal_refresh", "🔄 Aktualisieren")
                        )).queue();
            } else {
                int amount = (int) (bank * 0.5);
                if (amount < 1) {
                    event.reply("Nicht genug in der Bank!").setEphemeral(true).queue();
                    return;
                }
                transfer(userId, amount, -amount);
                event.editMessageEmbeds(buildBalanceEmbed(event.getUser().getName(), event.getUser().getEffectiveAvatarUrl(),
                        cash + amount, bank - amount,
                        EmojiBalance.coin + " **" + amount + "** abgehoben!").build())
                        .setComponents(ActionRow.of(
                                Button.success("bal_deposit", "💰 Einzahlen"),
                                Button.danger("bal_withdraw", "💸 Abheben"),
                                Button.secondary("bal_refresh", "🔄 Aktualisieren")
                        )).queue();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleRefresh(ButtonInteractionEvent event, long userId) {
        try {
            PreparedStatement ps = Database.getConnection().prepareStatement("SELECT * FROM users WHERE id = ?");
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return;

            int cash = rs.getInt("cash");
            int bank = rs.getInt("bank");

            event.editMessageEmbeds(buildBalanceEmbed(event.getUser().getName(), event.getUser().getEffectiveAvatarUrl(),
                    cash, bank, null).build())
                    .setComponents(ActionRow.of(
                            Button.success("bal_deposit", "💰 Einzahlen"),
                            Button.danger("bal_withdraw", "💸 Abheben"),
                            Button.secondary("bal_refresh", "🔄 Aktualisieren")
                    )).queue();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private EmbedBuilder buildBalanceEmbed(String name, String avatar, int cash, int bank, String footer) {
        int total = cash + bank;
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(new Color(255, 215, 0))
                .setTitle(EmojiBalance.balance + " Balance von " + name)
                .setThumbnail(avatar)
                .addField(EmojiBalance.coin + " Cash", "**" + cash + "**", true)
                .addField(EmojiBalance.balance + " Bank", "**" + bank + "**", true)
                .addField("💎 Gesamt", "**" + total + "**", true)
                .addField("Verteilung", buildProgressBar(cash, total) + "\n" + EmojiBalance.coin + " Cash: **" + pct(cash, total) + "%** | " + EmojiBalance.balance + " Bank: **" + pct(bank, total) + "%**", false);
        if (footer != null) eb.setFooter(footer);
        return eb;
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

    private String buildProgressBar(int cash, int total) {
        if (total == 0) return "▒▒▒▒▒▒▒▒▒▒";
        int filled = (int) Math.round((cash / (double) total) * 10);
        return "█".repeat(Math.max(0, filled)) + "░".repeat(Math.max(0, 10 - filled));
    }

    private int pct(int part, int total) {
        if (total == 0) return 0;
        return (int) Math.round((part / (double) total) * 100);
    }
}