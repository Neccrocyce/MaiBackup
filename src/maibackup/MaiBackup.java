package maibackup;

import logger.Group;
import logger.MaiLog;
import logger.MaiLogger;
import netscape.javascript.JSObject;

import javax.swing.*;
import java.io.*;
import java.nio.file.*;
import java.util.Arrays;

public class MaiBackup implements MaiLog {
    private static final Stats stats = new Stats();
    private static final Console console = new Console();
    private static boolean paused = false;
    private static boolean stopAtNext = false;

    /**
     *
     * @param args
     *  options - Start MaiBackup with OptionPanel
     *  verbose - Log more information
     */
    public static void main (String[] args) {
        boolean debug = false;
        boolean option = false;
        boolean continue_progress = false;

        if (args != null) {
            for (String arg : args) {
                if (arg.equals("verbose")) {
                    debug = true;
                } else if (arg.equals("options")) {
                    option = true;
                } else if (arg.equals("continue")) {
                    continue_progress = true;
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
            String progressStatus = "";
            String[] progress = new String[]{};
            String[] sources;

            console.start();
            MaiLogger.logInfo("Started MaiLogger");
            stats.incStatus();

            //Load Settings
            SettingsLoader.loadSettings();
            stats.incStatus();

            // Load Progress of last stopped backup
            String proceedStatus = "";
            String[] proceed = null;
            if (continue_progress) {
                MaiLogger.logInfo("Load progress of stopped backup to continue backup.");
                BufferedReader br = new BufferedReader(new FileReader(FixPaths.PROGRESS));
                String line;
                StringBuilder sb = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                    sb.append("\n");
                }
                br.close();
                String[] contentSplit = sb.toString().split("\n");
                proceedStatus = contentSplit[0];
                proceed = Arrays.copyOfRange(contentSplit, 1, contentSplit.length);
            }

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
            if (!continue_progress) {
                FileHandler.getInstance().rotateBackups();
            }
            stats.incStatus();

            //copy files
            if (continue_progress && proceedStatus.equals("Processing new and changed files")) {
                    sources = proceed;
            } else if (!continue_progress) {
                sources = SettingsLoader.getSrc();
            } else {
                // skip copying if progress stopped at remove files
                sources = null;
            }
            if (sources != null) {
                progressStatus = stats.getStatus();
                progress = FileHandler.getInstance().copyFiles(sources);
                stats.incStatus();
            }

            //move removed files to 01 dir
            if (continue_progress && proceedStatus.equals("Processing Removed Files")) {
                sources = proceed;
            } else if (!continue_progress) {
                sources = SettingsLoader.getSrc();
            } else {
                // skip copying if progress stopped at remove files
                sources = null;
            }
            if (sources != null && !stopAtNext) {
                progressStatus = stats.getStatus();
                progress = FileHandler.getInstance().moveDeletedFiles(sources);
                stats.incStatus();
            }

            //disconnect
            if (SettingsLoader.isUseShare()) {
                Connector.getInstance().disconnectDrive();
            }
            stats.incStatus();

            // save progress if stopAtNext
            if (stopAtNext) {
                StringBuilder contentBuilder = new StringBuilder();
                contentBuilder.append(progressStatus);
                contentBuilder.append("\n");
                for (String s: progress) {
                    contentBuilder.append(s);
                    contentBuilder.append("\n");
                }
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(FixPaths.PROGRESS));
                    writer.write(contentBuilder.toString());
                    writer.close();
                } catch (IOException e) {
                    MaiLogger.logCritical("Cannot create file for progress");
                }
            }

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

    public static Stats getStats () {
        return stats;
    }

    public static void pause () {
        paused = true;
    }

    public static void resume () {
        paused = false;
    }

    public static boolean isPaused () {
        return paused;
    }

    public static void stopAtNext () {
        stopAtNext = true;
    }

    public static boolean isStopAtNext () {
        return stopAtNext;
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
