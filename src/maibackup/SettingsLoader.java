package maibackup;

import logger.MaiLogger;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * This class loads the settings configured in the settings.cfg file
 * Furthermore it creates the file if it does not exit.
 * OS-specific: Windows NT
 */
public class SettingsLoader {
    /**
     * Directory in which the files should be copied (e.g. X:\Backup)
     */
    private static String dest;

    /**
     * Directory from which the files should be copied (e.g. C:\Source1, ...)
     */
    private static List<String> src;

    /**
     * Directories from which the files should NOT be copied (should be a subdirectory of scr)
     */
    private static List<Path> ignore;

    /**
     * True if it should be connected to a share, false otherwise
     */
    private static boolean useShare;

    private SettingsLoader() {}

    public static void loadSettings () {
        int id = MaiLogger.logNewTask("Loading Settings");
        Properties p = new Properties();
        try {
            p.load(new FileReader(FixPaths.SETTINGS));
            dest = p.getProperty("destination");
            if (dest == null) {
                throw new IllegalArgumentException("destination");
            }
            String locatShare = p.getProperty("pathShare", "");
            if (locatShare.trim().length() > 0) {
                useShare = true;
                String user = p.getProperty("user", "");
                user += " " + p.getProperty("password", "");
                int timeout;
                try {
                    timeout = Integer.parseInt(p.getProperty("timeout", ""));
                } catch (NumberFormatException e) {
                    MaiLogger.logError("Illegal argument for the setting \"timeout\". Set to default value (30 sec)");
                    timeout = 30;
                }
                Connector.init(dest.charAt(0), locatShare, user, timeout);
            }

            int i = 1;
            src = new ArrayList<>();
            while (true) {
                String s = p.getProperty("source" + i);
                if (s == null) break;
                src.add(s);
                i++;
            }

            i = 1;
            ignore = new ArrayList<>();
            while (true) {
                String s = p.getProperty("ignore" + i);
                if (s == null) break;
                ignore.add(Paths.get(s));
                i++;
            }
        } catch (FileNotFoundException fe) {
            new SettingsLoader().createDefaultProperties();
            MaiLogger.failedTask(id);
            MaiLogger.logCritical("Missing settings.cfg! New one was created in \"" + (Paths.get(FixPaths.SETTINGS).toAbsolutePath()) + "\". Configure and start again!");
        } catch (IOException e) {
            MaiLogger.failedTask(id);
            MaiLogger.logCritical(e.getMessage());
        } catch (IllegalArgumentException e) {
            MaiLogger.failedTask(id);
            MaiLogger.logCritical("Missing setting \"" + e.getMessage() + "\". Reconfigure settings and start again! (\"" + (Paths.get(FixPaths.SETTINGS).toAbsolutePath()) + "\")");
        }
        MaiLogger.succeededTask(id);
    }

    private void createDefaultProperties() {
        StringBuilder comment = new StringBuilder();
        comment.append("!!MAKE SURE TO USE \"\\\\\" INSTEAD OF \"\\\"!!\n");
        comment.append("Description:\n");
        comment.append("pathShare: Path to the network share (uses smb protocol). Default: \n");
        comment.append("user: User name required to connect to share. If a login by user and password is not required leave this empty. Default: \n");
        comment.append("password: Password required to connect to share. If a login by user and password is not required leave this empty. Default: \n");
        comment.append("timeout: Time in seconds to wait while trying to connect the network share. Default: 30\n");
        comment.append("destination: Directory in which the files should be copied. Default: C:\\Backup \n");
        comment.append("sourceX: Absolute path to a directory from which the files should be copied. For the first directory use: source1; for the second: source2; .... Default: C:\\Users \n");
        comment.append("ignoreX: Absolute path to a directory which is a subdirectory of any sourceX but should not be backed up. For the first directory use: ignore1; for the second: ignore2; .... Default: \n");
        Properties p = new Properties();
        p.setProperty("pathShare", "");
        p.setProperty("user", "");
        p.setProperty("password", "");
        p.setProperty("timeout", "30");
        p.setProperty("destination", "C:\\Backup");
        p.setProperty("source1", "C:\\Users");
        try {
            p.store(new FileWriter(FixPaths.SETTINGS), comment.toString());
        } catch (IOException e) {
            MaiLogger.logCritical(e.getMessage());
        }
        MaiLogger.logInfo("Created new settings.cfg");
    }

    public static String getDest() {
        return dest;
    }

    public static List<String> getSrc() {
        return src;
    }

    public static List<Path> getIgnore() {
        return ignore;
    }

    public static boolean isUseShare() {
        return useShare;
    }
}
