package de.tle.discord.cogs;

import de.tle.discord.db.Database;
import de.tle.discord.emojis.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.Color;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameCog extends ListenerAdapter {

    private final Random random = new Random();

    private static class BlackjackSession {
        List<Integer> playerCards = new ArrayList<>();
        List<Integer> dealerCards = new ArrayList<>();
        long userId;
        long bet;
        boolean doubled = false;
        List<Integer> splitHand = null;
        boolean isSplit = false;
        boolean playingSplitHand = false;
        long splitBet = 0;
    }

    private static class ChickenSession {
        long userId;
        long bet;
        int wins = 0;
        double multiplier = 1.0;
    }

    private record RouletteBet(long userId, long bet) {}
    private record CoinflipBet(long userId, long bet) {}
    private record DiceBet(long userId, long bet) {}

    private final Map<String, BlackjackSession> blackjackSessions = new ConcurrentHashMap<>();
    private final Map<String, ChickenSession> chickenSessions = new ConcurrentHashMap<>();
    private final Map<String, RouletteBet> rouletteBets = new ConcurrentHashMap<>();
    private final Map<String, CoinflipBet> coinflipBets = new ConcurrentHashMap<>();
    private final Map<String, DiceBet> diceBets = new ConcurrentHashMap<>();

    private static final Set<Integer> RED_NUMBERS = Set.of(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36);

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("play")) return;

        String type = event.getOption("type").getAsString();
        long userId = event.getUser().getIdLong();
        long bet = event.getOption("bet").getAsLong();

        createUser(userId);

        int cash = getCash(userId);
        if (cash < bet) {
            event.replyEmbeds(new EmbedBuilder()
                    .setColor(Color.RED)
                    .setTitle(EmojiMatch.match_lose + " Nicht genug Cash!")
                    .setDescription("Du hast **" + cash + "** " + EmojiBalance.coin + " aber brauchst **" + bet + "** " + EmojiBalance.coin)
                    .build()).setEphemeral(true).queue();
            return;
        }

        switch (type) {
            case "blackjack" -> blackjack(event, userId, bet);
            case "dice" -> dice(event, userId, bet);
            case "coinflip" -> coinflip(event, userId, bet);
            case "roulette" -> roulette(event, userId, bet);
            case "chickenfight" -> chickenfight(event, userId, bet);
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String compId = event.getComponentId();
        String key = event.getUser().getId();

        if (compId.startsWith("bj_")) {
            BlackjackSession s = blackjackSessions.get(key);
            if (s == null || s.userId != event.getUser().getIdLong()) {
                event.reply("Das ist nicht dein Spiel!").setEphemeral(true).queue();
                return;
            }
            switch (compId) {
                case "bj_hit" -> bjHit(event, s, key);
                case "bj_stand" -> bjStand(event, s, key);
                case "bj_double" -> bjDouble(event, s, key);
                case "bj_split" -> bjSplit(event, s, key);
            }
        }

        if (compId.startsWith("rl_")) handleRouletteBet(event, compId);
        if (compId.startsWith("cf_")) handleCoinflipChoice(event, compId);
        if (compId.startsWith("dice_")) handleDiceChoice(event, compId);

        if (compId.startsWith("ck_")) {
            ChickenSession cs = chickenSessions.get(key);
            if (cs == null || cs.userId != event.getUser().getIdLong()) {
                event.reply("Das ist nicht dein Spiel!").setEphemeral(true).queue();
                return;
            }
            if (compId.equals("ck_fight")) chickenFightRound(event, cs, key);
            else if (compId.equals("ck_cashout")) chickenCashout(event, cs, key);
        }
    }

    private void blackjack(SlashCommandInteractionEvent event, long userId, long bet) {
        BlackjackSession s = new BlackjackSession();
        s.userId = userId;
        s.bet = bet;
        s.playerCards.add(drawCard());
        s.playerCards.add(drawCard());
        s.dealerCards.add(drawCard());
        s.dealerCards.add(drawCard());

        String key = String.valueOf(userId);
        blackjackSessions.put(key, s);

        if (handValue(s.playerCards) == 21) {
            long winnings = (long) (bet * 2.5);
            updateCash(userId, (int) winnings);
            addWin(userId);
            blackjackSessions.remove(key);

            event.replyEmbeds(new EmbedBuilder()
                    .setColor(Color.YELLOW)
                    .setTitle("🃏 Blackjack - BLACKJACK!")
                    .setDescription(EmojiMatch.match_win + " **BLACKJACK!** Du gewinnst **" + winnings + "** " + EmojiBalance.coin)
                    .addField("Deine Hand " + cardsToEmoji(s.playerCards), "Wert: **" + handValue(s.playerCards) + "**", true)
                    .addField("Dealer Hand " + cardsToEmoji(s.dealerCards), "Wert: **" + handValue(s.dealerCards) + "**", true)
                    .addField("Balance", EmojiBalance.coin + " **" + getCash(userId) + "** | " + EmojiBalance.balance + " **" + getBank(userId) + "**", false)
                    .build()).queue();
            return;
        }

        updateCash(userId, (int) -bet);

        boolean canSplit = s.playerCards.size() == 2 && cardValue(s.playerCards.get(0)) == cardValue(s.playerCards.get(1));
        boolean canDouble = getCash(userId) >= bet;

        List<Button> buttons = new ArrayList<>();
        buttons.add(Button.success("bj_hit", "Hit 🃏"));
        buttons.add(Button.danger("bj_stand", "Stand ✋"));
        if (canDouble) buttons.add(Button.primary("bj_double", "Double ⏫"));
        if (canSplit && canDouble) buttons.add(Button.secondary("bj_split", "Split ✂️"));

        event.replyEmbeds(bjEmbed(s, "Dein Zug!", false).build())
                .addComponents(ActionRow.of(buttons)).queue();
    }

    private void bjHit(ButtonInteractionEvent event, BlackjackSession s, String key) {
        List<Integer> hand = s.isSplit && s.playingSplitHand ? s.splitHand : s.playerCards;
        hand.add(drawCard());

        if (handValue(hand) > 21) {
            if (s.isSplit && !s.playingSplitHand) {
                s.playingSplitHand = true;
                event.editMessageEmbeds(bjEmbed(s, "Erste Hand Bust! Spiele Split-Hand...", false).build())
                        .setComponents(ActionRow.of(
                                Button.success("bj_hit", "Hit 🃏"),
                                Button.danger("bj_stand", "Stand ✋")
                        )).queue();
                return;
            }
            addLoss(s.userId);
            blackjackSessions.remove(key);
            event.editMessageEmbeds(bjEmbed(s, EmojiMatch.match_lose + " **Bust!** Du hast **" + s.bet + "** " + EmojiBalance.coin + " verloren!", true).build())
                    .setComponents(Collections.emptyList()).queue();
            return;
        }

        if (handValue(hand) == 21) {
            bjStand(event, s, key);
            return;
        }

        event.editMessageEmbeds(bjEmbed(s, "Dein Zug!", false).build())
                .setComponents(ActionRow.of(
                        Button.success("bj_hit", "Hit 🃏"),
                        Button.danger("bj_stand", "Stand ✋")
                )).queue();
    }

    private void bjStand(ButtonInteractionEvent event, BlackjackSession s, String key) {
        if (s.isSplit && !s.playingSplitHand) {
            s.playingSplitHand = true;
            event.editMessageEmbeds(bjEmbed(s, "Spiele jetzt Split-Hand...", false).build())
                    .setComponents(ActionRow.of(
                            Button.success("bj_hit", "Hit 🃏"),
                            Button.danger("bj_stand", "Stand ✋")
                    )).queue();
            return;
        }

        while (handValue(s.dealerCards) < 17) s.dealerCards.add(drawCard());

        StringBuilder resultText = new StringBuilder();
        long mainBet = s.doubled ? s.bet * 2 : s.bet;
        resultText.append(evaluateHand("Haupthand", handValue(s.playerCards), handValue(s.dealerCards), mainBet, s));

        if (s.isSplit && s.splitHand != null) {
            resultText.append("\n").append(evaluateHand("Split-Hand", handValue(s.splitHand), handValue(s.dealerCards), s.splitBet, s));
        }

        blackjackSessions.remove(key);
        event.editMessageEmbeds(bjEmbed(s, resultText.toString(), true).build())
                .setComponents(Collections.emptyList()).queue();
    }

    private String evaluateHand(String handName, int playerVal, int dealerVal, long bet, BlackjackSession s) {
        if (playerVal > 21) {
            addLoss(s.userId);
            return EmojiMatch.match_lose + " **" + handName + "**: Bust! **-" + bet + "** " + EmojiBalance.coin;
        } else if (dealerVal > 21 || playerVal > dealerVal) {
            long win = bet * 2;
            updateCash(s.userId, (int) win);
            addWin(s.userId);
            return EmojiMatch.match_win + " **" + handName + "**: Gewinn! **+" + win + "** " + EmojiBalance.coin;
        } else if (playerVal == dealerVal) {
            updateCash(s.userId, (int) bet);
            return "🤝 **" + handName + "**: Unentschieden! Einsatz zurück.";
        } else {
            addLoss(s.userId);
            return EmojiMatch.match_lose + " **" + handName + "**: Verloren! **-" + bet + "** " + EmojiBalance.coin;
        }
    }

    private void bjDouble(ButtonInteractionEvent event, BlackjackSession s, String key) {
        if (getCash(s.userId) < s.bet) {
            event.reply("Nicht genug Cash zum Verdoppeln!").setEphemeral(true).queue();
            return;
        }
        updateCash(s.userId, (int) -s.bet);
        s.doubled = true;
        s.playerCards.add(drawCard());
        bjStand(event, s, key);
    }

    private void bjSplit(ButtonInteractionEvent event, BlackjackSession s, String key) {
        if (s.playerCards.size() != 2 || cardValue(s.playerCards.get(0)) != cardValue(s.playerCards.get(1))) {
            event.reply("Du kannst nicht splitten!").setEphemeral(true).queue();
            return;
        }
        if (getCash(s.userId) < s.bet) {
            event.reply("Nicht genug Cash zum Splitten!").setEphemeral(true).queue();
            return;
        }

        updateCash(s.userId, (int) -s.bet);
        s.isSplit = true;
        s.splitBet = s.bet;
        s.splitHand = new ArrayList<>();
        s.splitHand.add(s.playerCards.remove(1));
        s.playerCards.add(drawCard());
        s.splitHand.add(drawCard());
        s.playingSplitHand = false;

        event.editMessageEmbeds(bjEmbed(s, "Split! Spiele zuerst die Haupthand.", false).build())
                .setComponents(ActionRow.of(
                        Button.success("bj_hit", "Hit 🃏"),
                        Button.danger("bj_stand", "Stand ✋")
                )).queue();
    }

    private EmbedBuilder bjEmbed(BlackjackSession s, String status, boolean showDealer) {
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(new Color(0, 153, 51))
                .setTitle("🃏 Blackjack")
                .addField("Deine Hand " + cardsToEmoji(s.playerCards), "Wert: **" + handValue(s.playerCards) + "**", true);

        if (showDealer) {
            eb.addField("Dealer " + cardsToEmoji(s.dealerCards), "Wert: **" + handValue(s.dealerCards) + "**", true);
        } else {
            eb.addField("Dealer 🂠 " + cardToEmoji(s.dealerCards.get(0)), "Wert: **?**", true);
        }

        eb.addField("Einsatz", "**" + (s.doubled ? s.bet * 2 : s.bet) + "** " + EmojiBalance.coin, false);

        if (s.isSplit && s.splitHand != null) {
            eb.addField("Split-Hand " + cardsToEmoji(s.splitHand), "Wert: **" + handValue(s.splitHand) + "**", false);
        }

        eb.addField("Balance", EmojiBalance.coin + " **" + getCash(s.userId) + "** | " + EmojiBalance.balance + " **" + getBank(s.userId) + "**", false);
        eb.setFooter(status);
        return eb;
    }

    private int drawCard() {
        return random.nextInt(13) + 1;
    }

    private int cardValue(int card) {
        if (card >= 10) return 10;
        return card;
    }

    private int handValue(List<Integer> cards) {
        int total = 0;
        int aces = 0;
        for (int c : cards) {
            if (c == 1) { aces++; total += 11; }
            else total += cardValue(c);
        }
        while (total > 21 && aces > 0) { total -= 10; aces--; }
        return total;
    }

    private String cardToEmoji(int card) {
        return switch (card) {
            case 1 -> "🅰️"; case 2 -> "2️⃣"; case 3 -> "3️⃣"; case 4 -> "4️⃣"; case 5 -> "5️⃣";
            case 6 -> "6️⃣"; case 7 -> "7️⃣"; case 8 -> "8️⃣"; case 9 -> "9️⃣"; case 10 -> "🔟";
            case 11 -> "🇯"; case 12 -> "🇶"; case 13 -> "🇰"; default -> "❓";
        };
    }

    private String cardsToEmoji(List<Integer> cards) {
        StringBuilder sb = new StringBuilder();
        for (int c : cards) sb.append(cardToEmoji(c)).append(" ");
        return sb.toString().trim();
    }

    private void roulette(SlashCommandInteractionEvent event, long userId, long bet) {
        updateCash(userId, (int) -bet);
        rouletteBets.put(String.valueOf(userId), new RouletteBet(userId, bet));

        event.replyEmbeds(new EmbedBuilder()
                        .setColor(new Color(0, 100, 0))
                        .setTitle("🎰 Roulette")
                        .setDescription("Wähle deine Wette! Einsatz: **" + bet + "** " + EmojiBalance.coin)
                        .addField("Farben", "🔴 Rot (2x) | ⚫ Schwarz (2x) | 🟢 Grün/0 (36x)", false)
                        .addField("Bereiche", "1-12 (3x) | 13-24 (3x) | 25-36 (3x)", false)
                        .addField("Gerade/Ungerade", "Even (2x) | Odd (2x)", false)
                        .addField("Balance", EmojiBalance.coin + " **" + getCash(userId) + "** | " + EmojiBalance.balance + " **" + getBank(userId) + "**", false)
                        .build())
                .addComponents(
                        ActionRow.of(
                                Button.danger("rl_red", "Rot 🔴"),
                                Button.secondary("rl_black", "Schwarz ⚫"),
                                Button.success("rl_green", "Grün 🟢")
                        ),
                        ActionRow.of(
                                Button.primary("rl_1-12", "1-12"),
                                Button.primary("rl_13-24", "13-24"),
                                Button.primary("rl_25-36", "25-36")
                        ),
                        ActionRow.of(
                                Button.secondary("rl_even", "Even"),
                                Button.secondary("rl_odd", "Odd")
                        )
                ).queue();
    }

    private void handleRouletteBet(ButtonInteractionEvent event, String compId) {
        String key = event.getUser().getId();
        RouletteBet rb = rouletteBets.remove(key);
        if (rb == null || rb.userId != event.getUser().getIdLong()) {
            event.reply("Das ist nicht dein Spiel!").setEphemeral(true).queue();
            return;
        }

        int number = random.nextInt(37);
        boolean isRed = RED_NUMBERS.contains(number);
        String betType = compId.replace("rl_", "");

        boolean won = false;
        int multiplier = 1;

        switch (betType) {
            case "red" -> { won = isRed; multiplier = 2; }
            case "black" -> { won = number != 0 && !isRed; multiplier = 2; }
            case "green" -> { won = number == 0; multiplier = 36; }
            case "1-12" -> { won = number >= 1 && number <= 12; multiplier = 3; }
            case "13-24" -> { won = number >= 13 && number <= 24; multiplier = 3; }
            case "25-36" -> { won = number >= 25 && number <= 36; multiplier = 3; }
            case "even" -> { won = number != 0 && number % 2 == 0; multiplier = 2; }
            case "odd" -> { won = number % 2 == 1; multiplier = 2; }
        }

        String colorEmoji = number == 0 ? "🟢" : isRed ? "🔴" : "⚫";
        long payout = won ? rb.bet * multiplier : 0;

        if (won) { updateCash(rb.userId, (int) payout); addWin(rb.userId); }
        else addLoss(rb.userId);

        event.editMessageEmbeds(new EmbedBuilder()
                .setColor(won ? Color.GREEN : Color.RED)
                .setTitle("🎰 Roulette - Ergebnis")
                .addField("Zahl", colorEmoji + " **" + number + "**", true)
                .addField("Deine Wette", betType.toUpperCase(), true)
                .addField("Multiplikator", "**" + multiplier + "x**", true)
                .addField("Ergebnis", won
                        ? EmojiMatch.match_win + " **Gewinn! +" + payout + "** " + EmojiBalance.coin
                        : EmojiMatch.match_lose + " **Verloren! -" + rb.bet + "** " + EmojiBalance.coin, false)
                .addField("Balance", EmojiBalance.coin + " **" + getCash(rb.userId) + "** | " + EmojiBalance.balance + " **" + getBank(rb.userId) + "**", false)
                .build()).setComponents(Collections.emptyList()).queue();
    }

    private void coinflip(SlashCommandInteractionEvent event, long userId, long bet) {
        updateCash(userId, (int) -bet);
        coinflipBets.put(String.valueOf(userId), new CoinflipBet(userId, bet));

        event.replyEmbeds(new EmbedBuilder()
                        .setColor(Color.ORANGE)
                        .setTitle(EmojiBalance.coin + " Coinflip")
                        .setDescription("Wähle deine Seite! Einsatz: **" + bet + "** " + EmojiBalance.coin + "\n50/50 Chance – **2x** Gewinn!")
                        .addField("Balance", EmojiBalance.coin + " **" + getCash(userId) + "** | " + EmojiBalance.balance + " **" + getBank(userId) + "**", false)
                        .build())
                .addComponents(ActionRow.of(
                        Button.primary("cf_heads", "Heads 👑"),
                        Button.danger("cf_tails", "Tails 🦅")
                )).queue();
    }

    private void handleCoinflipChoice(ButtonInteractionEvent event, String compId) {
        String key = event.getUser().getId();
        CoinflipBet cb = coinflipBets.remove(key);
        if (cb == null || cb.userId != event.getUser().getIdLong()) {
            event.reply("Das ist nicht dein Spiel!").setEphemeral(true).queue();
            return;
        }

        boolean isHeads = random.nextBoolean();
        boolean choseHeads = compId.equals("cf_heads");
        boolean won = isHeads == choseHeads;
        long payout = won ? cb.bet * 2 : 0;

        if (won) { updateCash(cb.userId, (int) payout); addWin(cb.userId); }
        else addLoss(cb.userId);

        event.editMessageEmbeds(new EmbedBuilder()
                .setColor(won ? Color.GREEN : Color.RED)
                .setTitle(EmojiBalance.coin + " Coinflip - Ergebnis")
                .addField("Ergebnis", isHeads ? "👑 Heads" : "🦅 Tails", true)
                .addField("Deine Wahl", choseHeads ? "👑 Heads" : "🦅 Tails", true)
                .addField(won ? "Gewinn!" : "Verloren!", won
                        ? EmojiMatch.match_win + " **+" + payout + "** " + EmojiBalance.coin
                        : EmojiMatch.match_lose + " **-" + cb.bet + "** " + EmojiBalance.coin, false)
                .addField("Balance", EmojiBalance.coin + " **" + getCash(cb.userId) + "** | " + EmojiBalance.balance + " **" + getBank(cb.userId) + "**", false)
                .build()).setComponents(Collections.emptyList()).queue();
    }

    private void dice(SlashCommandInteractionEvent event, long userId, long bet) {
        updateCash(userId, (int) -bet);
        diceBets.put(String.valueOf(userId), new DiceBet(userId, bet));

        event.replyEmbeds(new EmbedBuilder()
                        .setColor(new Color(255, 165, 0))
                        .setTitle("🎲 Dice Roll (1-100)")
                        .setDescription("Wähle einen Bereich! Je kleiner der Bereich, desto höher der Gewinn!\nEinsatz: **" + bet + "** " + EmojiBalance.coin)
                        .addField("Multiplikatoren",
                                "`1-50` → **1.9x** | `40-100` → **1.6x** | `30-100` → **1.4x**\n" +
                                        "`20-100` → **1.2x** | `90-100` → **9.0x** | `95-100` → **16.0x**", false)
                        .addField("Balance", EmojiBalance.coin + " **" + getCash(userId) + "** | " + EmojiBalance.balance + " **" + getBank(userId) + "**", false)
                        .build())
                .addComponents(
                        ActionRow.of(
                                Button.success("dice_1-50", "1-50 (1.9x)"),
                                Button.primary("dice_40-100", "40-100 (1.6x)"),
                                Button.primary("dice_30-100", "30-100 (1.4x)")
                        ),
                        ActionRow.of(
                                Button.secondary("dice_20-100", "20-100 (1.2x)"),
                                Button.danger("dice_90-100", "90-100 (9.0x)"),
                                Button.danger("dice_95-100", "95-100 (16.0x)")
                        )
                ).queue();
    }

    private void handleDiceChoice(ButtonInteractionEvent event, String compId) {
        String key = event.getUser().getId();
        DiceBet db = diceBets.remove(key);
        if (db == null || db.userId != event.getUser().getIdLong()) {
            event.reply("Das ist nicht dein Spiel!").setEphemeral(true).queue();
            return;
        }

        int roll = random.nextInt(100) + 1;
        String range = compId.replace("dice_", "");
        String[] parts = range.split("-");
        int low = Integer.parseInt(parts[0]);
        int high = Integer.parseInt(parts[1]);

        double mult = calcDiceMult(low, high);
        boolean won = roll >= low && roll <= high;
        long payout = won ? (long) (db.bet * mult) : 0;

        if (won) { updateCash(db.userId, (int) payout); addWin(db.userId); }
        else addLoss(db.userId);

        event.editMessageEmbeds(new EmbedBuilder()
                .setColor(won ? Color.GREEN : Color.RED)
                .setTitle("🎲 Dice Roll - Ergebnis")
                .addField("Gewürfelt", "**" + roll + "**", true)
                .addField("Dein Bereich", "**" + low + "-" + high + "**", true)
                .addField("Multiplikator", "**" + mult + "x**", true)
                .addField("Ergebnis", won
                        ? EmojiMatch.match_win + " **Gewinn! +" + payout + "** " + EmojiBalance.coin
                        : EmojiMatch.match_lose + " **Verloren! -" + db.bet + "** " + EmojiBalance.coin, false)
                .addField("Balance", EmojiBalance.coin + " **" + getCash(db.userId) + "** | " + EmojiBalance.balance + " **" + getBank(db.userId) + "**", false)
                .build()).setComponents(Collections.emptyList()).queue();
    }

    private double calcDiceMult(int low, int high) {
        int rangeSize = high - low + 1;
        double mult = Math.round((95.0 / rangeSize) * 10.0) / 10.0;
        return Math.max(mult, 1.1);
    }

    private void chickenfight(SlashCommandInteractionEvent event, long userId, long bet) {
        updateCash(userId, (int) -bet);

        ChickenSession cs = new ChickenSession();
        cs.userId = userId;
        cs.bet = bet;
        chickenSessions.put(String.valueOf(userId), cs);

        event.replyEmbeds(new EmbedBuilder()
                        .setColor(Color.ORANGE)
                        .setTitle("🐔 Chicken Fight")
                        .setDescription("Dein Huhn ist bereit!\nEinsatz: **" + bet + "** " + EmojiBalance.coin
                                + "\nMultiplikator: **1.0x** | Gewinnchance: **65%**")
                        .addField("Balance", EmojiBalance.coin + " **" + getCash(userId) + "** | " + EmojiBalance.balance + " **" + getBank(userId) + "**", false)
                        .build())
                .addComponents(ActionRow.of(
                        Button.danger("ck_fight", "⚔️ Kämpfen!"),
                        Button.success("ck_cashout", "💰 Auscashen")
                )).queue();
    }

    private void chickenFightRound(ButtonInteractionEvent event, ChickenSession cs, String key) {
        double winChance = Math.max(0.15, 0.65 - (cs.wins * 0.10));
        boolean won = random.nextDouble() < winChance;

        if (won) {
            cs.wins++;
            cs.multiplier = Math.round((1.0 + cs.wins * 0.5 + cs.wins * cs.wins * 0.1) * 10.0) / 10.0;
            double nextChance = Math.max(0.15, 0.65 - (cs.wins * 0.10)) * 100;

            event.editMessageEmbeds(new EmbedBuilder()
                            .setColor(Color.YELLOW)
                            .setTitle("🐔 Chicken Fight - Runde " + cs.wins)
                            .setDescription(EmojiMatch.match_kills + " **Dein Huhn hat gewonnen!**"
                                    + "\n\nSiege: **" + cs.wins + "** | Multiplikator: **" + cs.multiplier + "x**"
                                    + "\nMöglicher Gewinn: **" + (long)(cs.bet * cs.multiplier) + "** " + EmojiBalance.coin
                                    + "\nNächste Gewinnchance: **" + String.format("%.0f", nextChance) + "%**")
                            .addField("Balance", EmojiBalance.coin + " **" + getCash(cs.userId) + "** | " + EmojiBalance.balance + " **" + getBank(cs.userId) + "**", false)
                            .build())
                    .setComponents(ActionRow.of(
                            Button.danger("ck_fight", "⚔️ Weiter!"),
                            Button.success("ck_cashout", "💰 " + (long)(cs.bet * cs.multiplier) + " auscashen")
                    )).queue();
        } else {
            addLoss(cs.userId);
            chickenSessions.remove(key);

            event.editMessageEmbeds(new EmbedBuilder()
                    .setColor(Color.RED)
                    .setTitle("🐔 Chicken Fight - Verloren!")
                    .setDescription(EmojiMatch.match_death + " **Dein Huhn wurde besiegt!**"
                            + "\n\nSiege davor: **" + cs.wins + "** | Verloren: **" + cs.bet + "** " + EmojiBalance.coin)
                    .addField("Balance", EmojiBalance.coin + " **" + getCash(cs.userId) + "** | " + EmojiBalance.balance + " **" + getBank(cs.userId) + "**", false)
                    .build()).setComponents(Collections.emptyList()).queue();
        }
    }

    private void chickenCashout(ButtonInteractionEvent event, ChickenSession cs, String key) {
        if (cs.wins == 0) {
            event.reply("Du musst mindestens eine Runde kämpfen!").setEphemeral(true).queue();
            return;
        }

        long payout = (long) (cs.bet * cs.multiplier);
        updateCash(cs.userId, (int) payout);
        addWin(cs.userId);
        chickenSessions.remove(key);

        event.editMessageEmbeds(new EmbedBuilder()
                .setColor(Color.GREEN)
                .setTitle("🐔 Chicken Fight - Ausgecasht!")
                .setDescription(EmojiMatch.match_win + " **Glückwunsch!**"
                        + "\n\nSiege: **" + cs.wins + "** | Multiplikator: **" + cs.multiplier + "x**"
                        + "\nGewinn: **" + payout + "** " + EmojiBalance.coin)
                .addField("Balance", EmojiBalance.coin + " **" + getCash(cs.userId) + "** | " + EmojiBalance.balance + " **" + getBank(cs.userId) + "**", false)
                .build()).setComponents(Collections.emptyList()).queue();
    }

    private void createUser(long userId) {
        try {
            PreparedStatement ps = Database.getConnection().prepareStatement("MERGE INTO users (id) KEY(id) VALUES (?)");
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private int getCash(long userId) {
        try {
            PreparedStatement ps = Database.getConnection().prepareStatement("SELECT cash FROM users WHERE id = ?");
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("cash");
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    private int getBank(long userId) {
        try {
            PreparedStatement ps = Database.getConnection().prepareStatement("SELECT bank FROM users WHERE id = ?");
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("bank");
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    private void updateCash(long userId, int amount) {
        try {
            PreparedStatement ps = Database.getConnection().prepareStatement("UPDATE users SET cash = cash + ? WHERE id = ?");
            ps.setInt(1, amount);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void addWin(long userId) {
        try {
            PreparedStatement ps = Database.getConnection().prepareStatement("UPDATE users SET wins = wins + 1 WHERE id = ?");
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void addLoss(long userId) {
        try {
            PreparedStatement ps = Database.getConnection().prepareStatement("UPDATE users SET losses = losses + 1 WHERE id = ?");
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
}