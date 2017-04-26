import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaCollector;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.sm.predicates.ForEveryStanza;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.util.ParserUtils;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.xmlpull.v1.XmlPullParser;

import java.util.UUID;

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

        authConnection.addPacketSendingListener(stanza -> System.out.println(stanza.toXML().toString()), ForEveryStanza.INSTANCE);
        authConnection.addSyncStanzaListener(stanza -> System.out.println(stanza.toXML().toString()), ForEveryStanza.INSTANCE);

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
        System.out.println("oaIdentity.asString() = " + oaIdentity.asString());
        authConnection.disconnect();

    }
}
