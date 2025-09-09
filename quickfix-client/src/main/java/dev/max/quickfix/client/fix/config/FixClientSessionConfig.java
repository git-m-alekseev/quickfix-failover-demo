package dev.max.quickfix.client.fix.config;

import dev.max.quickfix.client.ex.FixInitiatorException;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import quickfix.ConfigError;
import quickfix.Initiator;
import quickfix.SessionID;
import quickfix.SessionSettings;

import java.util.List;

@Data
@Component
@ConfigurationProperties("fix.client")
public class FixClientSessionConfig {

    private String configFile;
    private ClientSessionConfig session;

    public FixInitiatorSession getFixSession() {
        return getFixInitiatorSession(session);
    }

    private FixInitiatorSession getFixInitiatorSession(ClientSessionConfig config) {
        try {
            var settings = new SessionSettings(configFile);
            var sessionId = new SessionID(
                    settings.getString(SessionSettings.BEGINSTRING),
                    config.sender,
                    config.target);
            settings.setString(sessionId, SessionSettings.SENDERCOMPID, config.sender);
            settings.setString(sessionId, SessionSettings.TARGETCOMPID, config.target);
            for (int i = 0; i < config.acceptorAddresses.size(); i++) {
                var addr = config.acceptorAddresses.get(i).split(":");
                var host = addr[0];
                var port = addr[1];
                var idx = i == 0 ? "" : String.valueOf(i);
                settings.setString(sessionId, Initiator.SETTING_SOCKET_CONNECT_HOST + idx, host);
                settings.setString(sessionId, Initiator.SETTING_SOCKET_CONNECT_PORT + idx, port);
            }
            return new FixInitiatorSession(config.clientId, sessionId, settings);
        } catch (ConfigError e) {
            throw new FixInitiatorException(e);
        }
    }

    @Data
    public static class ClientSessionConfig {
        private String clientId;
        private String sender;
        private String target;
        private List<String> acceptorAddresses;
    }
}
