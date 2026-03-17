package de.tle.discord.cogs;

import de.tle.discord.db.Database;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Random;

public class GameCog extends ListenerAdapter {

    private final Random random = new Random();

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        if (!event.getName().equals("play")) return;

        String type = event.getOption("type").getAsString();
        long userId = event.getUser().getIdLong();

        createUser(userId);

        switch (type) {
            case "blackjack" -> blackjack(event, userId);
            case "dice" -> dice(event, userId);
            case "coinflip" -> coinflip(event, userId);
            case "roulette" -> roulette(event, userId);
            case "chickenfight" -> chickenfight(event, userId);
        }
    }

    // USER INIT

    private void createUser(long userId) {
        try {
            PreparedStatement ps = Database.getConnection().prepareStatement("""
                MERGE INTO users (id) KEY(id) VALUES (?)
            """);
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // BLACKJACK

    private void blackjack(SlashCommandInteractionEvent event, long userId) {
        int player = drawCard() + drawCard();
        int dealer = drawCard() + drawCard();

        while (player < 17) player += drawCard();
        while (dealer < 17) dealer += drawCard();

        String result;

        if (player > 21) result = "❌ Bust! You lose.";
        else if (dealer > 21 || player > dealer) {
            addWin(userId);
            result = "✅ You win!";
        } else if (player == dealer) result = "🤝 Draw.";
        else {
            addLoss(userId);
            result = "❌ You lose.";
        }

        event.reply("🃏 Blackjack\nYou: " + player + "\nDealer: " + dealer + "\n" + result).queue();
    }

    private int drawCard() {
        return random.nextInt(10) + 1;
    }

    // DICE

    private void dice(SlashCommandInteractionEvent event, long userId) {
        int player = random.nextInt(6) + 1;
        int bot = random.nextInt(6) + 1;

        String result;

        if (player > bot) {
            addWin(userId);
            result = "✅ You win!";
        } else if (player == bot) result = "🤝 Draw.";
        else {
            addLoss(userId);
            result = "❌ You lose.";
        }

        event.reply("🎲 Dice\nYou: " + player + "\nBot: " + bot + "\n" + result).queue();
    }

    // COINFLIP

    private void coinflip(SlashCommandInteractionEvent event, long userId) {
        boolean win = random.nextBoolean();

        if (win) {
            addWin(userId);
            event.reply("🪙 Heads! ✅ You win!").queue();
        } else {
            addLoss(userId);
            event.reply("🪙 Tails! ❌ You lose!").queue();
        }
    }

    // ROULETTE

    private void roulette(SlashCommandInteractionEvent event, long userId) {
        int number = random.nextInt(37); // 0-36

        if (number % 2 == 0) {
            addWin(userId);
            event.reply("🎡 Number: " + number + " (Even) ✅ You win!").queue();
        } else {
            addLoss(userId);
            event.reply("🎡 Number: " + number + " (Odd) ❌ You lose!").queue();
        }
    }

    // CHICKEN FIGHT

    private void chickenfight(SlashCommandInteractionEvent event, long userId) {
        int player = random.nextInt(100);
        int enemy = random.nextInt(100);

        String result;

        if (player > enemy) {
            addWin(userId);
            result = "🐔 You scared the opponent away!";
        } else {
            addLoss(userId);
            result = "💀 You chickened out!";
        }

        event.reply("🐔 Chicken Fight\nYou: " + player + "\nEnemy: " + enemy + "\n" + result).queue();
    }

    // DATABASE METHODS

    private void addWin(long userId) {
        updateStat(userId, "wins", 1);
        updateStat(userId, "cash", 100);
    }

    private void addLoss(long userId) {
        updateStat(userId, "losses", 1);
        updateStat(userId, "cash", -50);
    }

    private void updateStat(long userId, String column, int amount) {
        try {
            PreparedStatement ps = Database.getConnection().prepareStatement(
                    "UPDATE users SET " + column + " = " + column + " + ? WHERE id = ?"
            );
            ps.setInt(1, amount);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}