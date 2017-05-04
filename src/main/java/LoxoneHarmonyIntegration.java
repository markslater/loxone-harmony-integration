import harmony.SmackHarmonyHub;
import loxone.HttpLoxone;
import loxone.LoxoneCommandFailureException;

import static net.sourceforge.sorb.Sorb.sorb;

public final class LoxoneHarmonyIntegration {

    public static void main(String[] args) throws Exception {
        final AutoCloseable mainService =
                sorb(new HttpLoxone())
                        .then(loxone -> new SmackHarmonyHub(() -> {
                            try {
                                loxone.dimLights();
                            } catch (LoxoneCommandFailureException e) {
                                outputError(e);
                            }
                        }))
                        .start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                mainService.close();
            } catch (Exception e) {
                outputError(e);
            }
        }));
    }

    private static void outputError(Exception e) {
        e.printStackTrace(System.err);
    }

}
