package dev.max.quickfix.client.fix.initiator;

import dev.max.fix.utils.MessageUtils;
import dev.max.fix44.custom.fields.MsgType;
import dev.max.fix44.custom.messages.Reject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
public class InitiatorSessionListener implements Application {

    private final ResponseProcessor messageProcessor;

    @Override
    public void onCreate(SessionID sessionID) {
        log.info("Session created: " + sessionID);
    }

    @Override
    public void onLogon(SessionID sessionID) {
        log.info("Session logged in: " + sessionID);
    }

    @Override
    public void onLogout(SessionID sessionID) {
        log.info("Session logged out: " + sessionID);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionID) {

    }

    @Override
    public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        var type = MessageUtils.getTypeOpt(message)
                .orElseThrow(() -> new IncorrectTagValue(MsgType.FIELD));
        if (type.equals(Reject.MSGTYPE)) {
            log.error("Got reject message: {}", MessageUtils.toString(message));
        }
    }

    @Override
    public void toApp(Message message, SessionID sessionID) throws DoNotSend {

    }

    @Override
    public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        messageProcessor.process(message, sessionID);
    }
}
