package maibackup;

import logger.MaiLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * This class implements the methods to connect/disconnect a share
 * OS-specific: Windows NT
 */
public class Connector {
    private static Connector instance = null;

    /**
     * Letter, which the network drive will bind to (e.g. X)
     */
    private char devName;

    /**
     * Path to the network share (e.g. \\Hostname\share)
     */
    private String locatShare;

    /**
     * username + password (e.g. user:username 1234)
     */
    private String user;

    /**
     * Time to wait for connection to share (in seconds)
     */
    private int timeout;

    public static Connector getInstance () {
        if (instance == null) {
            instance = new Connector();
        }
        return instance;
    }

    public static void init (char devName, String locatShare, String user, int timeout) {
        getInstance();
        instance.devName = devName;
        instance.locatShare = locatShare;
        instance.user = user;
        instance.timeout = timeout;
    }

    private Connector () {

    }

    /**
     * connects to share and bind it to {@code devName}
     */
    public void connectDrive () {
        MaiLogger.logInfo("Trying to connect share " + locatShare + " to " + devName);

        StringBuilder errMsg = new StringBuilder();
        try {
            Process p = Runtime.getRuntime().exec("cmd /c net use " + devName + ": " + locatShare + (user.trim().equals("") ? "" : " /user:" + user) + " /persistent:no");
            if (!p.waitFor(timeout, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                MaiLogger.logCritical("Unable to connect: Timeout");
            }
            BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String s;
            while ((s = error.readLine()) != null) {
                errMsg.append(s);
            }
            p.destroy();
        } catch (IOException | InterruptedException e) {
            MaiLogger.logCritical("Unable to connect: " + e.getMessage());
        }
        if (errMsg.length() == 0) {
            MaiLogger.logInfo("Connected to share " + locatShare);
        } else {
            MaiLogger.logCritical("Unable to connect: " + errMsg);
        }
    }

    /**
     * unbind share {@code devName}
     */
    public void disconnectDrive () {
        int id = MaiLogger.logNewTask("Disconnecting share " + locatShare + " from " + devName);

        StringBuilder errMsg = new StringBuilder();
        try {
            Process p = Runtime.getRuntime().exec("net use " + devName + ": /delete");
            BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String s;
            while ((s = error.readLine()) != null) {
                errMsg.append(s);
            }
            p.destroy();
        } catch (IOException e) {
            MaiLogger.failedTask(id);
            MaiLogger.logCritical(e.getMessage());
        }
        if (errMsg.length() == 0) {
            MaiLogger.succeededTask(id);
        } else {
            MaiLogger.failedTask(id);
            MaiLogger.logCritical("Unable to disconnect: " + errMsg);
        }
    }
}
