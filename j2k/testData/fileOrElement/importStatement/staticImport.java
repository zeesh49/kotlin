import java.util.Map.Entry;
import B.BInner;

public class A {
    void foo(Entry<Object, Object> o) {
    }

    void foo2(BInner o) {
    }
}

class B {
    public static class BInner {}
}