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
    private int breakTime;

    public StateMachinePomodoroTimer(MessageChannel channel, String userId, Bot bot, int studyTime, int breakTime) {
        this.channel = channel;
        this.userId = userId;
        this.col = bot.getCol();
        this.endTime = Instant.now().plus(studyTime, ChronoUnit.MINUTES);
        this.scheduledStudyTimer = null;
        this.scheduledBreakTimer = null;
        this.bot = bot;
        this.userIdsInSession = new HashSet<>();
        userIdsInSession.add(userId);
        this.breakTime = breakTime;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || event.getChannel() != channel) return;

        String message = event.getMessage().getContentRaw();
        if (message.equals("!pomo-time")) {
            Instant currentTime = Instant.now();

            // Update end time to be break time if the studying time is over.
            if (currentTime.compareTo(endTime) > 0) {
                endTime = endTime.plus(breakTime, ChronoUnit.MINUTES);
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
            String serverId = event.getGuild().getId();
            scheduledStudyTimer.cancel(false);
            scheduledBreakTimer.cancel(false);
            bot.resetIdsToUpdate(serverId);
            bot.updateTimerActiveInServer(serverId, false);
            channel.sendMessage("Ended timer.").queue();
            event.getJDA().removeEventListener(this);
        }
        else if (message.equals("!quit-pomo")) {
            channel.sendMessage("Only the user who started the pomodoro timer can cancel it.").queue();
        }
        else if (message.equals("!add-to-pomo") && !userIdsInSession.contains(event.getAuthor().getId())) {
            if (userIdsInSession.size() >= 15) {
                channel.sendMessage("The current pomodoro session has reached maximum capacity.").queue();
                return;
            }
            String serverId = event.getGuild().getId();
            bot.addToUserIdsToUpdate(serverId, event.getAuthor().getId());
            userIdsInSession.add(event.getAuthor().getId());
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
