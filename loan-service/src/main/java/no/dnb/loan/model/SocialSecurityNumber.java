package no.dnb.loan.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import no.dnb.loan.configuration.SocialSecurityNumberDeserializer;
import org.springframework.lang.NonNull;

@JsonDeserialize(using = SocialSecurityNumberDeserializer.class)
public record SocialSecurityNumber(String value) {

    @NonNull
    @Override
    public String toString() {
        return String.format("%s*****", this.value.substring(0, 6));
    }
}
