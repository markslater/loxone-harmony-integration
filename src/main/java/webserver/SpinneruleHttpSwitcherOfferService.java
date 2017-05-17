package webserver;

import harmony.HarmonyHub;
import net.sourceforge.sorb.Service;

public final class SpinneruleHttpSwitcherOfferService implements Service<HttpSwitcherOffer> {
    public SpinneruleHttpSwitcherOfferService(HarmonyHub harmonyHub) {
    }

    @Override
    public HttpSwitcherOffer start() {
        return () -> {
        };
    }
}
