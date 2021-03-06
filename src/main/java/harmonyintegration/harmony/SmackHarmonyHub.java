package harmonyintegration.harmony;

import argo.jdom.JdomParser;
import argo.jdom.JsonNode;
import argo.saj.InvalidSyntaxException;
import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.sourceforge.sorb.Service;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaCollector;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
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

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

import static com.google.common.base.Charsets.UTF_8;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.xmlpull.v1.XmlPullParser.END_TAG;

public final class SmackHarmonyHub implements Service<HarmonyHub> {

    private static final String HARMONY_HUB = Joiner.on('.').join("192", "168", "86", "3");

    private final ActivityStartListener activityStartListener;
    private final PingFailureExceptionListener pingFailureExceptionListener;
    private final Stopwatch stopwatch = Stopwatch.createStarted();

    public SmackHarmonyHub(final ActivityStartListener activityStartListener, final PingFailureExceptionListener pingFailureExceptionListener) {
        this.activityStartListener = activityStartListener;
        this.pingFailureExceptionListener = pingFailureExceptionListener;
    }

    @Override
    public HarmonyHub start() {
        try {
            ProviderManager.addIQProvider(Bind.ELEMENT, Bind.NAMESPACE, new IQProvider<Bind>() {
                @Override
                public Bind parse(final XmlPullParser xmlPullParser, final int initialDepth) throws Exception {
                    Bind bind = null;
                    do {
                        final String name = xmlPullParser.getName();
                        if ("resource".equals(name)) {
                            bind = Bind.newSet(Resourcepart.from(xmlPullParser.nextText()));

                        } else if ("jid".equals(name)) {
                            bind = Bind.newResult(JidCreate.entityFullFrom("client@" + xmlPullParser.nextText()));

                        }
                    } while (xmlPullParser.next() != END_TAG && xmlPullParser.getDepth() != initialDepth);
                    return bind;
                }
            });

            final XMPPTCPConnectionConfiguration xmpptcpConnectionConfiguration = XMPPTCPConnectionConfiguration.builder()
                    .setHost(HARMONY_HUB)
                    .setPort(5222)
                    .setXmppDomain(HARMONY_HUB)
                    .build();
            final XMPPTCPConnection authConnection = new XMPPTCPConnection(xmpptcpConnectionConfiguration) {
                @Override
                protected void parseAndProcessStanza(final XmlPullParser parser) throws Exception {
                    ParserUtils.assertAtStartTag(parser);
                    Stanza stanza;
                    if (IQ.IQ_ELEMENT.equals(parser.getName()) && parser.getAttributeValue("", "type") == null) {
                        // Acknowledgement IQs don't contain a type so an empty result is created here to prevent a parsing NPE
                        stanza = new EmptyResultIQ();
                    } else {
                        stanza = PacketParserUtils.parseStanza(parser);
                    }
                    ParserUtils.assertAtEndTag(parser);
                    if (stanza != null) {
                        processStanza(stanza);
                    }
                }

                @Override
                public void sendStanza(final Stanza stanza) throws SmackException.NotConnectedException, InterruptedException {
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
                protected IQChildElementXmlStringBuilder getIQChildElementBuilder(final IQChildElementXmlStringBuilder xml) {
                    final String mimeType = "vnd.logitech.connect/vnd.logitech.pair";
                    xml.attribute("mime", mimeType);
                    xml.rightAngleBracket();
                    xml.append("method=pair:name=").append(java.util.Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes(UTF_8))).append("#iOS6.0.1#iPhone");
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
                protected void parseAndProcessStanza(final XmlPullParser parser) throws Exception {
                    ParserUtils.assertAtStartTag(parser);
                    Stanza stanza;
                    if (IQ.IQ_ELEMENT.equals(parser.getName()) && parser.getAttributeValue("", "type") == null) {
                        // Acknowledgement IQs don't contain a type so an empty result is created here to prevent a parsing NPE
                        stanza = new EmptyResultIQ();
                    } else {
                        stanza = PacketParserUtils.parseStanza(parser);
                    }
                    ParserUtils.assertAtEndTag(parser);
                    if (stanza != null) {
                        processStanza(stanza);
                    }
                }

                @Override
                public void sendStanza(final Stanza stanza) throws SmackException.NotConnectedException, InterruptedException {
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

            mainConnection.addSyncStanzaListener(ignored -> activityStartListener.activityStartTriggered(), stanza12 -> {
                final ExtensionElement event = stanza12.getExtension("event", "connect.logitech.com");
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
                    // TODO restart me??
                    return false;
                }

                return
                        jsonNode.isNumberValue("activityStatus") && "1".equals(jsonNode.getNumberValue("activityStatus"))
                                && jsonNode.isStringValue("activityId") && "42155155".equals(jsonNode.getStringValue("activityId")); // fire tv
            });

            final ScheduledExecutorService scheduledExecutorService = newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("Harmony ping/keepalive thread %d").build());
            scheduledExecutorService.scheduleAtFixedRate(() -> {
                try {
                    mainConnection.sendStanza(new IQ(new SimpleIQ("oa", "connect.logitech.com") {
                    }) {
                        @Override
                        protected IQChildElementXmlStringBuilder getIQChildElementBuilder(final IQChildElementXmlStringBuilder xml) {
                            final String mimeType = "vnd.logitech.connect/vnd.logitech.ping";
                            xml.attribute("mime", mimeType);
                            xml.rightAngleBracket();
                            return xml;
                        }
                    });
                } catch (SmackException.NotConnectedException e) {
                    pingFailureExceptionListener.pingFailed(e);
                    // TODO restart me
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    // TODO what do we do when this crap happens??
                }
            }, 30, 30, SECONDS);

            return new HarmonyHub() {
                @Override
                public void sendAllOff() {
                    try {
                        mainConnection.sendStanza(new IQ(new SimpleIQ("oa", "connect.logitech.com") {
                        }) {
                            @Override
                            protected IQChildElementXmlStringBuilder getIQChildElementBuilder(final IQChildElementXmlStringBuilder xml) {
                                final String mimeType = "vnd.logitech.harmony/vnd.logitech.harmony.engine?startactivity";
                                xml.attribute("mime", mimeType);
                                xml.rightAngleBracket();
                                xml.append("activityId=-1:timestamp=").append(String.valueOf(stopwatch.elapsed(MILLISECONDS)));
                                return xml;
                            }
                        });
                    } catch (SmackException.NotConnectedException | InterruptedException e) {
                        e.printStackTrace();
                        // TODO What do we do when this crap happens?
                    }
                }

                @Override
                public void close() {
                    scheduledExecutorService.shutdown();
                    mainConnection.disconnect();
                }
            };
        } catch (SmackException | IOException | XMPPException | OaIdentity.OaIdentityParseException | InterruptedException e) {
            throw new SmackHarmonyHubStartupException("Failed to start Smack Harmony client", e);
            // TODO audit and restart me ??
        }
    }

    @FunctionalInterface
    public interface ActivityStartListener {
        void activityStartTriggered();
    }

    @FunctionalInterface
    public interface PingFailureExceptionListener {
        void pingFailed(SmackException.NotConnectedException e);
    }
}
