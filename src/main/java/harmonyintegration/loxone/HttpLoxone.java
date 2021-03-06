package harmonyintegration.loxone;

import net.sourceforge.sorb.Service;
import net.sourceforge.urin.Fragment;
import net.sourceforge.urin.Path;
import net.sourceforge.urin.Urin;
import net.sourceforge.urin.scheme.http.HttpQuery;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

import static net.sourceforge.urin.Authority.authority;
import static net.sourceforge.urin.Host.ipV4Address;
import static net.sourceforge.urin.UserInfo.userInfo;
import static net.sourceforge.urin.scheme.http.Http.HTTP;

public final class HttpLoxone implements Service<Loxone> {
    private static final Urin<String, HttpQuery, Fragment<String>> DIM_LIGHTS_URIN = HTTP.urin(authority(userInfo("HarmonyIntegration:y+pH#wR2B7pR"), ipV4Address(192, 168, 86, 15)), Path.path("dev", "sps", "io", "Watch TV", "pulse"));

    @Override
    public Loxone start() {
        final CloseableHttpClient closeableHttpClient = HttpClients.createDefault();
        return new Loxone() {
            @Override
            public void dimLights() throws LoxoneCommandFailureException {
                try (CloseableHttpResponse closeableHttpResponse = closeableHttpClient.execute(new HttpGet(DIM_LIGHTS_URIN.asString()))) {
                    final int statusCode = closeableHttpResponse.getStatusLine().getStatusCode();
                    if (statusCode != 200) {
                        final String response;
                        try {
                            response = EntityUtils.toString(closeableHttpResponse.getEntity());
                        } catch (IOException | ParseException | IllegalArgumentException e) {
                            throw new LoxoneCommandFailureException("Expected HTTP response code 200, but got " + statusCode + ", and then failed to get response body", e);
                        }
                        throw new LoxoneCommandFailureException("Expected HTTP response code 200, but got " + statusCode + ", and response: " + response);
                    }
                } catch (IOException e) {
                    throw new LoxoneCommandFailureException("Failed to execute HTTP request to dim lights", e);
                }
            }

            @Override
            public void close() throws Exception {
                closeableHttpClient.close();
            }
        };
    }
}
