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
import java.util.Map;
import java.util.Random;

public class WorkCog extends ListenerAdapter {

    private final Random random = new Random();

    private final Map<String, String[]> PATHS = Map.of(
            "nature", new String[]{"Helfer", "Farmer", "Förster", "Agrar-Ingenieur", "Großgrundbesitzer"},
            "finance", new String[]{"Verkäufer", "Automobilmakler", "Bänker", "Immobilienmakler", "Fondsmanager", "Hedgefonds-Guru"},
            "medical", new String[]{"Praktikant", "Pfleger", "Krankenschwester", "Arzt", "Facharzt", "Chefarzt"},
            "tech", new String[]{"IT-Support", "Junior-Dev", "Programmierer", "System-Architekt", "CTO", "Tech-Milliardär"},
            "gastro", new String[]{"Spüler", "Koch", "Küchenchef", "Restaurantbesitzer", "Sternekoch", "Gastro-Tycoon"},
            "law", new String[]{"Kadett", "Polizist", "Kommissar", "Detektiv", "SEK-Beamter", "Polizeipräsident"},
            "media", new String[]{"Statist", "YouTuber", "Influencer", "Moderator", "Filmstar", "Medien-Mogul"},
            "illegal", new String[]{"Taschendieb", "Dealer", "Einbrecher", "Hehler", "Mafioso", "Patrone"}
    );

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        long userId = event.getUser().getIdLong();
        switch (event.getName()) {
            case "work" -> handleWork(event, userId);
            case "career" -> handleCareerChange(event, userId);
            case "job" -> handleJobInfo(event, userId);
            case "education" -> handleEducation(event, userId);
            case "crime" -> handleCrime(event, userId);
            case "shop" -> handleShop(event, userId);
            case "sellhouse" -> handleHouseSale(event, userId);
        }
    }

    private void handleWork(SlashCommandInteractionEvent event, long userId) {
        try {
            ResultSet rs = getUserData(userId);
            if (!rs.next()) return;

            long lastWork = rs.getLong("last_work");
            if (System.currentTimeMillis() < lastWork + 86400000L) {
                event.reply("Nächste Schicht erst in: " + formatTime((lastWork + 86400000L) - System.currentTimeMillis())).setEphemeral(true).queue();
                return;
            }

            String job = rs.getString("job");
            int level = rs.getInt("job_level");

            if (job.equals("Arbeitslos") && random.nextDouble() >= 0.995) {
                updateStat(userId, "last_work", System.currentTimeMillis());
                event.reply(EmojiMatch.match_lose + " Erwischt! Deine Schwarzarbeit wurde unterbunden.").queue();
                return;
            }

            int salary = 200 + (level * 150);
            updateCash(userId, salary);
            updateStat(userId, "last_work", System.currentTimeMillis());
            updateStat(userId, "job_level", level + 1);

            event.replyEmbeds(new EmbedBuilder()
                    .setColor(Color.CYAN).setTitle("💼 Arbeit beendet")
                    .setDescription("Lohn: **" + salary + "** " + EmojiBalance.coin)
                    .setFooter("Job: " + job + " | Level: " + (level + 1)).build()).queue();

            checkPromotionAuto(userId, job, level + 1);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleCareerChange(SlashCommandInteractionEvent event, long userId) {
        String newPath = event.getOption("path").getAsString();
        try {
            ResultSet rs = getUserData(userId);
            if (!rs.next()) return;

            String edu = rs.getString("education_type");
            if (newPath.equals("medical") && !edu.equals("study_med") && rs.getInt("job_level") > 0) {
                event.reply("Du musst erst Medizin studieren!").setEphemeral(true).queue();
                return;
            }
            if (newPath.equals("tech") && !(edu.contains("prog") || edu.contains("hacker")) && rs.getInt("job_level") > 0) {
                event.reply("Du brauchst eine IT-Ausbildung!").setEphemeral(true).queue();
                return;
            }

            String startJob = PATHS.get(newPath)[0];
            PreparedStatement ps = Database.getConnection().prepareStatement("UPDATE users SET job = ?, job_level = 0 WHERE id = ?");
            ps.setString(1, startJob); ps.setLong(2, userId); ps.executeUpdate();
            event.reply("Karriere gewechselt zu: **" + startJob + "**").queue();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleEducation(SlashCommandInteractionEvent event, long userId) {
        try {
            String type = event.getOption("type").getAsString();
            int cost = type.equals("study_med") ? 50000 : type.equals("course_hacker") ? 75000 : type.equals("course_prog") ? 15000 : 0;
            if (getCash(userId) < cost) { event.reply("Zu wenig Cash!").setEphemeral(true).queue(); return; }

            long duration = type.equals("study_med") ? 14L : type.equals("edu_prog") ? 7L : 1L;
            long end = System.currentTimeMillis() + (duration * 86400000L);

            updateCash(userId, -cost);
            PreparedStatement ps = Database.getConnection().prepareStatement("UPDATE users SET education_type = ?, education_end = ? WHERE id = ?");
            ps.setString(1, type); ps.setLong(2, end); ps.setLong(3, userId); ps.executeUpdate();
            event.reply("Ausbildung gestartet! Dauer: " + duration + " Tage.").queue();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleCrime(SlashCommandInteractionEvent event, long userId) {
        try {
            ResultSet rs = getUserData(userId);
            if (!rs.next() || !rs.getBoolean("has_weapon")) {
                event.reply("Du brauchst eine Waffe!").setEphemeral(true).queue(); return;
            }
            if (System.currentTimeMillis() < rs.getLong("last_crime") + 129600000L) {
                event.reply("Warte 36h!").setEphemeral(true).queue(); return;
            }
            updateStat(userId, "last_crime", System.currentTimeMillis());
            if (random.nextDouble() < (0.42 - (rs.getInt("criminal_records") * 0.05))) {
                updateCash(userId, 35000);
                event.reply("💰 Bankraub erfolgreich! 35k erhalten.").queue();
            } else {
                updateCash(userId, -2000);
                updateStat(userId, "criminal_records", rs.getInt("criminal_records") + 1);
                event.reply("🚔 Verhaftet! 2k Strafe.").queue();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleHouseSale(SlashCommandInteractionEvent event, long userId) {
        try {
            ResultSet rs = getUserData(userId);
            if (!rs.next() || !rs.getString("job").equals("Immobilienmakler")) {
                event.reply("Nur für Immobilienmakler!").setEphemeral(true).queue(); return;
            }
            int price = event.getOption("price").getAsInt();
            int base = 100000 + (random.nextInt(50) * 5000);
            int demand = random.nextInt(3);
            double limit = switch(demand) { case 0 -> 1.05; case 1 -> 1.25; default -> 1.65; };
            if (price <= (base * limit)) {
                int comm = (int)(price * 0.05);
                updateCash(userId, comm);
                event.reply("🏘️ Haus verkauft! Provision: **" + comm + "**").queue();
            } else { event.reply("🏘️ Preis zu hoch, Käufer springt ab.").queue(); }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleJobInfo(SlashCommandInteractionEvent event, long userId) {
        try {
            ResultSet rs = getUserData(userId);
            if (!rs.next()) return;
            long end = rs.getLong("education_end");
            if (end > 0 && end <= System.currentTimeMillis()) finishEdu(userId, rs.getString("education_type"));

            event.replyEmbeds(new EmbedBuilder().setTitle("📋 Job-Info")
                    .addField("Job", rs.getString("job"), true)
                    .addField("Level", String.valueOf(rs.getInt("job_level")), true)
                    .addField("Akte", rs.getInt("criminal_records") + " Einträge", true).build()).queue();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleShop(SlashCommandInteractionEvent event, long userId) {
        try {
            if (getCash(userId) < 7500) { event.reply("Zu wenig Cash!").setEphemeral(true).queue(); return; }
            updateCash(userId, -7500);
            PreparedStatement ps = Database.getConnection().prepareStatement("UPDATE users SET has_weapon = TRUE WHERE id = ?");
            ps.setLong(1, userId); ps.executeUpdate();
            event.reply("Waffe gekauft!").queue();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void checkPromotionAuto(long userId, String currentJob, int level) throws Exception {
        for (String pathKey : PATHS.keySet()) {
            String[] jobs = PATHS.get(pathKey);
            for (int i = 0; i < jobs.length; i++) {
                if (jobs[i].equals(currentJob)) {
                    int targetIndex = i;
                    
                    // Beförderungssystem level = job.level
                    if (level >= 100 && jobs.length > 5) targetIndex = 5;
                    else if (level >= 60 && jobs.length > 4) targetIndex = 4;
                    else if (level >= 30 && jobs.length > 3) targetIndex = 3;
                    else if (level >= 15 && jobs.length > 2) targetIndex = 2;
                    else if (level >= 5 && jobs.length > 1) targetIndex = 1;

                    if (targetIndex > i) {
                        String newJob = jobs[targetIndex];
                        PreparedStatement ps = Database.getConnection().prepareStatement(
                            "UPDATE users SET job = ? WHERE id = ?"
                        );
                        ps.setString(1, newJob);
                        ps.setLong(2, userId);
                        ps.executeUpdate();
                    }
                    return;
                }
            }
        }
    }

    private void finishEdu(long id, String type) throws Exception {
        String newJob;
        String newPath;
        
        if (type.equals("study_med")) {
            newJob = "Arzt";
            newPath = "medical";
        } else if (type.contains("hacker")) {
            newJob = "Hacker";
            newPath = "tech";
        } else if (type.contains("prog")) {
            newJob = "Programmierer";
            newPath = "tech";
        } else {
            return;
        }

        PreparedStatement ps = Database.getConnection().prepareStatement(
            "UPDATE users SET job = ?, education_type = 'None', education_end = 0, job_level = 10 WHERE id = ?"
        );
        ps.setString(1, newJob);
        ps.setLong(2, id);
        ps.executeUpdate();
    }

    private String formatTime(long ms) {
        long h = ms / 3600000;
        long m = (ms % 3600000) / 60000;
        return String.format("%02dh %02dm", h, m);
    }

    private ResultSet getUserData(long id) throws Exception {
        PreparedStatement ps = Database.getConnection().prepareStatement("SELECT * FROM users WHERE id = ?");
        ps.setLong(1, id);
        return ps.executeQuery();
    }

    private int getCash(long id) throws Exception {
        PreparedStatement ps = Database.getConnection().prepareStatement("SELECT cash FROM users WHERE id = ?");
        ps.setLong(1, id);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getInt("cash") : 0;
    }

    private void updateCash(long id, int amt) throws Exception {
        PreparedStatement ps = Database.getConnection().prepareStatement("UPDATE users SET cash = cash + ? WHERE id = ?");
        ps.setInt(1, amt);
        ps.setLong(2, id);
        ps.executeUpdate();
    }

    private void updateStat(long id, String col, long val) throws Exception {
        PreparedStatement ps = Database.getConnection().prepareStatement("UPDATE users SET " + col + " = ? WHERE id = ?");
        ps.setLong(1, val);
        ps.setLong(2, id);
        ps.executeUpdate();
    }
}