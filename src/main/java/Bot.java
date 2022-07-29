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
            col.insertOne(document);
        }
        query = new BasicDBObject("_id", id);
        user_doc = (Document) col.find(query).first();
        return user_doc;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message msg = event.getMessage();
        if (msg.getContentRaw().equals("!pomodoro-25-5")) {
            MessageChannel channel = event.getChannel();
            channel.sendMessage("<@" + event.getAuthor().getId() + ">, hello! I'll ping you in " +
                                "25 minutes. ").queue();
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            channel.sendMessage("Hello <@" + event.getAuthor().getId() + ">! It's been 25 minutes. I'll ping " +
                                "you in 5 minutes.").queue();
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            // Update user's total time studied
            // https://www.mongodb.com/docs/drivers/java/sync/current/usage-examples/updateOne/
            Document user_doc = getUserById("test2");
            double time_studied = (double) user_doc.get("time_studied");
            time_studied += 0.5;
            Bson updatedDoc = Updates.combine(Updates.set("time_studied", time_studied));
            UpdateOptions options = new UpdateOptions().upsert(true);
            try {
                col.updateOne(user_doc, updatedDoc, options);
            } catch (MongoException me) {
                channel.sendMessage("Pomodoro bot was unable to update total time studied due to an error: " + me);
            }

            channel.sendMessage("Hello <@" + event.getAuthor().getId() + ">! Your 5 minute break is over. Please call /pomodoro-25-5 to restart.").queue();
        }
        if (msg.getContentRaw().equals("!get-time-studied")) {
            MessageChannel channel = event.getChannel();
            Document user_doc = getUserById("test2");
            channel.sendMessage(event.getAuthor().getName() + " time studied: " + user_doc.get("time_studied") + " hours.").queue();
        }
    }
}
