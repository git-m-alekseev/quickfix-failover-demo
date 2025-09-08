package dev.max.quickfix.client.fix.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties("fix.client")
public class FixClientSessionConfig {

    private String configFile;
    private List<ClientSessionConfig> sessions;

    @Data
    public static class ClientSessionConfig {
        private String clientId;
        private String sender;
        private String target;
        private List<AcceptorAddress> acceptorAddresses;
    }

    record AcceptorAddress(String host, int port) { }
}
