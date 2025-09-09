package dev.max.quickfix.server.fix.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties("fix.server")
public class AcceptorProperties {

    private int port = 9878;
    private String configFile;
    private List<ClientSession> sessions;


    @Data
    public static class ClientSession {
        private String clientId;
        private String sender;
        private String target;
    }
}
