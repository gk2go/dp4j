
class Privater {

    private int[] ints = {1, 2, 3};

    private Privater() {
    }
}

public class PrintTest {

    @com.dp4j.Reflect
    public void test() {
        Privater pv = new Privater();
        final int[] d = pv.ints;
    }
}
