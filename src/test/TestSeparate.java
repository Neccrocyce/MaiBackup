package test;

import logger.MaiLogger;
import maibackup.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static test.TestExtraMethods.*;

/*
This class uses the path Z: to create all required files
 */
public class TestSeparate {

    @BeforeClass
    public static void setup () {
        MaiLogger.setUp(new MaiBackup(), -1, -1, true, false, "Z:/logs", "maibackup");
        createSettingsCFG("copy", dest + "\\dst", dest + "\\src");
        createSettingsCFG("copyIgnore", "", "", "", "", dest + "\\dst", new String[] {dest + "\\src\\01\\b.txt", dest + "\\src\\02"}, dest + "\\src");
        createSettingsCFG("move", dest + "\\dst", dest + "\\src\\01", dest + "\\src\\02");
        createSettingsCFG("rotate", dest + "\\rotate");
        try {
            Files.createDirectories(Paths.get("Z:\\Settings\\empty"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Before
    public void init () {
        try {
            new File(TestExtraMethods.dest).mkdirs();
            TestExtraMethods.deleteDirectory(Paths.get("Z:/Backup"));
            Files.createDirectory(Paths.get(dest));
            Files.createDirectory(Paths.get(dest + "\\src"));
            Files.createDirectory(Paths.get(dest + "\\src\\01"));
            Files.createFile(Paths.get(dest + "\\src\\01\\a.txt"));
            Files.createFile(Paths.get(dest + "\\src\\01\\b.txt"));
            Files.createDirectory(Paths.get(dest + "\\src\\02"));
            Files.createFile(Paths.get(dest + "\\src\\02\\c.txt"));
            Files.createFile(Paths.get(dest + "\\src\\02\\d.txt"));
            Files.createDirectory(Paths.get(dest + "\\dst"));
            Files.createDirectory(Paths.get(dest + "\\dst\\01"));
            Files.createFile(Paths.get(dest + "\\dst\\01\\e.txt"));
            Files.createFile(Paths.get(dest + "\\dst\\01\\f.txt"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        setMaiBackupToNull();

    }

    @Test
    public void testCopy () {
        writeFile("SourceA","Z:/Backup/src/01/a.txt");
        writeFile("SourceB","Z:/Backup/dst/01/e.txt");
        callMethod2File("copy", Paths.get("Z:/Backup/src/01/a.txt"), Paths.get("Z:/Backup/dst/01/e.txt"), Paths.get("Z:/Backup/dst/02/g.txt"));
        assert(new File("Z:/Backup/src/01/a.txt").exists());
        assert(new File("Z:/Backup/dst/01/e.txt").exists());
        assert(new File("Z:/Backup/dst/02/g.txt").exists());
        assertEquals("SourceA",readFile("Z:/Backup/src/01/a.txt"));
        assertEquals("SourceA",readFile("Z:/Backup/dst/01/e.txt"));
        assertEquals("SourceB",readFile("Z:/Backup/dst/02/g.txt"));
    }

    @Test
    public void testCopyFile () {
        String dir = "dst\\00_a";
        setDir("copy");
        SettingsLoader.loadSettings();
        try {
            Files.createDirectory(Paths.get(dest +  "\\" + dir));
        } catch (IOException e) {
            e.printStackTrace();
            fail("Exception");
        }
        callMethod2File("copyFile",Paths.get(dest + "\\src"), dest, dest + "\\" + dir, "", new ArrayList<Path>());
        checkNumDir("\\" + dir, 1);
        checkDirContent("\\" + dir ,"src");
        checkNumDir("\\" + dir + "\\src", 2);
        checkDirContent("\\" + dir + "\\src","01", "02");
        checkNumDir("\\" + dir + "\\src\\01", 2);
        checkDirContent("\\" + dir + "\\src\\01","a.txt", "b.txt");
        checkNumDir("\\" + dir + "\\src\\02", 2);
        checkDirContent("\\" + dir + "\\src\\02","c.txt", "d.txt");

        //test if source wasn't changed
        testSourceAsBefore();
    }

    @Test
    public void testCopyFileIgnore () {
        String dir = "dst\\00_a";
        setDir("copyIgnore");
        SettingsLoader.loadSettings();
        try {
            Files.createDirectory(Paths.get(dest +  "\\" + dir));
        } catch (IOException e) {
            e.printStackTrace();
            fail("Exception");
        }
        callMethod2File("copyFile",Paths.get(dest + "\\src"), dest, dest + "\\" + dir, "", SettingsLoader.getIgnore());
        checkNumDir("\\" + dir, 1);
        checkDirContent("\\" + dir ,"src");
        checkNumDir("\\" + dir + "\\src", 1);
        checkDirContent("\\" + dir + "\\src","01");
        checkNumDir("\\" + dir + "\\src\\01", 1);
        checkDirContent("\\" + dir + "\\src\\01","a.txt");

        //test if source wasn't changed
        testSourceAsBefore();
    }

    @Test
    public void testCopyFiles () {
        String dir = "dst\\00_a";
        setDir("copy");
        SettingsLoader.loadSettings();
        try {
            Files.createDirectory(Paths.get(dest +  "\\" + dir));
        } catch (IOException e) {
            e.printStackTrace();
            fail("Exception");
        }
        FileHandler.getInstance().copyFiles(SettingsLoader.getSrc());
        checkNumDir("\\" + dir, 1);
        checkDirContent("\\" + dir ,"src");
        checkNumDir("\\" + dir + "\\src", 2);
        checkDirContent("\\" + dir + "\\src","01", "02");
        checkNumDir("\\" + dir + "\\src\\01", 2);
        checkDirContent("\\" + dir + "\\src\\01","a.txt", "b.txt");
        checkNumDir("\\" + dir + "\\src\\02", 2);
        checkDirContent("\\" + dir + "\\src\\02","c.txt", "d.txt");

        testSourceAsBefore();
    }

    @Test
    public void testRotate () {
        try {
            String date = new SimpleDateFormat("yyyy.MM.dd").format(new Date());
            String dir = "rotate";
            String path = dest + "\\" + dir;
            Files.createDirectory(Paths.get(path));
            setDir(dir);
            SettingsLoader.loadSettings();
            //
            checkNumDir(dir,0);
            //00
            FileHandler.getInstance().rotateBackups();
            checkNumDir(dir,1);
            checkDirContent(dir + "\\00_" + date);
            Files.createFile(Paths.get(path + "\\00_" + date + "\\a1.txt"));
            Files.createFile(Paths.get(path + "\\00_" + date + "\\a2.txt"));
            //01
            FileHandler.getInstance().rotateBackups();
            checkNumDir(dir, 2);
            checkDirContent(dir + "\\00_" + date,"a1.txt", "a2.txt");
            checkDirContent(dir + "\\01_" + date);
            Files.createFile(Paths.get(path + "\\01_" + date + "\\b1.txt"));
            Files.createFile(Paths.get(path + "\\01_" + date + "\\b2.txt"));
            //02
            FileHandler.getInstance().rotateBackups();
            checkNumDir(dir, 3);
            checkDirContent(dir + "\\00_" + date,"a1.txt", "a2.txt");
            checkDirContent(dir + "\\01_" + date);
            checkDirContent(dir + "\\02_" + date,"b1.txt", "b2.txt");
            Files.createFile(Paths.get(path + "\\01_" + date + "\\c1.txt"));
            Files.createFile(Paths.get(path + "\\01_" + date + "\\c2.txt"));
            //03
            FileHandler.getInstance().rotateBackups();
            checkNumDir(dir, 4);
            checkDirContent(dir + "\\00_" + date,"a1.txt", "a2.txt");
            checkDirContent(dir + "\\01_" + date);
            checkDirContent(dir + "\\02_" + date,"c1.txt", "c2.txt");
            checkDirContent(dir + "\\03_" + date,"b1.txt", "b2.txt");
            Files.createFile(Paths.get(path + "\\01_" + date + "\\d1.txt"));
            Files.createFile(Paths.get(path + "\\01_" + date + "\\d2.txt"));
            //04
            FileHandler.getInstance().rotateBackups();
            checkNumDir(dir, 5);
            checkDirContent(dir + "\\00_" + date,"a1.txt", "a2.txt");
            checkDirContent(dir + "\\01_" + date);
            checkDirContent(dir + "\\02_" + date,"d1.txt", "d2.txt");
            checkDirContent(dir + "\\03_" + date,"c1.txt", "c2.txt");
            checkDirContent(dir + "\\04_" + date,"b1.txt", "b2.txt");
            Files.createFile(Paths.get(path + "\\01_" + date + "\\e1.txt"));
            Files.createFile(Paths.get(path + "\\01_" + date + "\\e2.txt"));
            //05
            FileHandler.getInstance().rotateBackups();
            Files.createFile(Paths.get(path + "\\01_" + date + "\\f1.txt"));
            Files.createFile(Paths.get(path + "\\01_" + date + "\\f2.txt"));
            //06
            FileHandler.getInstance().rotateBackups();
            Files.createFile(Paths.get(path + "\\01_" + date + "\\g1.txt"));
            Files.createFile(Paths.get(path + "\\01_" + date + "\\g2.txt"));
            //07
            FileHandler.getInstance().rotateBackups();
            checkNumDir(dir, 8);
            checkDirContent(dir + "\\00_" + date,"a1.txt", "a2.txt");
            checkDirContent(dir + "\\01_" + date);
            checkDirContent(dir + "\\02_" + date,"g1.txt", "g2.txt");
            checkDirContent(dir + "\\03_" + date,"f1.txt", "f2.txt");
            checkDirContent(dir + "\\04_" + date,"e1.txt", "e2.txt");
            checkDirContent(dir + "\\05_" + date,"d1.txt", "d2.txt");
            checkDirContent(dir + "\\06_" + date,"c1.txt", "c2.txt");
            checkDirContent(dir + "\\07_" + date,"b1.txt", "b2.txt");
            Files.createFile(Paths.get(path + "\\01_" + date + "\\h1.txt"));
            Files.createFile(Paths.get(path + "\\01_" + date + "\\h2.txt"));
            //11
            FileHandler.getInstance().rotateBackups();
            checkNumDir(dir, 6);
            checkDirContent(dir + "\\00_" + date,"a1.txt", "a2.txt");
            checkDirContent(dir + "\\01_" + date);
            checkDirContent(dir + "\\02_" + date,"h1.txt", "h2.txt");
            checkDirContent(dir + "\\03_" + date,"g1.txt", "g2.txt");
            checkDirContent(dir + "\\04_" + date,"f1.txt", "f2.txt");
            checkDirContent(dir + "\\11_" + date,"b1.txt", "b2.txt");
            Files.createFile(Paths.get(path + "\\01_" + date + "\\i1.txt"));
            Files.createFile(Paths.get(path + "\\01_" + date + "\\i2.txt"));
            //11.05
            FileHandler.getInstance().rotateBackups();
            Files.createFile(Paths.get(path + "\\01_" + date + "\\c1.txt"));
            Files.createFile(Paths.get(path + "\\01_" + date + "\\c2.txt"));
            //11.06
            FileHandler.getInstance().rotateBackups();
            Files.createFile(Paths.get(path + "\\01_" + date + "\\d1.txt"));
            Files.createFile(Paths.get(path + "\\01_" + date + "\\d2.txt"));
            //11.07
            FileHandler.getInstance().rotateBackups();
            Files.createFile(Paths.get(path + "\\01_" + date + "\\e1.txt"));
            Files.createFile(Paths.get(path + "\\01_" + date + "\\e2.txt"));
            //12
            FileHandler.getInstance().rotateBackups();
            checkNumDir(dir, 7);
            checkDirContent(dir + "\\00_" + date,"a1.txt", "a2.txt");
            checkDirContent(dir + "\\01_" + date);
            checkDirContent(dir + "\\02_" + date,"e1.txt", "e2.txt");
            checkDirContent(dir + "\\03_" + date,"d1.txt", "d2.txt");
            checkDirContent(dir + "\\04_" + date,"c1.txt", "c2.txt");
            checkDirContent(dir + "\\11_" + date,"f1.txt", "f2.txt");
            checkDirContent(dir + "\\12_" + date,"b1.txt", "b2.txt");
            Files.createFile(Paths.get(path + "\\01_" + date + "\\g1.txt"));
            Files.createFile(Paths.get(path + "\\01_" + date + "\\g2.txt"));
            //create 13-21
            Files.createDirectory(Paths.get(path + "\\13_" + date));
            Files.createFile(Paths.get(path + "\\13_" + date + "\\h1.txt"));
            Files.createFile(Paths.get(path + "\\13_" + date + "\\h2.txt"));
            Files.createDirectory(Paths.get(path + "\\14_" + date));
            Files.createFile(Paths.get(path + "\\14_" + date + "\\i1.txt"));
            Files.createFile(Paths.get(path + "\\14_" + date + "\\i2.txt"));
            Files.createDirectory(Paths.get(path + "\\15_" + date));
            Files.createFile(Paths.get(path + "\\15_" + date + "\\j1.txt"));
            Files.createFile(Paths.get(path + "\\15_" + date + "\\j2.txt"));
            Files.createDirectory(Paths.get(path + "\\16_" + date));
            Files.createFile(Paths.get(path + "\\16_" + date + "\\k1.txt"));
            Files.createFile(Paths.get(path + "\\16_" + date + "\\k2.txt"));
            Files.createDirectory(Paths.get(path + "\\17_" + date));
            Files.createFile(Paths.get(path + "\\17_" + date + "\\l1.txt"));
            Files.createFile(Paths.get(path + "\\17_" + date + "\\l2.txt"));
            Files.createDirectory(Paths.get(path + "\\18_" + date));
            Files.createFile(Paths.get(path + "\\18_" + date + "\\m1.txt"));
            Files.createFile(Paths.get(path + "\\18_" + date + "\\m2.txt"));
            Files.createDirectory(Paths.get(path + "\\19_" + date));
            Files.createFile(Paths.get(path + "\\19_" + date + "\\n1.txt"));
            Files.createFile(Paths.get(path + "\\19_" + date + "\\n2.txt"));
            Files.createDirectory(Paths.get(path + "\\20_" + date));
            Files.createFile(Paths.get(path + "\\20_" + date + "\\o1.txt"));
            Files.createFile(Paths.get(path + "\\20_" + date + "\\o2.txt"));
            Files.createDirectory(Paths.get(path + "\\21_" + date));
            Files.createFile(Paths.get(path + "\\21_" + date + "\\p1.txt"));
            Files.createFile(Paths.get(path + "\\21_" + date + "\\p2.txt"));
            //22
            FileHandler.getInstance().rotateBackups();
            checkNumDir(dir, 17);
            checkDirContent(dir + "\\00_" + date,"a1.txt", "a2.txt");
            checkDirContent(dir + "\\01_" + date);
            checkDirContent(dir + "\\02_" + date,"g1.txt", "g2.txt");
            checkDirContent(dir + "\\03_" + date,"e1.txt", "e2.txt");
            checkDirContent(dir + "\\04_" + date,"d1.txt", "d2.txt");
            checkDirContent(dir + "\\05_" + date,"c1.txt", "c2.txt");
            checkDirContent(dir + "\\11_" + date,"f1.txt", "f2.txt");
            checkDirContent(dir + "\\12_" + date,"b1.txt", "b2.txt");
            checkDirContent(dir + "\\13_" + date,"h1.txt", "h2.txt");
            checkDirContent(dir + "\\14_" + date,"i1.txt", "i2.txt");
            checkDirContent(dir + "\\15_" + date,"j1.txt", "j2.txt");
            checkDirContent(dir + "\\16_" + date,"k1.txt", "k2.txt");
            checkDirContent(dir + "\\17_" + date,"l1.txt", "l2.txt");
            checkDirContent(dir + "\\18_" + date,"m1.txt", "m2.txt");
            checkDirContent(dir + "\\19_" + date,"n1.txt", "n2.txt");
            checkDirContent(dir + "\\20_" + date,"o1.txt", "o2.txt");
            checkDirContent(dir + "\\21_" + date,"p1.txt", "p2.txt");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    //Robustness Test
    public void testStats () {
        Stats stats = new Stats();
        stats.incSuccRemovedFiles();
        stats.incRemovedFiles();
        stats.incSuccChangedFiles();
        stats.incChangedFiles();
        stats.incSuccNewFiles();
        stats.incNewFiles();
        stats.incAllFiles();
        stats.incStatus();
        stats.toString();
    }

    /*
    Create a new file /connect/settings.cfg with the specific information to connect to a share, i.e.,
    destination, pathShare, [user, password]
     */
    @Test
    public void testConnectDisconnect () {
        setDir("connect");
        try {
            if (!(new File("Z:\\Settings\\connect")).exists()) {
                fail("No file \"connect\\settings.cfg\". Create this file for this test by yourself");
            }
            SettingsLoader.loadSettings();
            Connector.getInstance().connectDrive();
            assert(new File ("X:").exists());
            Thread.sleep(2000);
            Connector.getInstance().disconnectDrive();
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testMoveFile () {
        String date = new SimpleDateFormat("yyyy.MM.dd").format(new Date());
        setDir("move");
        String dir0 = "dst\\00_" + date;
        String dir1 = "dst\\01_" + date;
        SettingsLoader.loadSettings();
        try {
            Files.createDirectories(Paths.get(dest + "\\" + dir0 + "\\01"));
            Files.createDirectories(Paths.get(dest + "\\" + dir1));
            Files.createFile(Paths.get(dest + "\\" + dir0 + "\\01\\a.txt"));
            Files.createFile(Paths.get(dest + "\\" + dir0 + "\\01\\b.txt"));
            Files.createFile(Paths.get(dest + "\\" + dir0 + "\\01\\c.txt"));
            Files.createFile(Paths.get(dest + "\\" + dir0 + "\\01\\d.txt"));
        } catch (IOException e) {
            e.printStackTrace();
            fail("Exception");
        }
        callMethod2File("moveFile", Paths.get(dest + "\\src\\01"), dest + "\\src", dest + "\\" + dir0, dest + "\\" + dir1);
        checkNumDir(dir0 + "\\01", 2);
        checkDirContent(dir0 + "\\01", "a.txt", "b.txt");
        checkNumDir(dir1 + "\\01", 2);
        checkDirContent(dir1 + "\\01", "c.txt", "d.txt");

        testSourceAsBefore();
    }

    @Test
    public void testMoveDeletedFiles () {
        String date = new SimpleDateFormat("yyyy.MM.dd").format(new Date());
        setDir("move");
        String dir0 = "dst\\00_" + date;
        String dir1 = "dst\\01_" + date;
        SettingsLoader.loadSettings();
        try {
            Files.createDirectories(Paths.get(dest + "\\" + dir0 + "\\01"));
            Files.createDirectories(Paths.get(dest + "\\" + dir0 + "\\02"));
            Files.createDirectories(Paths.get(dest + "\\" + dir1));
            Files.createFile(Paths.get(dest + "\\" + dir0 + "\\01\\a.txt"));
            Files.createFile(Paths.get(dest + "\\" + dir0 + "\\01\\b.txt"));
            Files.createFile(Paths.get(dest + "\\" + dir0 + "\\01\\c.txt"));
            Files.createFile(Paths.get(dest + "\\" + dir0 + "\\01\\d.txt"));
            Files.createFile(Paths.get(dest + "\\" + dir0 + "\\02\\a.txt"));
            Files.createFile(Paths.get(dest + "\\" + dir0 + "\\02\\b.txt"));
            Files.createFile(Paths.get(dest + "\\" + dir0 + "\\02\\c.txt"));
            Files.createFile(Paths.get(dest + "\\" + dir0 + "\\02\\d.txt"));
        } catch (IOException e) {
            e.printStackTrace();
            fail("Exception");
        }
        FileHandler.getInstance().moveDeletedFiles(SettingsLoader.getSrc());
        checkNumDir(dir0 + "\\01", 2);
        checkDirContent(dir0 + "\\01", "a.txt", "b.txt");
        checkNumDir(dir0 + "\\02", 2);
        checkDirContent(dir0 + "\\02", "c.txt", "d.txt");
        checkNumDir(dir1 + "\\01", 2);
        checkDirContent(dir1 + "\\01", "c.txt", "d.txt");
        checkNumDir(dir1 + "\\02", 2);
        checkDirContent(dir1 + "\\02", "a.txt", "b.txt");

        testSourceAsBefore();
    }


    @Test
    public void testOverride () {
        String date = new SimpleDateFormat("yyyy.MM.dd").format(new Date());
        setDir("copy");
        String dir0 = "dst\\00_" + date;
        String dir1 = "dst\\01_" + date;
        SettingsLoader.loadSettings();
        try {
            Files.createDirectories(Paths.get(dest + "\\" + dir0 + "\\src\\01"));
            Files.createDirectories(Paths.get(dest + "\\" + dir1));
            writeFile("SourceA",dest + "\\" + dir0 + "\\src\\01\\a.txt");
            writeFile("SourceB",dest + "\\" + dir0 + "\\src\\01\\b.txt");
        } catch (IOException e) {
            e.printStackTrace();
            fail("Exception");
        }
        FileHandler.getInstance().copyFiles(SettingsLoader.getSrc());
        checkNumDir("\\" + dir0, 1);
        checkDirContent("\\" + dir0 ,"src");
        checkNumDir("\\" + dir0 + "\\src", 2);
        checkDirContent("\\" + dir0 + "\\src","01", "02");
        checkNumDir("\\" + dir0 + "\\src\\01", 2);
        checkDirContent("\\" + dir0 + "\\src\\01","a.txt", "b.txt");
        checkNumDir("\\" + dir0 + "\\src\\02", 2);
        checkDirContent("\\" + dir0 + "\\src\\02","c.txt", "d.txt");

        checkNumDir("\\" + dir1, 1);
        checkDirContent("\\" + dir1 ,"src");
        checkNumDir("\\" + dir1 + "\\src", 1);
        checkDirContent("\\" + dir1 + "\\src","01");
        checkNumDir("\\" + dir1 + "\\src\\01", 2);
        checkDirContent("\\" + dir1 + "\\src\\01","a.txt", "b.txt");

        assertEquals("",readFile(dest + "\\" + dir0 + "\\src\\01\\a.txt"));
        assertEquals("",readFile(dest + "\\" + dir0 + "\\src\\01\\b.txt"));
        assertEquals("SourceA",readFile(dest + "\\" + dir1 + "\\src\\01\\a.txt"));
        assertEquals("SourceB",readFile(dest + "\\" + dir1 + "\\src\\01\\b.txt"));

        testSourceAsBefore();
    }


}
