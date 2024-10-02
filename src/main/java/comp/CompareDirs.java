package comp;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.security.MessageDigest;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class CompareDirs {

    public ParentDir dir1, dir2;
    private ActionTable actionTable;

    public CompareDirs(Path path1, Path path2) {
        dir1 = new ParentDir(path1);
        dir2 = new ParentDir(path2);
        actionTable = new ActionTable(dir1, dir2);
    }

    public void print() {
        actionTable.dump();
    }

    public class ParentDir {
        public Path path;
        public HashMap<String, MyFile> files = new HashMap<String, MyFile>();
        public HashSet<Path> dirs = new HashSet<Path>();

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

        public void dump(String prefix) {
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

        MyFile(Path path) throws IOException{
            this.path =             path;
            this.hash =    getHash(path);
            this.size = Files.size(path);
        }

        public static byte[] getHash(Path path) throws IOException {
            if(!Files.isReadable(path)) {
                throw new IllegalArgumentException("Error: file is not readable: " + path);
            }
            try {
                return MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
            }  catch (NoSuchAlgorithmException e) {
                System.err.println("Error getting hash: " + e.getMessage());
            }
            return null;
        }

        void dump() {
            System.out.println("Path: " + path);
            System.out.println("Size: " + size);
            System.out.println("Hash: " + hash);
        }
    }


    public class ActionTable {
        String dir1, dir2;
        private ArrayList<Entry> entries = new ArrayList<>();

        ActionTable(ParentDir dir1, ParentDir dir2) {
            this.dir1 = dir1.path.getFileName().toString();
            this.dir2 = dir2.path.getFileName().toString();

            Map<String, MyFile> dir2FilesCopy = new HashMap<>(dir2.files);
            Map<ByteArrayKey, MyFile> interFiles1 = new HashMap<>();

            for (String fileName : dir1.files.keySet()) {
                MyFile file1 = dir1.files.get(fileName);
                MyFile file2 = dir2FilesCopy.get(fileName);

                if (dir2FilesCopy.containsKey(fileName)) {
                    if (Arrays.equals(file1.hash, file2.hash)) {
                        if (file1.size == file2.size) {
                            add(Action.NONE, file1.path, file2.path);
                            dir2FilesCopy.remove(fileName);
                        } else {
                            add(Action.UPDATE, file1.path, file2.path);
                            dir2FilesCopy.remove(fileName);
                        }
                    } else {
                        add(Action.UPDATE, file1.path, file2.path);
                        dir2FilesCopy.remove(fileName);
                    }
                } else {
                    interFiles1.put(new ByteArrayKey(file1.hash), file1);
                }
            }

            for (MyFile file2 : dir2FilesCopy.values()) {
                ByteArrayKey hash = new ByteArrayKey(file2.hash);
                System.out.println(hash);

                if (interFiles1.containsKey(hash)) {
                    if (interFiles1.get(hash).size == file2.size) {
                        add(Action.RENAME, interFiles1.get(hash).path, file2.path);
                        interFiles1.remove(hash);
                    } else {
                        add(Action.ADD, null, file2.path);
                    }
                } else {
                    add(Action.ADD, null, file2.path);
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
            
            // Определяем максимальные ширины для колонок
            for (Entry entry : entries) {
                if (entry.path1 != null && entry.path1.toString().length() > maxWidth1) {
                    maxWidth1 = entry.path1.toString().length();
                }
                if (entry.path2 != null && entry.path2.toString().length() > maxWidth2) {
                    maxWidth2 = entry.path2.toString().length();
                }
            }
        
            // Формат заголовка и записей
            String formatHeader = "%-" + maxWidth1 + "s | %-" + maxWidth2 + "s%n";
            String formatEntry = "%-" + maxWidth1 + "s | %-" + maxWidth2 + "s%n";
        
            // Вывод заголовка таблицы
            System.out.printf(formatHeader, dir1, dir2);
            System.out.printf(formatHeader, "-".repeat(maxWidth1), "-".repeat(maxWidth2));
        
            // Вывод каждой записи
            for (Entry entry : entries) {
                // Получаем имена файлов или пустую строку, если путь равен null
                String file1 = entry.path1 != null ? entry.path1.getFileName().toString() : "";
                String file2 = entry.path2 != null ? entry.path2.getFileName().toString() : "";
        
                // Получаем символ действия только для соответствующего пути
                String actionPrefix1 = entry.path1 != null ? getActionSymbol(entry.action) : "";
                String actionPrefix2 = entry.path2 != null ? getActionSymbol(entry.action) : "";
        
                // Выводим запись с разными символами для каждой колонки
                System.out.printf(formatEntry, actionPrefix1 + file1, actionPrefix2 + file2);
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
                    return "~ ";  // Переименованный файл
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