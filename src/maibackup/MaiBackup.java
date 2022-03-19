package maibackup;

import logger.Group;
import logger.MaiLog;
import logger.MaiLogger;

import javax.swing.*;
import java.nio.file.*;

public class MaiBackup implements MaiLog {
    private static final Stats stats = new Stats();
    private static final Console console = new Console();
    private static boolean paused = false;

    /**
     *
     * @param args
     *  options - Start MaiBackup with OptionPanel
     *  verbose - Log more information
     */
    public static void main (String[] args) {
        boolean debug = false;
        boolean option = false;

        if (args != null) {
            for (String arg : args) {
                if (arg.equals("verbose")) {
                    debug = true;
                } else if (arg.equals("options")) {
                    option = true;
                }
            }
        }
        if (option) {
            startQuestion();
        }
        try {
            MaiLogger.setUp(new MaiBackup(), -1, 5, true, debug, FixPaths.LOGS, "maibackup");
            MaiLogger.rotate();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed with Error: " + ("Unknown Error during the start of MaiBackup: " + e.getMessage()),"Abort", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        try {
            console.start();
            MaiLogger.logInfo("Started MaiLogger");
            stats.incStatus();

            //Load Settings
            SettingsLoader.loadSettings();
            stats.incStatus();

            //connect
            if (SettingsLoader.isUseShare()) {
                Connector.getInstance().connectDrive();
            }
            stats.incStatus();

            //create dest dir
            if (!Files.exists(Paths.get(SettingsLoader.getDest()))) {
                Files.createDirectories(Paths.get(SettingsLoader.getDest()));
            }

            //rotate
            FileHandler.getInstance().rotateBackups();
            stats.incStatus();

            //copy files
            FileHandler.getInstance().copyFiles();
            stats.incStatus();

            //move removed files to 01 dir
            FileHandler.getInstance().moveDeletedFiles ();
            stats.incStatus();

            //disconnect
            if (SettingsLoader.isUseShare()) {
                Connector.getInstance().disconnectDrive();
            }
            stats.incStatus();

            //evaluate stats
            if (stats.isError()) {
                MaiLogger.logError("Finished backup with error(s)");
            } else {
                MaiLogger.logInfo("Finished backup successfully");
            }
            MaiLogger.logInfo(stats.toString());
            stats.incStatus();

            //stop
            console.interrupt();
            System.out.println("Type \"Enter\" to continue");
        } catch (Exception e) {
            e.printStackTrace();
            MaiLogger.logCritical("Unknown Error: " + e.getMessage());
        }
    }

    private static void startQuestion () {
        while (true) {
            int answer = JOptionPane.showOptionDialog(null, "Start MaiBackup?", "MaiBackup", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[]{"YES", "NO", "LATER"}, "YES");
            if (answer == JOptionPane.YES_OPTION) {
                return;
            } else if (answer == JOptionPane.CANCEL_OPTION) {
                for (int i = 30; i > 0; i--) {
                    System.out.println("Wait for " + i + " minutes");
                    try {
                        Thread.sleep(60 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                System.exit(0);
            }
        }
    }

    public MaiBackup() {

    }

    public static Stats getStats() {
        return stats;
    }

    public static void pause () {
        paused = true;
    }

    public static void resume () {
        paused = false;
    }

    public static boolean isPaused() {
        return paused;
    }

    @Override
    public void stop() {
        stop(MaiLogger.getLog(Group.CRITICAL).substring(28));
    }

    /**
     * stop and exit the application if a critical error occurs or the user quits the application manually
     */
    public static void stop (String errorMessage) {
        JOptionPane.showMessageDialog(null, "Failed with Error: " + (errorMessage.isEmpty() ? "unknown Error" : errorMessage),"Abort", JOptionPane.ERROR_MESSAGE);
        console.interrupt();
        System.exit(1);
    }

    @Override
    public void sendErrMsg(String s) {
        JOptionPane.showMessageDialog(null, "Failed with Error: " + s, "Abort", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void sendLog(String s) {
        System.out.println(s);
    }
}
