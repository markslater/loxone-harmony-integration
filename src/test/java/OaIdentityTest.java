import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OaIdentityTest {
    @Test
    void canParseSampleHarmonyHubOaResponse() throws Exception {
        String sampleReponse = "<oa xmlns='connect.logitech.com' mime='vnd.logitech.connect/vnd.logitech.pair' errorcode='200' errorstring='OK'>serverIdentity=ea564946-fdce-4bac-1d01-60fcd618e3bd:hubId=106:identity=ea564946-fdce-4bac-1d01-60fcd618e3bd:status=succeeded:protocolVersion={XMPP=\"1.0\", HTTP=\"1.0\", RF=\"1.0\", WEBSOCKET=\"1.0\"}:hubProfiles={Harmony=\"2.0\"}:productId=Pimento:friendlyName=Harmony Hub</oa>";
        assertThat(OaIdentity.parseHarmonyHubResponse(sampleReponse).asString(), equalTo("ea564946-fdce-4bac-1d01-60fcd618e3bd"));
    }

    @Test
    void parsingInvalidXmlThrowsOaIdentityParseException() throws Exception {
        OaIdentity.OaIdentityParseException exception = assertThrows(OaIdentity.OaIdentityParseException.class, () -> {
            OaIdentity.parseHarmonyHubResponse("barf").asString();
        });
        assertThat(exception.getMessage(), equalTo("Harmony Hub response is not valid XML"));
    }

    @Test
    void parsingResponseWithNoContentThrowsOaIdentityParseException() throws Exception {
        OaIdentity.OaIdentityParseException exception = assertThrows(OaIdentity.OaIdentityParseException.class, () -> {
            OaIdentity.parseHarmonyHubResponse("<oa xmlns='connect.logitech.com' mime='vnd.logitech.connect/vnd.logitech.pair' errorcode='200' errorstring='OK'/>").asString();
        });
        assertThat(exception.getMessage(), equalTo("Harmony Hub response does not contain \"identity\" key"));
    }

    @Test
    void parsingResponseWithNoIdentityKeyThrowsOaIdentityParseException() throws Exception {
        OaIdentity.OaIdentityParseException exception = assertThrows(OaIdentity.OaIdentityParseException.class, () -> {
            OaIdentity.parseHarmonyHubResponse("<oa xmlns='connect.logitech.com' mime='vnd.logitech.connect/vnd.logitech.pair' errorcode='200' errorstring='OK'>serverIdentity=ea564946-fdce-4bac-1d01-60fcd618e3bd:hubId=106:status=succeeded:protocolVersion={XMPP=\"1.0\", HTTP=\"1.0\", RF=\"1.0\", WEBSOCKET=\"1.0\"}:hubProfiles={Harmony=\"2.0\"}:productId=Pimento:friendlyName=Harmony Hub</oa>").asString();
        });
        assertThat(exception.getMessage(), equalTo("Harmony Hub response does not contain \"identity\" key"));
    }

}