package test;

import logger.MaiLogger;
import maibackup.Console;
import maibackup.MaiBackup;
import maibackup.Stats;
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

public class Debugger {
    @Test
    public void print_stats () {
        Stats stats = new Stats();
        System.out.println(stats.toString());
    }

    @Test
    public void runConsole () {
        new Console().start();
        Boolean paused = false;
        try {
            while (true) {
                paused = (Boolean) (Reflector.getStaticField(MaiBackup.class, "paused"));
                if (paused) {
                    Thread.sleep(100);
                } else {
                    System.out.println("This is a test.");
                    Thread.sleep(1000);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception");
        }
    }
}
