import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Bot extends ListenerAdapter {

    private MongoClient client;
    private MongoDatabase db;
    private MongoCollection col;
    private Hashtable<String, Boolean> timerActiveInServer;
    private Hashtable<String, HashSet<String>> userIdsToUpdate;

    public Bot(String databaseConnector) {
        this.client = MongoClients.create(databaseConnector);
        this.db = client.getDatabase("discord-db");
        this.col = db.getCollection("discord-collection");
        this.userIdsToUpdate = new Hashtable<>();
        this.timerActiveInServer = new Hashtable<>();
    }

    public void addToUserIdsToUpdate(String serverId, String id) {
        if (userIdsToUpdate.containsKey(serverId)) {
            HashSet<String> set = userIdsToUpdate.get(serverId);
            set.add(id);
            userIdsToUpdate.put(serverId, set);
        }
        else {
            HashSet<String> set = new HashSet<>();
            set.add(id);
            userIdsToUpdate.put(serverId, set);
        }
    }

    public MongoCollection getCol() {
        return col;
    }

    public HashSet<String> getUserIdsToUpdate(String serverId) {
        if (userIdsToUpdate.containsKey(serverId)) {
            return userIdsToUpdate.get(serverId);
        }
        return null;
    }

    public void resetIdsToUpdate(String serverId) {
        if (userIdsToUpdate.containsKey(serverId)) {
            HashSet<String> set = new HashSet<>();
            userIdsToUpdate.put(serverId, set);
        }
    }

    public Document getUserById(String id) {
        BasicDBObject query = new BasicDBObject("_id", id);
        Document user_doc = (Document) col.find(query).first();
        // add user to the database if they aren't in it.
        if (user_doc == null) {
            Document document = new Document();
            document.append("_id", id);
            document.append("time_studied", 0.0);
            document.append("time_zone", "UTC");
            document.append("date_created", getDate("UTC"));
            col.insertOne(document);
        }
        query = new BasicDBObject("_id", id);
        user_doc = (Document) col.find(query).first();
        return user_doc;
    }

    public String getDate(String timeZone) {
        TimeZone tz = TimeZone.getTimeZone(timeZone);
        Calendar time = Calendar.getInstance(tz);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM. dd, YYYY", Locale.US);
        simpleDateFormat.setTimeZone(tz);
        return simpleDateFormat.format(time.getTime());
    }

    public Hashtable<String, Boolean> getTimerActiveInServer() {
        return timerActiveInServer;
    }

    public void updateTimerActiveInServer(String serverId, boolean isActive) {
        timerActiveInServer.put(serverId, isActive);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message msg = event.getMessage();
        if (msg.getContentRaw().equals("!pomodoro-15-5")) {
            PomodoroTimer.setTimer(event, this, 15, 5);
        }
        if (msg.getContentRaw().equals("!pomodoro-20-5")) {
            PomodoroTimer.setTimer(event, this, 20, 5);
        }
        if (msg.getContentRaw().equals("!pomodoro-25-5")) {
            PomodoroTimer.setTimer(event, this, 25, 5);
        }
        if (msg.getContentRaw().equals("!pomodoro-45-15")) {
            PomodoroTimer.setTimer(event, this, 45, 15);
        }
        if (msg.getContentRaw().equals("!pomodoro-50-10")) {
            PomodoroTimer.setTimer(event, this, 50, 10);
        }
        if (msg.getContentRaw().equals("!pomodoro-55-5")) {
            PomodoroTimer.setTimer(event, this, 55, 5);
        }
        if (msg.getContentRaw().equals("!get-time-studied")) {
            String userId = event.getAuthor().getId();
            MessageChannel channel = event.getChannel();
            Document user_doc = getUserById(userId);
            DecimalFormat df = new DecimalFormat("#.00");
            channel.sendMessage(event.getAuthor().getName() + ", you've studied for "
                    + df.format(user_doc.get("time_studied")) + " hours since " + user_doc.get("date_created") + ".").queue();
        }
        if (msg.getContentRaw().equals("!get-time-studied-today")) {
            String userId = event.getAuthor().getId();
            MessageChannel channel = event.getChannel();
            Document user_doc = getUserById(userId);
            String currentDate = getDate((String) user_doc.get("time_zone"));
            DecimalFormat df = new DecimalFormat("#.00");
            if (currentDate.equals(user_doc.get("last_study_date"))) {
                channel.sendMessage(event.getAuthor().getName() + ", you've studied for " +
                        df.format(user_doc.get("time_studied_today")) + " hours today.").queue();
            }
            else {
                channel.sendMessage(event.getAuthor().getName() + ", you've studied for 0.0 hours today.").queue();
            }
        }
        // users can only change their time zone is a private channel. This is because the bot requires user input and also to protect information about users.
        if (msg.getContentRaw().equals("!pomo-set-time-zone")) {
            MessageChannel channel = event.getChannel();
            if (channel.getType() != ChannelType.PRIVATE) {
                channel.sendMessage("<@" + event.getAuthor().getId() + ">, you may only change your time zone in a DM.").queue();
                return;
            }
            String userId = event.getAuthor().getId();
            String[] timeZones = {"GMT", "UTC", "ECT", "EET", "ART", "EAT", "MET", "NET", "PLT", "IST", "BST", "VST", "CTT", "JST",
            "ACT", "AET", "SST", "NST", "MIT", "HST", "AST", "PST", "PNT", "MST", "CST", "EST", "IET", "PRT", "CNT", "AGT", "BET", "CAT"};
            String str = "";
            for (int i = 0; i < timeZones.length - 1; i++) {
                str += (timeZones[i] + ", ");
            }
            str += timeZones[timeZones.length -1];

            Document user_doc = getUserById(userId);
            channel.sendMessage(event.getAuthor().getName() + ", your current time zone is "
                    + user_doc.get("time_zone") + ". Please select one of the following time zones:\n" + str).queue();

            // remove listener after one minute.
            StateMachineChangeTimeZone listener = new StateMachineChangeTimeZone(channel, userId, col);
            event.getJDA().addEventListener(listener);
            Runnable cancelListener = () -> {
                event.getJDA().removeEventListener(listener);
            };
            ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            scheduledExecutorService.schedule(cancelListener , 1, TimeUnit.MINUTES);
        }

        if (msg.getContentRaw().equals("!pomo-time") || msg.getContentRaw().equals("!quit-pomo") || msg.getContentRaw().equals("!add-to-pomo")) {
            String serverId = PomodoroTimer.getServerId(event);
            if (!timerActiveInServer.containsKey(serverId) || !timerActiveInServer.get(serverId)) {
                MessageChannel channel = event.getChannel();
                channel.sendMessage("There is no timer currently running. Call '!pomodoro-25-5' to start one.").queue();
            }
        }

        if (msg.getContentRaw().equals("!pomodoro-info")) {
            MessageChannel channel = event.getChannel();
            String pomodoro_15_5 = "• '!pomodoro-15-5': Executes a 15 minute study timer and a 5 minute break timer.\n";
            String pomodoro_20_5 = "• '!pomodoro-20-10': Executes a 20 minute study timer and a 5 minute break timer.\n";
            String pomodoro_25_5 = "• '!pomodoro-25-5': Executes a 25 minute study timer and a 5 minute break timer.\n";
            String pomodoro_45_15 = "• '!pomodoro-45-15': Executes a 45 minute study timer and a 15 minute break timer.\n";
            String pomodoro_50_10 = "• '!pomodoro-50-10': Executes a 50 minute study timer and a 10 minute break timer.\n";
            String pomodoro_55_5 = "• '!pomodoro-55-5': Executes a 55 minute study timer and a 5 minute break timer.\n\n";

            String quitPomo = "• '!quit-pomo': If a pomodoro timer is currently running, this cancels that timer.\n";
            String pomoTime = "• '!pomo-time': If a pomodoro timer is currently running, this displays the time remaining on the timer.\n";
            String addToPomo = "• '!add-to-pomo': If a pomodoro timer is currently running, but someone else called the timer, " +
                               "this command will add you to the pomodoro and add onto the amount of time you've studied.\n";
            String getTimeStudiedToday = "• '!get-time-studied-today': Displays the number of hours you've studied today.\n";
            String getTimeStudied = "• '!get-time-studied': Displays the number of  hours you've studied since using Pomodoro Bot for the first time.\n";
            String setTimeZone = "• '!pomo-set-time-zone': Changes the time zone associated with your account (default is UTC). " +
                                 "You can change your time zone if you want a more accurate number for '!get-time-studied-today'. You may only change your time zone in a DM.\n";
            ArrayList<String> commands = new ArrayList<>(Arrays.asList(pomodoro_25_5, pomodoro_15_5, pomodoro_20_5,
                                                         pomodoro_45_15, pomodoro_50_10, pomodoro_55_5, quitPomo, pomoTime,
                                                         addToPomo, getTimeStudiedToday, getTimeStudied, setTimeZone));
            String commandsString = "";
            for (int i = 0; i < commands.size(); i++) {
                commandsString += commands.get(i);
            }

            channel.sendMessage("Hello! Welcome to Pomodoro Bot! This bot allows you to set study timers " +
                                "(Pomodoros) and keeps track of how many hours you study. If you would like to " +
                                "contribute to this project, please check out the source code on github: " +
                                "https://github.com/Jakeahuber/pomodoro-bot. If you would like to report any " +
                                "problems with the bot, please submit an issue to the github repository: " +
                                "https://github.com/Jakeahuber/pomodoro-bot/issues. Here's a list of available " +
                                "commands:\n\n" + commandsString).queue();
        }
    }
}
