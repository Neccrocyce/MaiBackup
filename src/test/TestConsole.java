package test;

import maibackup.Console;
import maibackup.MaiBackup;
import test.Reflector;

public class TestConsole {
    public static void main (String[] args) {
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
        }
    }
}
