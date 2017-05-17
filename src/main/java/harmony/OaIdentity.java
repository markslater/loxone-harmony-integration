package harmony;

import com.google.common.base.Splitter;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.io.StringReader;
import java.util.Optional;

final class OaIdentity {

    private final String value;

    private OaIdentity(String value) {
        this.value = value;
    }

    static OaIdentity parseHarmonyHubResponse(String harmonyHubResponse) throws OaIdentityParseException {
        final String responseBody = extractResponseBody(harmonyHubResponse);
        final String identity = extractIdentity(responseBody);
        return new OaIdentity(identity);
    }

    private static String extractResponseBody(String harmonyHubResponse) throws OaIdentityParseException {
        Optional<String> result = Optional.empty();
        final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        try {
            final XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(new StringReader(harmonyHubResponse));
            try {
                while (xmlStreamReader.hasNext()) {
                    final int elementType = xmlStreamReader.next();

                    if (XMLEvent.START_ELEMENT == elementType) {
                        final String elementText = xmlStreamReader.getElementText();
                        result = Optional.of(elementText);
                    }
                }
            } finally {
                xmlStreamReader.close();
            }
        } catch (XMLStreamException e) {
            throw new OaIdentityParseException("Harmony Hub response is not valid XML", e);
        }
        return result.orElseThrow(() -> new OaIdentityParseException("Harmony Hub response has no content"));
    }

    private static String extractIdentity(String responseBody) throws OaIdentityParseException {
        Optional<String> result = Optional.empty();
        final Iterable<String> nameValuePairs = Splitter.on(':').split(responseBody);
        for (String nameValuePair : nameValuePairs) {
            final String prefix = "identity=";
            if (nameValuePair.startsWith(prefix)) {
                result = Optional.of(nameValuePair.substring(prefix.length()));
            }
        }
        return result.orElseThrow(() -> new OaIdentityParseException("Harmony Hub response does not contain \"identity\" key"));
    }

    String asString() {
        return value;
    }

    static final class OaIdentityParseException extends Exception {
        OaIdentityParseException(String message) {
            super(message);
        }

        OaIdentityParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
