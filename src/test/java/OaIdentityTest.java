import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class OaIdentityTest {
    @Test
    public void canParseSampleHarmonyHubOaResponse() throws Exception {
        String sampleReponse = "<oa xmlns='connect.logitech.com' mime='vnd.logitech.connect/vnd.logitech.pair' errorcode='200' errorstring='OK'>serverIdentity=ea564946-fdce-4bac-1d01-60fcd618e3bd:hubId=106:identity=ea564946-fdce-4bac-1d01-60fcd618e3bd:status=succeeded:protocolVersion={XMPP=\"1.0\", HTTP=\"1.0\", RF=\"1.0\", WEBSOCKET=\"1.0\"}:hubProfiles={Harmony=\"2.0\"}:productId=Pimento:friendlyName=Harmony Hub</oa>";
        assertThat(OaIdentity.parseHarmonyHubResponse(sampleReponse).asString(), equalTo("ea564946-fdce-4bac-1d01-60fcd618e3bd"));
    }

}