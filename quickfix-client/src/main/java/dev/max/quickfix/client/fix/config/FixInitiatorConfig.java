package dev.max.quickfix.client.fix.config;

import dev.max.fix44.custom.messages.MessageFactory;
import dev.max.quickfix.client.fix.initiator.InitiatorSessionListener;
import dev.max.quickfix.client.fix.initiator.ResponseProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import quickfix.ConfigError;
import quickfix.DefaultSessionFactory;
import quickfix.FileStoreFactory;
import quickfix.ScreenLogFactory;
import quickfix.ThreadedSocketInitiator;

@Configuration
@RequiredArgsConstructor
public class FixInitiatorConfig {

    private final FixClientSessionConfig sessionConfig;

    @Bean
    public FixInitiatorSession fixInitiatorSession() {
        return sessionConfig.getFixSession();
    }

    @Bean
    public ResponseProcessor responseProcessor(FixInitiatorSession session) {
        return new ResponseProcessor(session);
    }

    @Bean
    public ThreadedSocketInitiator initiator(FixInitiatorSession session, ResponseProcessor responseProcessor) {
        try {
            var listener = new InitiatorSessionListener(responseProcessor);
            var storeFactory = new FileStoreFactory(session.settings());
            var logFactory = new ScreenLogFactory(session.settings());
            var sessionFactory = new DefaultSessionFactory(listener, storeFactory, logFactory, new MessageFactory());
            return new ThreadedSocketInitiator(sessionFactory, session.settings());
        } catch (ConfigError e) {
            throw new RuntimeException(e);
        }
    }
}
