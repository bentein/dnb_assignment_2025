package no.dnb.loan.configuration;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import no.dnb.loan.model.SocialSecurityNumber;

import java.io.IOException;

public class SocialSecurityNumberDeserializer extends StdDeserializer<SocialSecurityNumber> {

    public SocialSecurityNumberDeserializer() {
        this(null);
    }

    public SocialSecurityNumberDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public SocialSecurityNumber deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        var node = jsonParser.getCodec().readTree(jsonParser);
        return new SocialSecurityNumber(node.toString());
    }
}
