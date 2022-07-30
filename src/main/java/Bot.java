import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Bot extends ListenerAdapter {

    private MongoClient client;
    private MongoDatabase db;
    private MongoCollection col;
    private HashSet<String> userIdsToUpdate;
    private boolean timerActive;

    public Bot(String databaseConnector) {
        this.client = MongoClients.create(databaseConnector);
        this.db = client.getDatabase("discord-db");
        this.col = db.getCollection("discord-collection");
        this.userIdsToUpdate = new HashSet<>();
        this.timerActive = false;
    }

    public void addToUserIdsToUpdate(String id) {
        userIdsToUpdate.add(id);
    }

    public void resetIdsToUpdate() {
        this.userIdsToUpdate = new HashSet<>();
    }

    public void setTimerActive(boolean res) {
        this.timerActive = res;
    }

    private Document getUserById(String id) {
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

    private String getDate(String timeZone) {
        TimeZone tz = TimeZone.getTimeZone(timeZone);
        Calendar time = Calendar.getInstance(tz);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM. dd, YYYY", Locale.US);
        simpleDateFormat.setTimeZone(tz);
        return simpleDateFormat.format(time.getTime());
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message msg = event.getMessage();
        if (msg.getContentRaw().equals("!pomodoro-25-5")) {
            MessageChannel channel = event.getChannel();
            String userId = event.getAuthor().getId();
            addToUserIdsToUpdate(userId);
            timerActive = true;
            StateMachinePomodoroTimer listener = new StateMachinePomodoroTimer(channel, userId, col, this);
            channel.sendMessage("<@" + userId + ">, hello! I'll ping you in " +
                    "25 minutes. Please call '!pomo-time' to check how much time is left or '!quit-pomo' to quit the timer.").queue();

            Runnable after25MinStudy = () -> channel.sendMessage("Hello <@" + userId + ">! " +
                    "It's been 25 minutes. I'll ping you in 5 minutes.").queue();

            Runnable after5MinBreak = () -> {
                channel.sendMessage("Hello <@" + userId + ">! Your 5 minute break is " +
                                    "over. Please call /pomodoro-25-5 to restart.").queue();
                // Iterates through each person that was added to the study timer ands updates their time studied.
                // https://www.mongodb.com/docs/drivers/java/sync/current/usage-examples/updateOne/
                UpdateOptions options = new UpdateOptions().upsert(true);
                event.getJDA().removeEventListener(listener);
                Iterator<String> it = userIdsToUpdate.iterator();
                while (it.hasNext()) {
                    String user = it.next();
                    Document user_doc = getUserById(user);
                    double time_studied = (double) user_doc.get("time_studied");
                    time_studied += 0.5;

                    String todayDate = getDate((String) user_doc.get("time_zone"));

                    double studyTimeToday = 0.5;
                    Object prevStudyTimeToday = user_doc.get("time_studied_today");
                    String lastStudyDate = (String) user_doc.get("last_study_date");
                    if (lastStudyDate != null && lastStudyDate.equals(todayDate)) {
                        if (prevStudyTimeToday != null) {
                            studyTimeToday += (double) prevStudyTimeToday;
                        }
                    }

                    Bson updatedDoc = Updates.combine(Updates.set("time_studied", time_studied),
                            Updates.set("time_studied_today", studyTimeToday), Updates.set("last_study_date", todayDate));

                    try {
                        col.updateOne(user_doc, updatedDoc, options);
                    } catch (MongoException me) {
                        channel.sendMessage("Pomodoro bot was unable to update total time studied for <@" + user + "> due to an error updating to the database.");
                    }
                }
                resetIdsToUpdate();
                timerActive = false;
            };
            ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            ScheduledFuture<?> scheduledStudyTimer = scheduledExecutorService.schedule(after25MinStudy , 100 , TimeUnit.SECONDS ); // 10-second delay
            ScheduledFuture<?> scheduledBreakTimer = scheduledExecutorService.schedule(after5MinBreak , 125 , TimeUnit.SECONDS ); // 15-second delay
            listener.addScheduledExecutors(scheduledStudyTimer, scheduledBreakTimer);
            event.getJDA().addEventListener(listener);
        }
        if (msg.getContentRaw().equals("!get-time-studied")) {
            String userId = event.getAuthor().getId();
            MessageChannel channel = event.getChannel();
            Document user_doc = getUserById(userId);
            channel.sendMessage(event.getAuthor().getName() + ", you've studied for "
                    + user_doc.get("time_studied") + " hours since " + user_doc.get("date_created") + ".").queue();
        }
        if (msg.getContentRaw().equals("!get-time-studied-today")) {
            String userId = event.getAuthor().getId();
            MessageChannel channel = event.getChannel();
            Document user_doc = getUserById(userId);
            String currentDate = getDate((String) user_doc.get("time_zone"));
            if (currentDate.equals(user_doc.get("last_study_date"))) {
                channel.sendMessage(event.getAuthor().getName() + ", you've studied for " +
                        user_doc.get("time_studied_today") + " hours today.").queue();
            }
            else {
                channel.sendMessage(event.getAuthor().getName() + ", you've studied for 0 hours today.").queue();
            }
        }
        // users can only change their time zone is a private channel. This is because the bot requires user input and also to protect information about users.
        if (msg.getContentRaw().equals("!set-time-zone")) {
            MessageChannel channel = event.getChannel();
            if (channel.getType() != ChannelType.PRIVATE) {
                channel.sendMessage("<@" + event.getAuthor().getId() + ">, you may only change your time zone in a private DM.").queue();
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
            event.getJDA().addEventListener(new StateMachineChangeTimeZone(channel, userId, col));
        }

        if (msg.getContentRaw().equals("!pomo-time") || msg.getContentRaw().equals("!quit-pomo") || msg.getContentRaw().equals("!add-to-pomo")) {
            if (!timerActive) {
                MessageChannel channel = event.getChannel();
                channel.sendMessage("There is no timer currently running. You can call '!pomodoro-25-5' to start one.").queue();
            }
        }

        if (msg.getContentRaw().equals("!pomodoro-info")) {
            MessageChannel channel = event.getChannel();
            String pomodoro_25_5 = "• '!pomodoro-25-5': Executes a 25 minute study timer and a 5 minute break timer.\n";
            String quitPomo = "• '!quit-pomo': If a pomodoro timer is currently running, this cancels that timer.\n";
            String pomoTime = "• '!pomo-time': If a pomodoro timer is currently running, this displays the time remaining on the timer.\n";
            String addToPomo = "• '!add-to-pomo': If a pomodoro timer is currently running, but someone else called the timer, " +
                               "this command will add you to the pomodoro and add onto the amount of time you've studied.\n";
            String getTimeStudiedToday = "• '!get-time-studied-today': Displays the number of hours you've studied today.\n";
            String getTimeStudied = "• '!get-time-studied': Displays the number of  hours you've studied since using Pomodoro Bot for the first time.\n";
            String setTimeZone = "• '!set-time-zone': Changes the time zone associated with your account (default is UTC). " +
                                 "You can change your time zone if you want a more accurate number for '!get-time-studied-today'.";
            ArrayList<String> commands = new ArrayList<>(Arrays.asList(pomodoro_25_5, quitPomo, pomoTime, addToPomo,
                                                         getTimeStudiedToday, getTimeStudied, setTimeZone));
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
