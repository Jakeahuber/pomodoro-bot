import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import java.util.Map;
import javax.security.auth.login.LoginException;

public class Main {
    public static void main(String[] args) throws LoginException {
        Map<String, String> env = System.getenv();
        String token = env.get("POMODORO_BOT_TOKEN");
        String databaseConnector = env.get("POMODORO_MONGO_DB");
        JDABuilder.createLight(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES,
                               GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(new Bot(databaseConnector))
                .setActivity(Activity.playing("Type '!pomodoro-25-5' to get started!"))
                .build();
    }
}
