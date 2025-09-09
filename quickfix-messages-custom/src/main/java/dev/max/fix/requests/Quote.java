package dev.max.fix.requests;

import dev.max.fix44.custom.fields.BidPx;
import dev.max.fix44.custom.fields.ClientInstrument;
import dev.max.fix44.custom.fields.OfferPx;
import dev.max.fix44.custom.fields.ReqID;
import dev.max.fix44.custom.messages.ClientQuote;
import quickfix.FieldNotFound;

public record Quote(
        String instrument,
        double bid,
        double ask
) {

    public static Quote fromClientQuote(ClientQuote clientQuote) {
        try {
            var instrument = clientQuote.getClientInstrument().getValue();
            double bid = clientQuote.getOfferPx().getValue().doubleValue();
            double ask = clientQuote.getBidPx().getValue().doubleValue();
            return new Quote(instrument, bid, ask);
        } catch (FieldNotFound e) {
            throw new RuntimeException(e);
        }
    }

    public ClientQuote toClientQuote(String rqId) {
        var quote = new ClientQuote();
        quote.set(new ReqID(rqId));
        quote.set(new BidPx(ask));
        quote.set(new OfferPx(bid));
        quote.set(new ClientInstrument(instrument));
        return quote;
    }
}
