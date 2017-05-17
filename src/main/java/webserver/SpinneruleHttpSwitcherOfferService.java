package webserver;

import harmony.HarmonyHub;
import net.sourceforge.sorb.Service;
import net.sourceforge.spinnerule.*;

import java.io.IOException;

public final class SpinneruleHttpSwitcherOfferService implements Service<HttpSwitcherOffer> {
    private final HarmonyHub harmonyHub;
    private final SpinneruleService spinneruleService;

    public SpinneruleHttpSwitcherOfferService(HarmonyHub harmonyHub) throws IOException {
        this.harmonyHub = harmonyHub;
        spinneruleService = new SpinneruleService(Context.context("/allOff", request -> {
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
