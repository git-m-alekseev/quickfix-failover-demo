package dev.max.fix.utils;

import dev.max.fix44.custom.fields.MsgType;

import java.util.Optional;

public final class MessageUtils {

    public static Optional<String> getTypeOpt(quickfix.Message message) {
        return message.getHeader().getOptionalString(MsgType.FIELD);
    }

    public static String toString(quickfix.Message message) {
        String msgStr = message.toRawString();
        if (msgStr == null) {
            msgStr = message.toString();
        }
        return msgStr.replace('\u0001', '|');
    }
}
