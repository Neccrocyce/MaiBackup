package test;

import maibackup.FileHandler;
import maibackup.FixPaths;
import maibackup.MaiBackup;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Stream;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class TestExtraMethods {
    static String dest = "Z:\\Backup";

    static void checkNumDir (String dir, int num) {
        Path dest = Paths.get(TestExtraMethods.dest + "\\" + dir);
        try (Stream<Path> files = Files.list(dest)) {
            assertEquals(num, files.count());
        } catch (IOException e) {
            e.printStackTrace();
            fail("Exception");
        }
    }

    static void checkDirContent (String dir, String ... content) {
        Path dest = Paths.get(TestExtraMethods.dest + "\\" + dir);
        String[] files;
        try (Stream<Path> filesStream = Files.list(dest)) {
            files = filesStream.map(f -> f.toFile().getName()).toArray(String[]::new);
            Arrays.sort(files);
            Arrays.sort(content);
            assertArrayEquals(content, files);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Exception");
        }

    }

    static String readFile (String path) {
        StringBuilder content = new StringBuilder();
        try (Stream<String> str = Files.lines(new File(path).toPath())) {
            str.forEach(s -> content.append(s).append("\n"));
            if (content.length() > 0) {
                content.deleteCharAt(content.length() - 1);
            }
        } catch (IOException e) {
            e.printStackTrace();
            fail("Exception");
        }
        return content.toString();
    }

    static void writeFile (String content, String path) {
        PrintWriter out;
        try {
            out = new PrintWriter(new FileWriter(path));
            Arrays.stream(content.split("\n")).forEach(out::println);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            fail("Exception");
        }
    }

    static void setMaiBackupToNull () {
        try {
            Reflector.setStaticField(MaiBackup.class, "instance",null);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception");
        }
    }

    static void setDir (String dir) {
        FixPaths.DIR = "Z:\\Settings\\" + dir;
        FixPaths.SETTINGS = FixPaths.DIR + "\\settings.cfg";
    }

    static Object callMethodFile (String method, Object... args) {
        try {
            return Reflector.callMethod(FileHandler.getInstance(), method, args);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            fail("Exception");
        }
        return null;
    }

    static Object callMethod2File (String method, Object... args) {
        try {
            return Reflector.callMethod2(FileHandler.getInstance(), method, args);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            fail("Exception");
        }
        return null;
    }

    static void createSettingsCFG (String name, String pathShare, String devName, String user, String pw, String dst, String[] ignores, String... source) {
        Properties p = new Properties();
        p.setProperty("pathShare", pathShare);
        p.setProperty("deviceName", devName);
        p.setProperty("user", user);
        p.setProperty("password", pw);
        p.setProperty("destination", dst);
        for(int i = 1; i <= source.length; i++) {
            p.setProperty("source" + i, source[i-1]);
        }
        for(int i = 1; i <= ignores.length; i++) {
            p.setProperty("ignore" + i, ignores[i-1]);
        }
        try {
            Files.createDirectories(Paths.get("Z:\\Settings\\" + name));
            p.store(new FileWriter("Z:\\Settings\\" + name + "\\settings.cfg"),"");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void createSettingsCFG (String name, String dst, String... source) {
        createSettingsCFG(name, "","","","",dst, new String[]{}, source);
    }

    static String createExpResult (int newFiles, int changedFiles, int removedFiles, String sizeAllFiles, String sizeNewFiles, String sizeChangedFiles, String sizeRemovedFiles) {
        return "Number of files to backup: \n" +
                        "\tall files: \t\t" + (newFiles + changedFiles) + sizeAllFiles + "\n" +
                        "\tnew files: \t\t" + newFiles + sizeNewFiles + "\n" +
                        "\tchanged files: \t" + changedFiles + sizeChangedFiles + "\n" +
                        "\tremoved files: \t" + removedFiles + sizeRemovedFiles + "\n" +
                        "Number of files FAILED to backup: \n" +
                        "\tall files: \t\t" + "0/" + (newFiles + changedFiles) + " (0%)" + "\n" +
                        "\tnew files: \t\t" + "0/" + newFiles + " (0%)" + "\n" +
                        "\tchanged files: \t" + "0/" + changedFiles + " (0%)" + "\n" +
                        "\tremoved files: \t" +  "0/" + removedFiles + " (0%)" + "\n";
    }

    static String createExpResult (int newFiles, int changedFiles, int removedFiles) {
        return createExpResult (newFiles, changedFiles, removedFiles, " (0 B)", " (0 B)", " (0 B)", " (0 B)");
    }

    static void testSourceAsBefore () {
        checkNumDir("\\src", 2);
        checkDirContent("\\src","01", "02");
        checkNumDir("\\src\\01", 2);
        checkDirContent("\\src\\01","a.txt", "b.txt");
        checkNumDir("\\src\\02", 2);
        checkDirContent("\\src\\02","c.txt", "d.txt");
    }

    static void deleteDirectory (Path file) throws IOException, NullPointerException {
        Path[] files;
        try (Stream<Path> str = Files.list(file)) {
            files = str.toArray(Path[]::new);
        }
        for (Path f : files) {
            if (Files.isDirectory(f)) {
                deleteDirectory(f);
            }
            else {
                Files.delete(f);
            }
        }

        Files.delete(file);
    }
}
