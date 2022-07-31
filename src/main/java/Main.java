import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import javax.security.auth.login.LoginException;

public class Main {
    public static void main(String[] args) throws LoginException {
        String token = System.getenv("POMODORO_BOT_TOKEN");
        String databaseConnector = System.getenv("POMODORO_MONGO_DB");
        JDABuilder.createLight(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES,
                               GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(new Bot(databaseConnector))
                .setActivity(Activity.playing("Type '!pomodoro-info' to get started!"))
                .build();
    }
}
