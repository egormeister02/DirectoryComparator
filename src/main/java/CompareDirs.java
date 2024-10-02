import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.security.MessageDigest;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class CompareDirs {
    /**
     * CompareDirss two directories and their contents recursively.
     * @param args The paths to the two directories to be compared.
     */
    public static void main(String[] args) {

        if (args.length != 2) {
            System.err.println("Usage: java CompareDirs <path1> <path2>");
            return;
        }
        
        Path path1 = Paths.get(args[0]);
        Path path2 = Paths.get(args[1]);
        try {
            CompareDirs compare = new CompareDirs(path1, path2);
            compare.actionTable.dump();
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }

    private ParentDir dir1, dir2;
    private ActionTable actionTable;

    public CompareDirs(Path path1, Path path2) {
        dir1 = new ParentDir(path1);
        dir2 = new ParentDir(path2);
        actionTable = new ActionTable(dir1, dir2);
    }

    public class ParentDir {
        public Path path;
        public final HashMap<String, MyFile> files = new HashMap<String, MyFile>();
        public final HashSet<Path> dirs = new HashSet<Path>();

        ParentDir(Path parentPath) {
            // Проверяем, является ли переданный путь директорией
            if (!Files.isDirectory(parentPath)) {
                throw new IllegalArgumentException("Error: path is not a directory: " + parentPath);
            }

            try {
                this.path = parentPath;
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(parentPath)) {
                    for (Path path : stream) {
                        if (Files.isDirectory(path)) {
                            dirs.add(path);
                        } else if (Files.isRegularFile(path)) {
                            files.put(path.getFileName().toString(), new MyFile(path));
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Error directory working: " + e.getMessage());
            }
        }

        void dump(String prefix) {
            for (Path dir : dirs) {
                System.out.println(prefix + "├── " + dir.getFileName());
            }

            for (String fileName : files.keySet()) {
                System.out.println(prefix + "└── " +fileName);
            }
        }
    }

    public static class MyFile {
        Path   path;
        long   size;
        byte[] hash;

        MyFile(Path path) {
            this.path =          path;
            this.hash = getHash(path);
        }

        void dump() {
            System.out.println("Path: " + path);
            System.out.println("Size: " + size);
            System.out.println("Hash: " + hash);
        }
    }


    public class ActionTable {
        String dir1, dir2;
        private HashMap<ByteArrayKey, MyFile> interFiles1 = new HashMap<ByteArrayKey, MyFile>();
        private ArrayList<Entry> entries = new ArrayList<>();

        ActionTable(ParentDir dir1, ParentDir dir2) {
            this.dir1 = dir1.path.getFileName().toString();
            this.dir2 = dir2.path.getFileName().toString();

            for (String fileName : dir1.files.keySet()) {
                MyFile file1 = dir1.files.get(fileName);
                MyFile file2 = dir2.files.get(fileName);
                if (dir2.files.containsKey(fileName)) {
                    if (Arrays.equals(file1.hash, file2.hash)) {
                        if (file1.size == file2.size){
                            add(Action.NONE, file1.path, file2.path);
                            dir2.files.remove(fileName);
                        } else {
                            add(Action.UPDATE, file1.path, file2.path);
                            dir2.files.remove(fileName);
                        }
                    } else {
                        add(Action.UPDATE, file1.path, file2.path);
                        dir2.files.remove(fileName);
                    }
                } else {
                    interFiles1.put(new ByteArrayKey(file1.hash), file1);
                }
            }
            for (String fileName : dir2.files.keySet()) {
                MyFile file2 = dir2.files.get(fileName);
                ByteArrayKey hash = new ByteArrayKey(file2.hash);
                System.out.println(hash);
                if (interFiles1.containsKey(hash)) {
                    if (interFiles1.get(hash).size == file2.size) {
                        add(Action.RENAME, null, dir2.files.get(fileName).path);
                        interFiles1.remove(hash);
                    } else {
                        add(Action.ADD, null, dir2.files.get(fileName).path);
                    }
                } else {
                    add(Action.ADD, null, dir2.files.get(fileName).path);
                }
            }
            for (MyFile file : interFiles1.values()) {
                add(Action.REMOVE, file.path, null);
            }
        }

        public void add(Action action, Path path1, Path path2) {
            entries.add(new Entry(action, path1, path2));
        }

        public void dump() {
            int maxWidth1 = dir1.length();
            int maxWidth2 = dir2.length();
            
            for (Entry entry : entries) {
                if (entry.path1 != null && entry.path1.toString().length() > maxWidth1) {
                    maxWidth1 = entry.path1.toString().length();
                }
                if (entry.path2 != null && entry.path2.toString().length() > maxWidth2) {
                    maxWidth2 = entry.path2.toString().length();
                }
            }

            // Заголовок таблицы
            String formatHeader = "%-" + maxWidth1 + "s | %-" + maxWidth2 + "s%n";
            String formatEntry = "%-" + maxWidth1 + "s | %-" + maxWidth2 + "s%n";

            System.out.printf(formatHeader, dir1, dir2);
            System.out.printf(formatHeader, "-".repeat(maxWidth1), "-".repeat(maxWidth2));

            // Выводим каждую запись
            for (Entry entry : entries) {
                String file1 = entry.path1 != null ? entry.path1.getFileName().toString() : "";
                String file2 = entry.path2 != null ? entry.path2.getFileName().toString() : "";
                String actionPrefix = getActionSymbol(entry.action);

                System.out.printf(formatEntry, 
                                actionPrefix + file1, 
                                actionPrefix + file2);
            }
        }

        private String getActionSymbol(Action action) {
            switch (action) {
                case REMOVE:
                    return "- ";  // Удаленный файл
                case ADD:
                    return "+ ";  // Новый файл
                case UPDATE:
                    return "* ";  // Обновленный файл
                case RENAME:
                    return "r ";  // Переименованный файл
                case NONE:
                    return "  ";  // По умолчанию, без действия
                default:
                    return "HUY";
            }
        }

        public class Entry {
            Action action;
            Path path1;
            Path path2;

            Entry(Action action, Path path1, Path path2) {
                this.action = action;
                this.path1 = path1;
                this.path2 = path2;
            }
        }
    }

    public static byte[] getHash(Path path) {
        if(!Files.isReadable(path)) {
            throw new IllegalArgumentException("Error: file is not readable: " + path);
        }
        try {
            return MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
        } catch (IOException e) {
            System.err.println("Error file working: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error getting hash: " + e.getMessage());
        }
        return null;
    }

    public enum Action {
        NONE,
        ADD,
        REMOVE,
        UPDATE,
        RENAME
    }

    public class ByteArrayKey {
        private final byte[] bytes;

        public ByteArrayKey(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ByteArrayKey)) return false;
            ByteArrayKey other = (ByteArrayKey) obj;
            return Arrays.equals(this.bytes, other.bytes);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(bytes);
        }
    }
}