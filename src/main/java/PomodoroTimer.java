import com.mongodb.MongoException;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PomodoroTimer {

    public static void setTimer(MessageReceivedEvent event, Bot bot, int studyTime, int breakTime) {
        MessageChannel channel = event.getChannel();
        Hashtable<String, Boolean> timerActiveInServer = bot.getTimerActiveInServer();
        String serverId = event.getGuild().getId();

        // There can only be one pomodoro running at a time.
        if (timerActiveInServer.containsKey(serverId) && timerActiveInServer.get(serverId)) {
            channel.sendMessage("There is already another pomodoro running. You may join that pomodoro by calling '!add-to-pomo'.").queue();
            return;
        }

        String userId = event.getAuthor().getId();
        bot.addToUserIdsToUpdate(serverId, userId);
        bot.updateTimerActiveInServer(serverId, true);

        StateMachinePomodoroTimer listener = new StateMachinePomodoroTimer(channel, userId, bot, studyTime, breakTime);
        channel.sendMessage("<@" + userId + ">, hello! The studying timer will end in " + studyTime +
                " minutes. Please call '!pomo-time' to check how much time is left or '!quit-pomo' to quit the timer. " +
                "If there are other users that would like to join this pomodoro session, please enter '!add-to-pomo'.").queue();

        Runnable afterStudy = () -> {
            Iterator<String> it = bot.getUserIdsToUpdate(serverId).iterator();
            String listOfIds = "";
            while (it.hasNext()) {
                String user = it.next();
                listOfIds += "<@" + user + ">, ";
            }

            channel.sendMessage("Hello " + listOfIds +
                    "it's been " + studyTime + " minutes! The break will be over in " + breakTime +" minutes.").queue();
        };

        Runnable afterBreak = () -> {
            // Iterates through each person that was added to the study timer ands updates their time studied.
            // https://www.mongodb.com/docs/drivers/java/sync/current/usage-examples/updateOne/
            UpdateOptions options = new UpdateOptions().upsert(true);
            event.getJDA().removeEventListener(listener);
            Iterator<String> it = bot.getUserIdsToUpdate(serverId).iterator();
            String listOfIds = "";
            while (it.hasNext()) {
                String user = it.next();
                listOfIds += "<@" + user + ">, ";
                Document user_doc = bot.getUserById(user);
                double time_studied = (double) user_doc.get("time_studied");
                time_studied += (studyTime + breakTime) / 60.0;

                String todayDate = bot.getDate((String) user_doc.get("time_zone"));

                double studyTimeToday = (studyTime + breakTime) / 60.0;
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
                    bot.getCol().updateOne(user_doc, updatedDoc, options);
                } catch (MongoException me) {
                    channel.sendMessage("Pomodoro bot was unable to update total time studied for <@" + user + "> due to an error.");
                }
            }
            channel.sendMessage("Hello " + listOfIds +
                    "the " + breakTime + " minute break is over! Please call another pomodoro timer to restart. For a full list of timers available, call '!pomodoro-info'.").queue();
            bot.resetIdsToUpdate(serverId);
            bot.updateTimerActiveInServer(serverId, false);
        };
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> scheduledStudyTimer = scheduledExecutorService.schedule(afterStudy , studyTime, TimeUnit.MINUTES);
        ScheduledFuture<?> scheduledBreakTimer = scheduledExecutorService.schedule(afterBreak , studyTime + breakTime, TimeUnit.MINUTES);
        listener.addScheduledExecutors(scheduledStudyTimer, scheduledBreakTimer);
        event.getJDA().addEventListener(listener);
    }
}
