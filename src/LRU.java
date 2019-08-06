import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 基于 双向链表 + HashMap 的 LRU 算法实现，对算法的解释如下：
 * 1.访问某个节点时，将其从原来的位置删除，并重新插入到链表头部。
 * 这样就能保证链表尾部存储的就是最近最久未使用的节点，当节点数量大于缓存最大空间时就淘汰链表尾部的节点。
 * <p>
 * 2.为了使删除操作时间复杂度为 O(1)，就不能采用遍历的方式找到某个节点。
 * HashMap 存储着 Key 到节点的映射，通过 Key 就能以 O(1) 的时间得到节点，然后再以 O(1) 的时间将其从双向队列中删除。
 *
 * @param <K> 缓存结点的key的泛型
 * @param <V> 缓存结点的value的泛型
 */
public class LRU<K, V> implements Iterable<K> {

    private Node head; /* 头指针：指向链表的第一个存放缓存数据的结点 */
    private Node tail; /* 尾指针：指向链表的最后一个存放缓存数据的结点 */
    private HashMap<K, Node> map; /* 存放key到结点的映射。有了结点的key，我们就能在O(1)时间内查找到该结点 */
    private int cacheCapacity; /* LRU缓存空间的容量 */


    /**
     * 构造函数:做一些初始化的工作。
     *
     * @param cacheCapacity 缓存容量
     */
    public LRU(int cacheCapacity) {
        this.cacheCapacity = cacheCapacity;

        /*  源码中超过了阈值hashMap才会扩容。等于阈值并不会扩容!
            if (++size > threshold)
                resize();
        */
        /* 因为HashMap的装载因子是0.75，所以hashmap在使用了四分之三的空间后，就会自动扩容。
        而我们的缓存空间是不能扩容的，所以，我们只能使用四分之三的HashMap空间。
        因此我们设置HashMap容量的时候，应该将其设置为(cacheCapa+1)的三分之四倍 */
        this.map = new HashMap<>((int) (Math.ceil((cacheCapacity + 1) / 0.75f)));// +1的原因是: 即使缓存溢出（即缓存中存放了cacheCapacity+1个数据）时，hashMap也不会扩容。

        head = new Node(null, null);
        tail = new Node(null, null);

        head.next = tail;
        tail.pre = head;
    }

    /**
     * 根据key访问一个缓存中的结点，返回其value值
     *
     * @param key
     * @return
     */
    public V get(K key) {

        if (!map.containsKey(key)) {   /* 如果map中不存在这个key，说明缓存中 不存在这个key对应的结点。于是返回null */
            return null;
        }

        Node node = map.get(key); /* 从map中获取我们要访问的结点。只用花O(1)的时间 */

        /* 将最近访问的结点，从当前位置删除，然后插入到队头 */
        unlink(node);
        insertHead(node);

        return node.v;  /* 返回结点的value */
    }

    /**
     * 将数据存放入LRU缓存
     *
     * @param key
     * @param value
     */
    public void put(K key, V value) {

        if (map.containsKey(key)) { //若缓存中已经存在了这个key
            Node node = map.get(key);
            unlink(node);//将原来的结点删除
        }

        /* 若有原来的结点，则把原来的结点删除。反正插入队头的，一定是新建的结点 */
        Node node = new Node(key, value); //用传入的key和value，新建一个node结点
        map.put(key, node);//将映射关系存入map，便于我们以后以O(1)的时间查找到结点
        insertHead(node);//将新建的节点，插入到队头

        /* 如果插入新的节点后，缓存中的结点数量超过了 缓存容量，则我们需要删除缓存中的 最久未被访问结点。*/
        if (map.size() > cacheCapacity) {
            Node toRemove = removeTail();//删除链表尾端的缓存结点
            map.remove(toRemove.k);//更新映射表。将被删除结点的映射关系，从map中删除
        }
    }

    /**
     * 将结点从双向链表中删除
     *
     * @param node 当前待删除结点
     */
    public void unlink(Node node) {
        Node preNode = node.pre; //当前结点的前驱结点
        Node nextNode = node.next; //当前结点的后继结点

        /* 与当前结点相关的四个指针都要处理！ */
        preNode.next = nextNode;
        nextNode.pre = preNode;

        node.pre = null;//切断这个结点和链表的所有联系。不然这个结点还是和链表有联系。这样JVM节不会回收这个对象
        node.next = null;
    }

    /**
     * 使用头插法，将结点插入到链表头。
     * （注意顺序，以免断链）
     * （head相当于头指针）
     *
     * @param node
     */
    private void insertHead(Node node) {

        Node nextNode = head.next;

        /* 处理node的四个指针 */
        node.next = nextNode;
        nextNode.pre = node;

        node.pre = head;
        head.next = node;
    }

    /**
     * 删除链表的最后一个缓存结点，并将其返回
     *
     * @return
     */
    private Node removeTail() {
        Node node = tail.pre;//node是当前要被删除的结点
        Node preNode = node.pre;

        /* 四个指针都要处理 */
        tail.pre = preNode;
        preNode.next = tail;

        node.pre = null;
        node.next = null;

        return node;
    }


    /**
     * 实现了Iterator接口的iterator()方法后，对象就可以用foreach遍历
     *
     * @return
     */
    @Override
    public Iterator<K> iterator() {

        return new Iterator<K>() {
            private Node cur = head.next;//cur指向当前遍历到的结点

            @Override
            public boolean hasNext() {
                return cur != tail;//当前指针指向tail结点时，会返回false
            }

            @Override
            public K next() {
                Node node = cur;
                cur = cur.next;
                return node.k;//返回当前结点的key
            }
        };
    }

    /**
     * 打印缓存中的所有数据
     */
    public void print() {
        Iterator iter=this.iterator();
        while (iter.hasNext()){
            System.out.print(iter.next());
        }

    }

    /**
     * 内部类：双向链表中的一个结点。
     * 使用双向链表可以将查找前驱结点的时间从O(n)减小到O(1)，
     * 从而在删除链表中的结点的时候，节省了找前驱结点需要的O(n)的时间。
     * 因此现在删除结点的时间复杂度是O(1).
     */
    private class Node {
        Node pre; /* 前驱结点 */
        Node next;  /* 后继结点 */
        K k; /* 缓存结点中存放的key */
        V v; /* 缓存结点中存放的value */

        /**
         * 构造函数
         *
         * @param k
         * @param v
         */
        public Node(K k, V v) {
            this.k = k;
            this.v = v;
        }
    }
}
