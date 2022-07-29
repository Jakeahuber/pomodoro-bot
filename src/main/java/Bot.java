import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.text.SimpleDateFormat;
import java.util.*;

public class Bot extends ListenerAdapter {

    private MongoClient client;
    private MongoDatabase db;
    private MongoCollection col;

    public Bot(String databaseConnector) {
        this.client = MongoClients.create(databaseConnector);
        this.db = client.getDatabase("discord-db");
        this.col = db.getCollection("discord-collection");
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
            document.append("today_study_times", new Document());
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
            channel.sendMessage("<@" + event.getAuthor().getId() + ">, hello! I'll ping you in " +
                                "25 minutes. ").queue();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            channel.sendMessage("Hello <@" + event.getAuthor().getId() + ">! It's been 25 minutes. I'll ping " +
                                "you in 5 minutes.").queue();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            // Update user's total time studied and adds this study time to the today study time document
            // https://www.mongodb.com/docs/drivers/java/sync/current/usage-examples/updateOne/
            UpdateOptions options = new UpdateOptions().upsert(true);

            Document user_doc = getUserById("test5");
            double time_studied = (double) user_doc.get("time_studied");
            time_studied += 0.5;

            String todayDate = getDate((String) user_doc.get("time_zone"));

            double studyTimeToday = 0.5;
            Object prevStudyTimeToday = user_doc.get(todayDate);
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
                channel.sendMessage("Pomodoro bot was unable to update total time studied due to an error: " + me);
            }

            channel.sendMessage("Hello <@" + event.getAuthor().getId() + ">! Your 5 minute break is over. Please call /pomodoro-25-5 to restart.").queue();
        }
        if (msg.getContentRaw().equals("!get-time-studied")) {
            MessageChannel channel = event.getChannel();
            Document user_doc = getUserById("test5");
            channel.sendMessage(event.getAuthor().getName() + ", you've studied for "
                    + user_doc.get("time_studied") + " hours since " + user_doc.get("date_created") + ".").queue();
        }
        if (msg.getContentRaw().equals("!get-time-studied-today")) {
            MessageChannel channel = event.getChannel();
            Document user_doc = getUserById("test5");
            String currentDate = getDate((String) user_doc.get("time_zone"));
            if (currentDate.equals(user_doc.get("last_study_date"))) {
                channel.sendMessage(event.getAuthor().getName() + ", you've studied for " +
                        user_doc.get("time_studied_today") + " hours today.").queue();
            }
            else {
                channel.sendMessage(event.getAuthor().getName() + ", you've studied for 0 hours today.").queue();
            }
        }

        // TODO
        if (msg.getContentRaw().equals("!set-time-zone")) {
            String[] timeZones = {"GMT", "UTC", "ECT", "EET", "ART", "EAT", "MET", "NET", "PLT", "IST", "BST", "VST", "CTT", "JST",
            "ACT", "AET", "SST", "NST", "MIT", "HST", "AST", "PST", "PNT", "MST", "CST", "EST", "IET", "PRT", "CNT", "AGT", "BET", "CAT"};
            String str = "";
            for (int i = 0; i < timeZones.length - 1; i++) {
                str += (timeZones[i] + ", ");
            }
            str += timeZones[timeZones.length -1];

            MessageChannel channel = event.getChannel();
            Document user_doc = getUserById("test5");
            channel.sendMessage(event.getAuthor().getName() + ", your current time zone is "
                    + user_doc.get("time_zone") + ". Please select one of the following time zones:\n" + str).queue();
        }
    }
}
