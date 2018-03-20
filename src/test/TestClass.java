package test;

import logger.MaiLogger;
import maibackup.MaiBackup;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static test.TestExtraMethods.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestClass {

    @BeforeClass
    public static void setUp () {
        createSettingsCFG("complete1", dest + "\\dst", dest + "\\src\\01", "Z:/Backup/src/02");
    }

    @Before
    public void init () {
        try {
            new File(TestExtraMethods.dest).mkdirs();
            Reflector.callMethod2(MaiBackup.getInstance(),"deleteDirectory", Paths.get("Z:/Backup"));
            Files.createDirectory(Paths.get(dest));
            Files.createDirectory(Paths.get(dest + "\\src"));
            Files.createDirectory(Paths.get(dest + "\\src\\01"));
            Files.createFile(Paths.get(dest + "\\src\\01\\a.txt"));
            Files.createFile(Paths.get(dest + "\\src\\01\\b.txt"));
            Files.createDirectory(Paths.get(dest + "\\src\\02"));
            Files.createFile(Paths.get(dest + "\\src\\02\\c.txt"));
            Files.createFile(Paths.get(dest + "\\src\\02\\d.txt"));
        } catch (InvocationTargetException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        setMaiBackupToNull();
    }

    @After
    public void after () {
        MaiLogger.clearLog();
    }

    @Test
    public void testLocal1 () {
        String dir = "dst\\00_" + new SimpleDateFormat("yyyy.MM.dd").format(new Date());
        setDir("complete1");
        MaiBackup.main(null);
        checkNumDir("\\" + dir, 2);
        checkDirContent("\\" + dir,"01", "02");
        checkNumDir("\\" + dir + "\\01", 2);
        checkDirContent("\\" + dir + "\\01","a.txt", "b.txt");
        checkNumDir("\\" + dir + "\\02", 2);
        checkDirContent("\\" + dir + "\\02","c.txt", "d.txt");
        String expEv = "Number of files to backup: 4\n" +
                "Number of new or changed files: 4\n" +
                "Number of new files: 4\n" +
                "Number of successful copied new files: 4/4 (100%)\n" +
                "Number of changed files: 0\n" +
                "Number of successful copied changed files: 0/0 (100%)\n" +
                "Number of all successful copied files: 4/4 (100%)\n" +
                "Number of files to remove: 0\n" +
                "Number of successful removed files: 0/0 (100%)\n";
        int indexResult = MaiLogger.getLogAll().indexOf("INFO: Result:");
        assertEquals(expEv, MaiLogger.getLogAll().substring(indexResult + 14));

        testSourceAsBefore();
    }

    @Test
    public void testLocalOverrideRotate () {
        try {
            String dir = "dst";
            String date = new SimpleDateFormat("yyyy.MM.dd").format(new Date());
            setDir("complete1");
            Files.createDirectories(Paths.get(dest + "\\dst\\00_a"));
            Files.createDirectories(Paths.get(dest + "\\dst\\00_a\\01"));
            Files.createDirectories(Paths.get(dest + "\\dst\\00_a\\02"));
            Files.createFile(Paths.get(dest + "\\dst\\00_a\\01\\a.txt"));
            Files.createFile(Paths.get(dest + "\\dst\\\\00_a\\02\\d.txt"));
            Files.createDirectories(Paths.get(dest + "\\dst\\01_a"));
            Files.createFile(Paths.get(dest + "\\dst\\01_a\\e.txt"));
            Files.createDirectories(Paths.get(dest + "\\dst\\02_a"));
            Files.createFile(Paths.get(dest + "\\dst\\02_a\\f.txt"));
            Files.createDirectories(Paths.get(dest + "\\dst\\03_a"));
            Files.createFile(Paths.get(dest + "\\dst\\03_a\\g.txt"));
            Files.createDirectories(Paths.get(dest + "\\dst\\04_a"));
            Files.createFile(Paths.get(dest + "\\dst\\04_a\\h.txt"));
            Files.createDirectories(Paths.get(dest + "\\dst\\05_a"));
            Files.createFile(Paths.get(dest + "\\dst\\05_a\\i.txt"));
            Files.createDirectories(Paths.get(dest + "\\dst\\06_a"));
            Files.createFile(Paths.get(dest + "\\dst\\06_a\\j.txt"));
            Files.createDirectories(Paths.get(dest + "\\dst\\07_a"));
            Files.createFile(Paths.get(dest + "\\dst\\07_a\\k.txt"));
            MaiBackup.main(null);

            checkNumDir("\\" + dir, 6);
            //00
            checkNumDir("\\" + dir + "\\00_" + date, 2);
            checkDirContent("\\" + dir + "\\00_" + date,"01", "02");
            checkNumDir("\\" + dir + "\\00_" + date + "\\01", 2);
            checkDirContent("\\" + dir + "\\00_" + date + "\\01","a.txt", "b.txt");
            checkNumDir("\\" + dir + "\\00_" + date + "\\02", 2);
            checkDirContent("\\" + dir + "\\00_" + date + "\\02","c.txt", "d.txt");
            //01
            checkNumDir("\\" + dir + "\\01_a", 2);
            checkDirContent("\\" + dir + "\\01_a","01", "02");
            checkNumDir("\\" + dir + "\\01_a"+ "\\01", 1);
            checkDirContent("\\" + dir + "\\01_a" + "\\01","a.txt");
            checkNumDir("\\" + dir + "\\01_a" + "\\02", 1);
            checkDirContent("\\" + dir + "\\01_a" + "\\02","d.txt");
            //02
            checkNumDir("\\" + dir + "\\02_a", 1);
            checkDirContent("\\" + dir + "\\02_a","e.txt");
            //03
            checkNumDir("\\" + dir + "\\03_a", 1);
            checkDirContent("\\" + dir + "\\03_a","f.txt");
            //04
            checkNumDir("\\" + dir + "\\04_a", 1);
            checkDirContent("\\" + dir + "\\04_a","g.txt");
            //11
            checkNumDir("\\" + dir + "\\11_a", 1);
            checkDirContent("\\" + dir + "\\11_a","k.txt");
            //evaluate
            String expEv = "Number of files to backup: 4\n" +
                    "Number of new or changed files: 4\n" +
                    "Number of new files: 2\n" +
                    "Number of successful copied new files: 2/2 (100%)\n" +
                    "Number of changed files: 2\n" +
                    "Number of successful copied changed files: 2/2 (100%)\n" +
                    "Number of all successful copied files: 4/4 (100%)\n" +
                    "Number of files to remove: 0\n" +
                    "Number of successful removed files: 0/0 (100%)\n";
            int indexResult = MaiLogger.getLogAll().indexOf("INFO: Result:");
            assertEquals(expEv, MaiLogger.getLogAll().substring(indexResult + 14));

            testSourceAsBefore();

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testShare () {
        setDir("complete2");
        //test
        TestExtraMethods.dest = "X:\\Test";

        String dir = "dst\\00_" + new SimpleDateFormat("yyyy.MM.dd").format(new Date());
        MaiBackup.main(null);
        String expEv = "Number of files to backup: 4\n" +
                "Number of new or changed files: 4\n" +
                "Number of new files: 4\n" +
                "Number of successful copied new files: 4/4 (100%)\n" +
                "Number of changed files: 0\n" +
                "Number of successful copied changed files: 0/0 (100%)\n" +
                "Number of all successful copied files: 4/4 (100%)\n" +
                "Number of files to remove: 0\n" +
                "Number of successful removed files: 0/0 (100%)\n";
        int indexResult = MaiLogger.getLogAll().indexOf("INFO: Result:");
        assertEquals(expEv, MaiLogger.getLogAll().substring(indexResult + 14));
        TestExtraMethods.callMethod("connectDrive");
        try {
            checkNumDir("\\" + dir, 2);
            checkDirContent("\\" + dir, "01", "02");
            checkNumDir("\\" + dir + "\\01", 2);
            checkDirContent("\\" + dir + "\\01", "a.txt", "b.txt");
            checkNumDir("\\" + dir + "\\02", 2);
            checkDirContent("\\" + dir + "\\02", "c.txt", "d.txt");
        } catch (AssertionError e) {
            e.printStackTrace();
        } finally {
            //delete old dirs
            try {

                Reflector.callMethod2(MaiBackup.getInstance(),"deleteDirectory", Paths.get("X:/Test"));
                TestExtraMethods.callMethod("disconnectDrive");
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        TestExtraMethods.dest = "Z:\\Backup";
        testSourceAsBefore();
    }

    @Test
    public void testZCreateDefaultSettingsCFG () {
        try {
            Files.deleteIfExists(Paths.get("Z:\\Settings\\empty\\settings.cfg"));
        } catch (IOException e) {
            e.printStackTrace();
            fail("Exception");
        }
        TestExtraMethods.setDir("empty");
        MaiBackup.main(null);
    }
}
