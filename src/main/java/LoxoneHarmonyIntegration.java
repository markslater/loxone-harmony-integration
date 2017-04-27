import argo.jdom.JdomParser;
import argo.jdom.JsonNode;
import argo.saj.InvalidSyntaxException;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaCollector;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.util.ParserUtils;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.xmlpull.v1.XmlPullParser;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.io.StringReader;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.xmlpull.v1.XmlPullParser.END_TAG;

public final class LoxoneHarmonyIntegration {

    public static void main(String[] args) throws Exception {
        ProviderManager.addIQProvider(Bind.ELEMENT, Bind.NAMESPACE, new IQProvider<Bind>() {
            @Override
            public Bind parse(XmlPullParser xmlPullParser, int initialDepth) throws Exception {
                Bind bind = null;
                do {
                    switch (xmlPullParser.getName()) {
                        case "resource":
                            bind = Bind.newSet(Resourcepart.from(xmlPullParser.nextText()));
                            break;
                        case "jid":
                            bind = Bind.newResult(JidCreate.entityFullFrom("client@" + xmlPullParser.nextText()));
                    }
                } while (xmlPullParser.next() != END_TAG && xmlPullParser.getDepth() != initialDepth);
                return bind;
            }
        });

        final XMPPTCPConnectionConfiguration xmpptcpConnectionConfiguration = XMPPTCPConnectionConfiguration.builder()
                .setHost("192.168.0.4")
                .setPort(5222)
                .setXmppDomain("192.168.0.4")
                .build();
        final XMPPTCPConnection authConnection = new XMPPTCPConnection(xmpptcpConnectionConfiguration) {
            @Override
            protected void parseAndProcessStanza(XmlPullParser parser) throws Exception {
                ParserUtils.assertAtStartTag(parser);
                int parserDepth = parser.getDepth();
                Stanza stanza = null;
                try {
                    if (IQ.IQ_ELEMENT.equals(parser.getName()) && parser.getAttributeValue("", "type") == null) {
                        // Acknowledgement IQs don't contain a type so an empty result is created here to prevent a parsing NPE
                        stanza = new EmptyResultIQ();
                    } else {
                        stanza = PacketParserUtils.parseStanza(parser);
                    }
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
                ParserUtils.assertAtEndTag(parser);
                if (stanza != null) {
                    processStanza(stanza);
                }
            }

            @Override
            public void sendStanza(Stanza stanza) throws SmackException.NotConnectedException, InterruptedException {
                if (stanza.getError() == null || stanza.getError().getCondition() != XMPPError.Condition.service_unavailable) {
                    super.sendStanza(stanza);
                }
            }
        };

//        authConnection.addPacketSendingListener(stanza -> System.out.println(stanza.toXML().toString()), ForEveryStanza.INSTANCE);
//        authConnection.addSyncStanzaListener(stanza -> System.out.println(stanza.toXML().toString()), ForEveryStanza.INSTANCE);

        authConnection.connect();
        authConnection.login("guest@connect.logitech.com/gatorade.", "gatorade.", Resourcepart.from("auth"));
        authConnection.setFromMode(XMPPConnection.FromMode.USER);

        final Stanza iq = new IQ(new SimpleIQ("oa", "connect.logitech.com") {
        }) {
            @Override
            protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
                final String mimeType = "vnd.logitech.connect/vnd.logitech.pair";
                xml.attribute("mime", mimeType);
                xml.rightAngleBracket();
                xml.append("method=pair:name=").append(java.util.Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes())).append("#iOS6.0.1#iPhone");
                return xml;
            }
        };

        final StanzaCollector stanzaCollector = authConnection.createStanzaCollector(stanza -> iq.getStanzaId().equals(stanza.getStanzaId()));
        authConnection.sendStanza(iq);
        final Stanza stanza = stanzaCollector.nextResult(5000);
        // ugh... username is identity + "@connect.logitech.com/gatorade"
        // password is identity
        // all the other jibber jabber can go straight in the bin.
        // once we've got username and password we make *another* connection using those rather than the stuff we had before.
        final OaIdentity oaIdentity = OaIdentity.parseHarmonyHubResponse(((UnparsedIQ) stanza).getContent().toString());
        authConnection.disconnect();

