package dev.max.fix.utils;

import java.util.HashMap;
import java.util.Map;

public enum ClientRequestTypes {
    PING, SUBSCRIBE, EXECUTE;

    private static final Map<String, ClientRequestTypes> map = new HashMap<>();

    static {
        for (ClientRequestTypes value : ClientRequestTypes.values()) {
            map.put(value.name(), value);
        }
    }

    public static ClientRequestTypes fromString(String value) {
        return map.get(value);
    }
}
