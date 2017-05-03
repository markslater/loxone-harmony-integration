import static net.sourceforge.sorb.Sorb.sorb;

public final class LoxoneHarmonyIntegration {

    public static void main(String[] args) throws Exception {
        final AutoCloseable mainService = sorb(new SmackHarmonyHub()).start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                mainService.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }

}