        final XMPPTCPConnection mainConnection = new XMPPTCPConnection(xmpptcpConnectionConfiguration) {
            @Override
            protected void parseAndProcessStanza(XmlPullParser parser) throws Exception {
                ParserUtils.assertAtStartTag(parser);
                int parserDepth = parser.getDepth();
                Stanza stanza = null;
                try {
                    if (IQ.IQ_ELEMENT.equals(parser.getName()) && parser.getAttributeValue("", "type") == null) {
                        // Acknowledgement IQs don't contain a type so an empty result is created here to prevent a parsing NPE
                        stanza = new EmptyResultIQ();
                    } else {
                        stanza = PacketParserUtils.parseStanza(parser);
                    }
                } catch (Exception e) {
                    CharSequence content = PacketParserUtils.parseContentDepth(parser, parserDepth);
                    System.err.println(e.getMessage());
                }
                ParserUtils.assertAtEndTag(parser);
                if (stanza != null) {
                    processStanza(stanza);
                }
            }

            @Override
            public void sendStanza(Stanza stanza) throws SmackException.NotConnectedException, InterruptedException {
                if (stanza.getError() == null || stanza.getError().getCondition() != XMPPError.Condition.service_unavailable) {
                    super.sendStanza(stanza);
                }
            }
        };

//        mainConnection.addPacketSendingListener(s -> System.out.println(s.toXML().toString()), ForEveryStanza.INSTANCE);
//        mainConnection.addSyncStanzaListener(s -> System.out.println(s.toXML().toString()), ForEveryStanza.INSTANCE);

        mainConnection.connect();
        mainConnection.login(oaIdentity.asString() + "@connect.logitech.com/gatorade", oaIdentity.asString(), Resourcepart.from("main"));
        mainConnection.setFromMode(XMPPConnection.FromMode.USER);

        mainConnection.addSyncStanzaListener(new StanzaListener() {
            @Override
            public void processStanza(Stanza stanza) throws SmackException.NotConnectedException {
                System.out.println("On");
            }
        }, new StanzaFilter() {
            @Override
            public boolean accept(Stanza stanza) {
                ExtensionElement event = stanza.getExtension("event", "connect.logitech.com");
                if (event == null) {
                    return false;
                }
                final String type = ((StandardExtensionElement) event).getAttributeValue("type");
                if (!"connect.stateDigest?notify".equals(type)) {
                    return false;
                }

                final String text = ((StandardExtensionElement) event).getText();
                final JsonNode jsonNode;
                try {
                    jsonNode = new JdomParser().parse(text);
                } catch (InvalidSyntaxException e) {
                    e.printStackTrace();
                    return false;
                }

                return
                        jsonNode.isNumberValue("activityStatus") && "1".equals(jsonNode.getNumberValue("activityStatus"))
//                        && jsonNode.isNumberValue("activityId") && "23648476".equals("activityId"); // fire tv
                                && jsonNode.isStringValue("activityId") && "23649686".equals(jsonNode.getStringValue("activityId")); // living room sonos
            }
        });

        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    mainConnection.sendStanza(new IQ(new SimpleIQ("oa", "connect.logitech.com") {
                    }) {
                        @Override
                        protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
                            final String mimeType = "vnd.logitech.connect/vnd.logitech.ping";
                            xml.attribute("mime", mimeType);
                            xml.rightAngleBracket();
                            return xml;
                        }
                    });
                } catch (SmackException.NotConnectedException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, 30, 30, SECONDS);


        Thread.sleep(100000);
        scheduledExecutorService.shutdown();
        mainConnection.disconnect();
    }

    private static String extractResponseBody(String harmonyHubEvent) throws OaIdentity.OaIdentityParseException {
        Optional<String> result = Optional.empty();
        final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        try {
            final XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(new StringReader(harmonyHubEvent));
            try {
                while (xmlStreamReader.hasNext()) {
                    final int elementType = xmlStreamReader.next();

                    if (
                            XMLEvent.START_ELEMENT == elementType
                                    && "event".equals(xmlStreamReader.getLocalName())
                                    && "connect.stateDigest?notify".equals(xmlStreamReader.getAttributeValue(null, "type"))
                            ) {
                        final String elementText = xmlStreamReader.getElementText();
                        result = Optional.of(elementText);
                    }
                }
            } finally {
                xmlStreamReader.close();
            }
        } catch (XMLStreamException e) {
            throw new OaIdentity.OaIdentityParseException("Harmony Hub response is not valid XML", e);
        }
        return result.orElseThrow(() -> new OaIdentity.OaIdentityParseException("Harmony Hub response has no content"));
    }

}
