import java.nio.file.Path;
import java.nio.file.Paths;

import comp.CompareDirs;

public class Compare {
        public static void main(String[] args) {

        if (args.length != 2) {
            System.err.println("Usage: java CompareDirs <path1> <path2>");
            return;
        }
        
        Path path1 = Paths.get(args[0]);
        Path path2 = Paths.get(args[1]);
        
        CompareDirs compare = new CompareDirs(path1, path2);
        compare.print();

        //compare.dir1.dump(compare.dir1.path.toString());
        //compare.dir2.dump(compare.dir2.path.toString());
    }
}
