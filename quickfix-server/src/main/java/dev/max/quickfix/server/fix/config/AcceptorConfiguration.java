package dev.max.quickfix.server.fix.config;

import dev.max.fix44.custom.messages.MessageFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import quickfix.Application;
import quickfix.ConfigError;
import quickfix.JdbcStoreFactory;
import quickfix.LogFactory;
import quickfix.MessageStoreFactory;
import quickfix.ScreenLogFactory;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.ThreadedSocketAcceptor;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
public class AcceptorConfiguration {

    private final AcceptorProperties acceptorProperties;

    @Bean
    public ThreadedSocketAcceptor acceptor(
            Application application,
            DataSource dataSource
    ) throws ConfigError {
        final var settings = getSessionSettings();
        var messageFactory = new MessageFactory();
        var storeFactory = messageStoreFactory(settings, dataSource);
        var logFactory = logFactory(settings);
        return ThreadedSocketAcceptor.newBuilder()
                .withApplication(application)
                .withMessageStoreFactory(storeFactory)
                .withSettings(settings)
                .withLogFactory(logFactory)
                .withMessageFactory(messageFactory)
                .build();
    }

    private SessionSettings getSessionSettings() throws ConfigError {
        var settings = new SessionSettings(acceptorProperties.getConfigFile());
        settings.setLong(ThreadedSocketAcceptor.SETTING_SOCKET_ACCEPT_PORT, acceptorProperties.getPort());
        for (AcceptorProperties.ClientSession session : acceptorProperties.getSessions()) {
            var sessionId = new SessionID(
                    settings.getString(SessionSettings.BEGINSTRING),
                    session.getTarget(),
                    session.getSender());
            settings.setString(sessionId, SessionSettings.SENDERCOMPID, session.getSender());
            settings.setString(sessionId, SessionSettings.TARGETCOMPID, session.getTarget());
        }
        return settings;
    }

    private MessageStoreFactory messageStoreFactory(SessionSettings settings, DataSource dataSource) {
        var storeFactory = new JdbcStoreFactory(settings);
        storeFactory.setDataSource(dataSource);
        return storeFactory;
    }

    private LogFactory logFactory(SessionSettings settings) {
        return new ScreenLogFactory(settings);
    }
}
