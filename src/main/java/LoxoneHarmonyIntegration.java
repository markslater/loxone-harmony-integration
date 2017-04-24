import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Bind;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.sm.predicates.ForEveryStanza;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.xmlpull.v1.XmlPullParser;

public final class LoxoneHarmonyIntegration {
    public static void main(String[] args) throws Exception {
        ProviderManager.addIQProvider(Bind.ELEMENT, Bind.NAMESPACE, new IQProvider<Bind>() {
            @Override
            public Bind parse(XmlPullParser parser, int initialDepth) throws Exception {
                String name;
                Bind bind = null;
                outerloop:
                while (true) {
                    int eventType = parser.next();
                    switch (eventType) {
                        case XmlPullParser.START_TAG:
                            name = parser.getName();
                            switch (name) {
                                case "resource":
                                    String resourceString = parser.nextText();
                                    bind = Bind.newSet(Resourcepart.from(resourceString));
                                    break;
                                case "jid":
                                    EntityFullJid fullJid = JidCreate.entityFullFrom("client@" + parser.nextText());
                                    bind = Bind.newResult(fullJid);
                                    break;
                            }
                            break;
                        case XmlPullParser.END_TAG:
                            if (parser.getDepth() == initialDepth) {
                                break outerloop;
                            }
                            break;
                    }
                }
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
                super.parseAndProcessStanza(parser);
            }

            @Override
            public void sendStanza(Stanza stanza) throws SmackException.NotConnectedException, InterruptedException {
                super.sendStanza(stanza);
            }
        };

        authConnection.addPacketSendingListener(stanza -> System.out.println(stanza.toXML().toString()), ForEveryStanza.INSTANCE);
        authConnection.addSyncStanzaListener(stanza -> System.out.println(stanza.toXML().toString()), ForEveryStanza.INSTANCE);

        authConnection.connect();
        authConnection.login("guest@connect.logitech.com/gatorade.", "gatorade.", Resourcepart.from("auth"));
        authConnection.setFromMode(XMPPConnection.FromMode.USER);

//        AuthRequest sessionRequest = createSessionRequest(loginToken);
//        AuthReply oaResponse = sendOAStanza(authConnection, sessionRequest, AuthReply.class);

        authConnection.disconnect();

    }
}
