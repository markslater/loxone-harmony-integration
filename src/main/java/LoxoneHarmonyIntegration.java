import net.sourceforge.urin.Fragment;
import net.sourceforge.urin.Path;
import net.sourceforge.urin.Urin;
import net.sourceforge.urin.scheme.http.HttpQuery;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

import static net.sourceforge.sorb.Sorb.sorb;
import static net.sourceforge.urin.Authority.authority;
import static net.sourceforge.urin.Host.ipV4Address;
import static net.sourceforge.urin.UserInfo.userInfo;
import static net.sourceforge.urin.scheme.http.Http.HTTP;

public final class LoxoneHarmonyIntegration {

    private static final Urin<String, HttpQuery, Fragment<String>> DIM_LIGHTS_URIN = HTTP.urin(authority(userInfo("HarmonyIntegration:y+pH#wR2B7pR"), ipV4Address(192, 168, 0, 15)), Path.path("dev", "sps", "io", "Watch TV", "pulse"));

    public static void main(String[] args) throws Exception {
        final AutoCloseable mainService = sorb(new SmackHarmonyHub(() -> {
            try {
                try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
                    HttpGet httpget = new HttpGet(DIM_LIGHTS_URIN.asString());
                    System.out.println("Executing request " + httpget.getRequestLine());

                    // Create a custom response handler
                    String responseBody = httpclient.execute(httpget, response -> {
                        int status = response.getStatusLine().getStatusCode();
                        if (status >= 200 && status < 300) {
                            HttpEntity entity = response.getEntity();
                            return entity != null ? EntityUtils.toString(entity) : null;
                        } else {
                            throw new ClientProtocolException("Unexpected response status: " + status);
                        }
                    });
                    System.out.println("----------------------------------------");
                    System.out.println(responseBody);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("On");
        })).start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                mainService.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }

}
