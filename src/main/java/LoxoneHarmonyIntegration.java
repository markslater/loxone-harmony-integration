import static net.sourceforge.sorb.Sorb.sorb;

public final class LoxoneHarmonyIntegration {

    public static void main(String[] args) throws Exception {
        final AutoCloseable mainService =
                sorb(new HttpLoxone())
                        .then(loxone -> new SmackHarmonyHub(loxone::dimLights))
                        .start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                mainService.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }

}
