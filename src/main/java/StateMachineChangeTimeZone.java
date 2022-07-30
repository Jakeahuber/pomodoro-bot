import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Arrays;
import java.util.HashSet;

public class StateMachineChangeTimeZone extends ListenerAdapter {

    private final MessageChannel channel;
    private final String userId;
    private final MongoCollection col;

    public StateMachineChangeTimeZone(MessageChannel channel, String userId, MongoCollection col) {
        this.channel = channel;
        this.userId = userId;
        this.col = col;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || event.getChannel() != channel || !event.getAuthor().getId().equals(userId)) return;

        String message = event.getMessage().getContentRaw();

        HashSet<String> timeZones = new HashSet<>(Arrays.asList("GMT", "UTC", "ECT", "EET", "ART", "EAT", "MET",
                                                  "NET", "PLT", "IST", "BST", "VST", "CTT", "JST", "ACT", "AET",
                                                  "SST", "NST", "MIT", "HST", "AST", "PST", "PNT", "MST", "CST",
                                                  "EST", "IET", "PRT", "CNT", "AGT", "BET", "CAT"));

        if (timeZones.contains(message.toUpperCase())) {
            BasicDBObject query = new BasicDBObject("_id", userId);
            Document user_doc = (Document) col.find(query).first();
            UpdateOptions options = new UpdateOptions().upsert(true);
            Bson updatedDoc = Updates.combine(Updates.set("time_zone", message.toUpperCase()));
            try {
                col.updateOne(user_doc, updatedDoc, options);
            } catch (MongoException me) {
                channel.sendMessage("Pomodoro bot was unable to update total time studied due to an error.");
            }
            event.getJDA().removeEventListener(this);
            channel.sendMessage("<@" + event.getAuthor().getId() + ">, your timezone was changed to " + message.toUpperCase() + "!").queue();
        } else {
            channel.sendMessage("<@" + event.getAuthor().getId() + ">, please enter a valid timezone.").queue();
        }
    }

}
