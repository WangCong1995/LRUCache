import java.util.Map;

public class Main {

    public static void main(String[] args) {
        LRU<String, Integer> lru = new LRU<>(5);
        for (int i = 0; i < 10; i++) {
            lru.put("key" + i, i);
            lru.print();
            System.out.println();
        }
    }
}
