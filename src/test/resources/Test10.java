class T {
    private void p(int i, Double d, String... s){}
}

public class Test10{


    @com.dp4j.InjectReflection
    public void t() {
	p(1,new Double(2),"hello", "reflection");
    }
}

