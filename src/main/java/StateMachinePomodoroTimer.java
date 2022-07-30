import com.mongodb.client.MongoCollection;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.concurrent.ScheduledFuture;

public class StateMachinePomodoroTimer extends ListenerAdapter {
    private final MessageChannel channel;
    private final String userId;
    private final MongoCollection col;
    private Instant endTime;
    private ScheduledFuture<?> scheduledStudyTimer;
    private ScheduledFuture<?> scheduledBreakTimer;
    private HashSet<String> userIdsInSession;
    private Bot bot;

    public StateMachinePomodoroTimer(MessageChannel channel, String userId, MongoCollection col, Bot bot) {
        this.channel = channel;
        this.userId = userId;
        this.col = col;
        this.endTime = Instant.now().plus(100, ChronoUnit.SECONDS);
        this.scheduledStudyTimer = null;
        this.scheduledBreakTimer = null;
        this.bot = bot;
        this.userIdsInSession = new HashSet<>();
        userIdsInSession.add(userId);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || event.getChannel() != channel) return;

        String message = event.getMessage().getContentRaw();
        if (message.equals("!pomo-time")) {
            Instant currentTime = Instant.now();

            // Update end time to be break time if the studying time is over.
            if (currentTime.compareTo(endTime) > 0) {
                endTime = endTime.plus(25, ChronoUnit.SECONDS);
            }
            Duration timeLeft = Duration.between(currentTime, endTime);
            long millis = timeLeft.toMillis();
            double minutes = (millis / 1000.0) / 60.0;
            int wholeMinutes = (int) minutes;
            double fractionSeconds = minutes - wholeMinutes;
            int seconds = (int) Math.floor(60 * fractionSeconds);
            channel.sendMessage("There are " + wholeMinutes + " minutes and " + seconds + " seconds left.").queue();
        }
        else if (message.equals("!quit-pomo") && event.getAuthor().getId().equals(userId)) {
            scheduledStudyTimer.cancel(false);
            scheduledBreakTimer.cancel(false);
            bot.resetIdsToUpdate();
            bot.setTimerActive(false);
            channel.sendMessage("Ended timer.").queue();
            event.getJDA().removeEventListener(this);
        }
        else if (message.equals("!quit-pomo")) {
            channel.sendMessage("Only the user who started the pomodoro timer can cancel it.").queue();
        }
        else if (message.equals("!add-to-pomo") && !userIdsInSession.contains(event.getAuthor().getId())) {
            bot.addToUserIdsToUpdate(event.getAuthor().getId());
            channel.sendMessage("<@" + event.getAuthor().getId() + ">, you've been added to the pomodoro timer!").queue();
        }
        else if (message.equals("!add-to-pomo") && userIdsInSession.contains(event.getAuthor().getId())) {
            channel.sendMessage("<@" + event.getAuthor().getId() + ">, you've already been added to the pomodoro timer!").queue();
        }
    }

    public void addScheduledExecutors(ScheduledFuture<?> scheduledStudyTimer, ScheduledFuture<?> scheduledBreakTimer) {
        this.scheduledStudyTimer = scheduledStudyTimer;
        this.scheduledBreakTimer = scheduledBreakTimer;
    }
}
