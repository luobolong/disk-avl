import com.luobo.DiskAvlTree;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class DiskAvlTreeTest {
    @Test
    public void testCase1() throws IOException {
        DiskAvlTree diskAvlTree = new DiskAvlTree("testCase1.dat");
        diskAvlTree.insert(1);
        diskAvlTree.insert(2);
        diskAvlTree.insert(3);
        diskAvlTree.insert(4);
        diskAvlTree.insert(5);
        diskAvlTree.search(3);
        boolean search1 = diskAvlTree.search(3);
        System.out.println(search1); // true
        assert search1;
        boolean search2 = diskAvlTree.search(6);
        System.out.println(search2); // false
        assert !search2;
        diskAvlTree.delete(3);
        boolean search3 = diskAvlTree.search(3);
        System.out.println(search3); // false
        assert !search3;
    }
}
