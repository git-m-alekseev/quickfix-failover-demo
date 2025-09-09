package dev.max.quickfix.server.fix;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import quickfix.Acceptor;

@Component
@RequiredArgsConstructor
public class AcceptorLifecycleManager implements ApplicationRunner, DisposableBean {

    private final Acceptor acceptor;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        acceptor.start();
    }

    @Override
    public void destroy() {
        acceptor.stop();
    }
}
