package ru.ncedu.egormeister.compdirs;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import javax.swing.JFileChooser;
import ru.ncedu.egormeister.compdirs.comporator.CompareDirs;

public class Compare {
    public static void main(String[] args) {
        Path path1;
        Path path2;

        if (args.length == 0) {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            int returnVal1 = chooser.showOpenDialog(null);
            if (returnVal1 == JFileChooser.APPROVE_OPTION) {
                File file1 = chooser.getSelectedFile();
                path1 = file1.toPath();
            } else {
                System.err.println("First directory selection cancelled.");
                return;
            }

            int returnVal2 = chooser.showOpenDialog(null);
            if (returnVal2 == JFileChooser.APPROVE_OPTION) {
                File file2 = chooser.getSelectedFile();
                path2 = file2.toPath();
            } else {
                System.err.println("Second directory selection cancelled.");
                return;
            }
        } else if (args.length == 1){
            System.out.println("Usage: java -jar compare.jar <path to dir1> <path to dir2>");
            return;
        } else {
            path1 = Paths.get(args[0]);
            path2 = Paths.get(args[1]);
        }
            try {
            CompareDirs compare = new CompareDirs(path1, path2);
            compare.print();
            } catch (IllegalArgumentException e) {
                System.err.println(e.getMessage());
            }
        
    }
}


