package maibackup;

public class FixPaths {
    /**
     * Separator which is used between to directories
     * Windows: "\\", Linux: "/"
     */
    final static String SEP = "\\";

    /**
     * directory for settings.cfg and log files
     */
    public static String DIR = System.getProperty("user.home") + SEP + "Documents" + SEP + "MaiBackup";

    public static String SETTINGS = DIR + SEP + "settings.cfg";
    final static String LOGS = DIR + SEP + "logs";
    public static String PROGRESS = DIR + SEP + "progress.txt";

}
