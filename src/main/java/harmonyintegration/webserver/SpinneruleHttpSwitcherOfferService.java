package harmonyintegration.webserver;

import harmonyintegration.harmony.HarmonyHub;
import net.sourceforge.sorb.Service;
import net.sourceforge.spinnerule.*;

import java.io.IOException;
import java.net.URI;

public final class SpinneruleHttpSwitcherOfferService implements Service<HttpSwitcherOffer> {
    private final SpinneruleService spinneruleService;

    public SpinneruleHttpSwitcherOfferService(final HarmonyHub harmonyHub, final int port) throws IOException {
        spinneruleService = new SpinneruleService(port, new Auditor() {
            @Override
            public void successfullyHandledRequest(final URI requestURI, final String protocol, final String requestMethod, final RequestHeaders requestHeaders, final String clientIpAddress, final int responseCode, final long responseLengthBytes) {
                System.out.println("requestURI = " + requestURI);
                System.out.println("protocol = " + protocol);
                System.out.println("requestMethod = " + requestMethod);
                System.out.println("requestHeaders = " + requestHeaders);
                System.out.println("clientIpAddress = " + clientIpAddress);
                System.out.println("responseCode = " + responseCode);
                System.out.println("responseLengthBytes = " + responseLengthBytes);
            }

            @Override
            public void failedHandlingRequest(final IOException e) {
                e.printStackTrace();
            }

            @Override
            public void failedHandlingRequest(final RuntimeException e) {
                e.printStackTrace();
            }
        }, Context.context("/allOff", request -> {
            harmonyHub.sendAllOff();
            return Response.ok(BodiedContent.text("Switched off"));
        }), Context.context("/", request -> Response.notFound(EmptyContent.emptyContent())));
    }

    @Override
    public HttpSwitcherOffer start() {
        final Spinnerule spinnerule = spinneruleService.start();
        return spinnerule::close;
    }
}
