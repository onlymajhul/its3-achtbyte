package de.tle.discord.cogs;

import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class GameCommands extends ListenerAdapter {

    @Override
    public void onGuildReady(GuildReadyEvent event) {

        event.getGuild().updateCommands().addCommands(
                Commands.slash("play", "Start a game")
                        .addOptions(
                                new OptionData(OptionType.STRING, "type", "Which game?", true)
                                        .addChoice("Blackjack", "blackjack")
                                        .addChoice("Dice", "dice")
                                        .addChoice("Coinflip", "coinflip")
                                        .addChoice("Roulette", "roulette")
                                        .addChoice("Chicken-Fight", "chickenfight"),
                                new OptionData(OptionType.INTEGER, "bet", "How much do you want to bet?", true)
                                        .setMinValue(10)
                                        .setMaxValue(100000)
                        ),
                Commands.slash("leaderboard", "View the leaderboard")
                        .addOptions(
                                new OptionData(OptionType.STRING, "type", "Which Leaderboard?")
                                        .addChoice("All Leaderboard", "leaderboard")
                                        .addChoice("All Time Balance", "alltime-lb")
                                        .addChoice("All Time Wins", "alltime-wins-lb")
                                        .addChoice("Bank Balance", "stash-lb")
                        ),
                Commands.slash("profile", "View your Profile"),
                Commands.slash("work", "Führe deine tägliche Arbeit aus"),
                Commands.slash("career", "Wähle oder wechsle deinen Karrierepfad")
                        .addOptions(
                                new OptionData(OptionType.STRING, "path", "In welcher Branche möchtest du arbeiten?", true)
                                        .addChoice("Natur & Landwirtschaft", "nature")
                                        .addChoice("Finanzen & Immobilien", "finance")
                                        .addChoice("Medizin & Gesundheit", "medical")
                                        .addChoice("Technik & IT", "tech")
                                        .addChoice("Gastronomie & Handwerk", "gastro")
                                        .addChoice("Recht & Ordnung", "law")
                                        .addChoice("Unterhaltung & Medien", "media")
                                        .addChoice("Unterwelt (Illegal)", "illegal")
                        ),
                Commands.slash("job", "Informationen über deinen aktuellen Job & Fortschritt"),
                Commands.slash("sellhouse", "Als Immobilienmakler ein Haus verkaufen")
                        .addOptions(
                                new OptionData(OptionType.INTEGER, "price", "Dein Angebotspreis für das Objekt", true)
                                        .setMinValue(50000)
                        ),
                Commands.slash("education", "Starte ein Studium oder eine Ausbildung")
                        .addOptions(
                                new OptionData(OptionType.STRING, "type", "Was möchtest du lernen?", true)
                                        .addChoice("Studium Medizin (50k, 14 Tage)", "study_med")
                                        .addChoice("Kurs Cybersecurity (75k, 1 Tag)", "course_hacker")
                                        .addChoice("Ausbildung Programmierer (Kostenlos, 7 Tage)", "edu_prog")
                                        .addChoice("Kurs Programmierer (15k, 1 Tag)", "course_prog")
                        ),
                Commands.slash("crime", "Begehe ein Verbrechen (Bankraub)"),
                Commands.slash("shop", "Kaufe Items"),
                Commands.slash("balance", "Check your current Balance")
                        .addOptions(
                                new OptionData(OptionType.STRING, "type", "Which Balance?")
                                        .addChoice("All", "all")
                                        .addChoice("Cash", "cash")
                                        .addChoice("Bank", "bank")
                        ),
                Commands.slash("deposit", "Deposit cash into bank")
                        .addOptions(
                                new OptionData(OptionType.INTEGER, "amount", "Amount to deposit", true)
                                        .setMinValue(1)
                        ),
                Commands.slash("withdraw", "Withdraw cash from bank")
                        .addOptions(
                                new OptionData(OptionType.INTEGER, "amount", "Amount to withdraw", true)
                                        .setMinValue(1)
                        )
        ).queue();
    }
}