import net.sourceforge.sorb.Service;
import net.sourceforge.urin.Fragment;
import net.sourceforge.urin.Path;
import net.sourceforge.urin.Urin;
import net.sourceforge.urin.scheme.http.HttpQuery;
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
    private static final Urin<String, HttpQuery, Fragment<String>> DIM_LIGHTS_URIN = HTTP.urin(authority(userInfo("HarmonyIntegration:y+pH#wR2B7pR"), ipV4Address(192, 168, 0, 15)), Path.path("dev", "sps", "io", "Watch TV", "pulse"));

    @Override
    public Loxone start() {
        final CloseableHttpClient closeableHttpClient = HttpClients.createDefault();
        return new Loxone() {
            @Override
            public void dimLights() {
                HttpGet httpget = new HttpGet(DIM_LIGHTS_URIN.asString());
                System.out.println("Executing request " + httpget.getRequestLine());

                try (final CloseableHttpResponse closeableHttpResponse = closeableHttpClient.execute(httpget)) {
                    final int statusCode = closeableHttpResponse.getStatusLine().getStatusCode();
                    if (statusCode != 200) {
                        System.err.println(EntityUtils.toString(closeableHttpResponse.getEntity()));
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to dim lights", e);
                }
            }

            @Override
            public void close() throws Exception {
                closeableHttpClient.close();
            }
        };
    }
}
