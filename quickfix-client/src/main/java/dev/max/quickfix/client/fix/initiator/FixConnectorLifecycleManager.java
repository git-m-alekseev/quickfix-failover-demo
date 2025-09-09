package dev.max.quickfix.client.fix.initiator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FixConnectorLifecycleManager implements ApplicationRunner, DisposableBean {

    private final FixInitiatorConnector fixConnector;

    @Override
    public void run(ApplicationArguments args) {
        fixConnector.start();
    }

    @Override
    public void destroy() {
        fixConnector.stop();
    }
}
