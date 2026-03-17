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
                                new OptionData(OptionType.STRING, "type", "Which game?")
                                        .addChoice("Blackjack", "blackjack")
                                        .addChoice("Dice", "dice")
                                        .addChoice("Coinflip", "coinflip")
                                        .addChoice("Roulette", "roulette")
                                        .addChoice("Chicken-Fight", "chickenfight")
                        ),
                Commands.slash("leaderboard", "View your game stats")
                        .addOptions(
                                new OptionData(OptionType.STRING, "type", "Which Leaderboard?")
                                        .addChoice("All Leaderboard", "leaderboard")
                                        .addChoice("All Time Balance", "alltime-lb")
                                        .addChoice("All Time Wins", "alltime-wins-lb")
                                        .addChoice("Stash Balance", "stash-lb")
                        ),
                Commands.slash("profile", "View your Profile")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "user", "Which User?")
                                                .addChoice("Yourself", "self")
                                                .addChoice("Another User", "otheruser")
                                ),
                Commands.slash("balance", "Check your current Balance")
                        .addOptions(
                                new OptionData(OptionType.STRING, "type", "Which Balance?")
                                        .addChoice("All", "all")
                                        .addChoice("Cash", "cash")
                                        .addChoice("Bank", "bank")
                                        .addChoice("Wallet", "wallet")
                                        .addChoice("Stash", "stash")
                        )

        ).queue();
    }
}