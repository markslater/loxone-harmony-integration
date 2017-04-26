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

    static OaIdentity parseHarmonyHubResponse(String harmonyHubResponse) {
        final String responseBody = extractResponseBody(harmonyHubResponse);
        final String identity = extractIdentity(responseBody);
        return new OaIdentity(identity);
    }

    private static String extractResponseBody(String harmonyHubResponse) {
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
            e.printStackTrace();
        }
        return result.orElseThrow(() -> new RuntimeException("Broken"));
    }

    private static String extractIdentity(String responseBody) {
        Optional<String> result = Optional.empty();
        final Iterable<String> nameValuePairs = Splitter.on(':').split(responseBody);
        for (String nameValuePair : nameValuePairs) {
            final String prefix = "identity=";
            if (nameValuePair.startsWith(prefix)) {
                result = Optional.of(nameValuePair.substring(prefix.length()));
            }
        }
        return result.orElseThrow(() -> new RuntimeException("Broken"));
    }

    String asString() {
        return value;
    }
}
