package webserver;

import harmony.HarmonyHub;
import net.sourceforge.sorb.Service;
import net.sourceforge.spinnerule.*;

import java.io.IOException;

public final class SpinneruleHttpSwitcherOfferService implements Service<HttpSwitcherOffer> {
    private final SpinneruleService spinneruleService;

    public SpinneruleHttpSwitcherOfferService(HarmonyHub harmonyHub, int port) throws IOException {
        spinneruleService = new SpinneruleService(port, Context.context("/allOff", request -> {
            harmonyHub.sendAllOff();
            return Response.ok(BodiedContent.text("Switched off"));
        }));
    }

    @Override
    public HttpSwitcherOffer start() {
        final Spinnerule spinnerule = spinneruleService.start();
        return spinnerule::close;
    }
}
