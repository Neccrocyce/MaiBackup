package test;

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
        String[] files = null;
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
        try {
            Reflector.setField(MaiBackup.getInstance(), "dir", "Z:\\Settings\\" + dir);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception");
        }
    }

    static Object callMethod (String method, Object... args) {
        try {
            return Reflector.callMethod(MaiBackup.getInstance(), method, args);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            fail("Exception");
        }
        return null;
    }

    static Object callMethod2 (String method, Object... args) {
        try {
            return Reflector.callMethod2(MaiBackup.getInstance(), method, args);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            fail("Exception");
        }
        return null;
    }

    static void createSettingsCFG (String name, String pathShare, String devName, String user, String pw, String dst, String... source) {
        Properties p = new Properties();
        p.setProperty("pathShare", pathShare);
        p.setProperty("deviceName", devName);
        p.setProperty("user", user);
        p.setProperty("password", pw);
        p.setProperty("destination", dst);
        for(int i = 1; i <= source.length; i++) {
            p.setProperty("source" + i, source[i-1]);
        }
        try {
            Files.createDirectories(Paths.get("Z:\\Settings\\" + name));
            p.store(new FileWriter("Z:\\Settings\\" + name + "\\settings.cfg"),"");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void createSettingsCFG (String name, String dst, String... source) {
        createSettingsCFG(name, "","","","",dst,source);
    }

    static void testSourceAsBefore () {
        checkNumDir("\\src", 2);
        checkDirContent("\\src","01", "02");
        checkNumDir("\\src\\01", 2);
        checkDirContent("\\src\\01","a.txt", "b.txt");
        checkNumDir("\\src\\02", 2);
        checkDirContent("\\src\\02","c.txt", "d.txt");
    }
}
