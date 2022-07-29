import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bson.Document;
import org.bson.conversions.Bson;

import javax.security.auth.login.LoginException;
import java.util.Collections;

public class Commands extends ListenerAdapter {

    public MongoClient client;
    public MongoDatabase db;
    public MongoCollection col;

    public Commands() {
        this.client = MongoClients.create("mongodb+srv://jakeahuber:6QVcZ2\"TzzXHdJa@cluster0.p85yq.mongodb.net/?retryWrites=true&w=majority");
        this.db = client.getDatabase("discord-db");
        this.col = db.getCollection("discord-collection");
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
    {
        if (event.getName().equals("pomodoro-25-5")) {
            event.reply("<@" + event.getUser().getId() + ">, hello! I'll ping you in 25 minutes. ").queue();
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            MessageChannelUnion channel = event.getChannel();
            channel.sendMessage("Hello <@" + event.getUser().getId() + ">! It's been 25 minutes. I'll ping you in 5 minutes.").queue();
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            Document user_doc = getUserById("test2");
            double time_studied = (double) user_doc.get("time_studied");
            time_studied += 0.5;

            // https://www.mongodb.com/docs/drivers/java/sync/current/usage-examples/updateOne/
            Bson updatedDoc = Updates.combine(Updates.set("time_studied", time_studied));
            UpdateOptions options = new UpdateOptions().upsert(true);
            try {
                UpdateResult result = col.updateOne(user_doc, updatedDoc, options);
            } catch (MongoException me) {
                channel.sendMessage("Pomodoro bot was unable to update total time studied due to an error: " + me);
            }
            channel.sendMessage("Hello <@" + event.getUser().getId() + ">! Your 5 minute break is over. Please call /pomodoro-25-5 to restart.").queue();
        }

        if (event.getName().equals("get-time-studied")) {
            Document user_doc = getUserById("test2");
            event.reply("<@" + event.getUser().getId() + "> time studied: " + user_doc.get("time_studied") + " hours.").queue();
        }
    }

    private Document getUserById(String id) {
        BasicDBObject query = new BasicDBObject("_id", id);
        Document user_doc = (Document) col.find(query).first();
        // add user to the database if they aren't in it.
        if (user_doc == null) {
            Document document = new Document();
            document.append("_id", id);
            document.append("time_studied", 0.0);
            col.insertOne(document);
        }
        query = new BasicDBObject("_id", id);
        user_doc = (Document) col.find(query).first();
        return user_doc;
    }
}
