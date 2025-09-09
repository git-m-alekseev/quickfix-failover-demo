package dev.max.quickfix.server.fix;

import dev.max.fix.utils.MessageUtils;
import dev.max.fix44.custom.fields.MsgType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import quickfix.Application;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.RejectLogon;
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;

@Slf4j
@Component
@RequiredArgsConstructor
public class AcceptorSessionListener implements Application {

    private final MessageProcessor messageProcessor;

    @Override
    public void onCreate(SessionID sessionID) {
        log.info("Session created: {}", sessionID);
    }

    @Override
    public void onLogon(SessionID sessionID) {
        log.info("Session logged in: {}", sessionID);
    }

    @Override
    public void onLogout(SessionID sessionID) {
        log.info("Session logged out: {}", sessionID);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionID) {

    }

    @Override
    public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        var msgType = MessageUtils.getTypeOpt(message)
                .orElseThrow(() -> new IncorrectTagValue(MsgType.FIELD, null, "Missing MsgType"));
        if (MsgType.REJECT.equals(msgType)) {
            log.error("Received Reject message for sessionId: {}: {}", sessionID, MessageUtils.toString(message));
        }
    }

    @Override
    public void toApp(Message message, SessionID sessionID) throws DoNotSend {

    }

    @Override
    public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectTagValue, UnsupportedMessageType {
        messageProcessor.process(message, sessionID);
    }
}
