class T {
    private static void p(int i, Double d, String... s){}
}

public class Test{


    @com.dp4j.InjectReflection
    public void t() {
	T.p(1,new Double(2),"hello", "reflection");
    }
}