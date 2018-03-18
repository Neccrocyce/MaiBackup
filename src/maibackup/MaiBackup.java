package maibackup;

import logger.MaiLog;
import logger.MaiLogger;

import javax.swing.*;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class MaiBackup implements MaiLog {
    private static MaiBackup instance = null;
    /**
     * last log entry
     */
    private String lastLog;
    /**
     * {0: number of files, 1: number of files to override, 2: number of overridden files, 3: number of new files, 4: number of copied new files, 5: number of failed copies of new files, 6: number of failed files to override}
      */
    private int[] statcopy = new int[7];
    /**
     * directory for settings.cfg and log files
     */
    private String dir = "%userprofile%\\Documents\\MaiBackup";
    /**
     * Letter, which the network drive will bind to (e.g. X)
     */
    private String devName;
    /**
     * Path to the network share (e.g. \\Hostname\share)
     */
    private String locatShare;
    /**
     * username + password (e.g. user:username 1234)
     */
    private String user;
    /**
     * Folder in which the files should be copied (e.g. X:\Backup)
     */
    private String dest;
    /**
     * Folder from which the files should be copied (e.g. C:\Source1, ...)
     */
    private List<String> src;

    public static void main (String[] args) {
        try {
            instance = getInstance();
            MaiLogger.setUp(instance, -1, 5, true, instance.dir + "\\logs");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed with Error: " + ("Unknown Error during the start of MaiBackup: " + e.getMessage()),"Abort", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        try {
            MaiLogger.logInfo("Started MaiLogger");
            //Load Settings
            instance.loadSettings();
            //connect
            if (!instance.devName.equals("")) {
                instance.connectDrive();
            }
            //create dest dir
            if (!Files.exists(Paths.get(instance.dest))) {
                Files.createDirectories(Paths.get(instance.dest));
            }
            //rotate
            instance.rotateBackups();
            //copy files
            instance.copyFiles();
            //move removed files to 01 dir
            instance.moveDeletedFiles ();
            //disconnect
            if (!instance.devName.equals("")) {
                instance.disconnectDrive();
            }
            //evaluate stats
            instance.evaluateStats();
        } catch (Exception e) {
            e.printStackTrace();
            MaiLogger.logCritical("Unknown Error: " + e.getMessage());
        }
    }

    public static MaiBackup getInstance () {
        if (instance == null) {
            instance = new MaiBackup();
        }
        return instance;
    }

    private MaiBackup () {

    }

    private void loadSettings () {
        int id = MaiLogger.logTask("Load Settings");
        Properties p = new Properties();
        try {
            p.load(new FileReader(dir + "\\settings.cfg"));
            devName = p.getProperty("deviceName");
            locatShare = p.getProperty("pathShare");
            user = p.getProperty("user");
            user += " " + p.getProperty("password");
            dest = p.getProperty("destination");
            int i = 1;
            src = new ArrayList<>();
            while (true) {
                String s = p.getProperty("source" + i);
                if (s == null) break;
                src.add(s);
                i++;
            }
        } catch (FileNotFoundException fe) {
            createDefaultProperties ();
            MaiLogger.failedTask(id);
            MaiLogger.logCritical("Missing settings.cfg! New one was created in \"" + (Paths.get(dir + "\\settings.cfg").toAbsolutePath()) + "\". Configure and start again!");
        } catch (IOException e) {
            MaiLogger.failedTask(id);
            MaiLogger.logCritical(e.getMessage());
        }
        MaiLogger.succeededTask(id);
    }


    /**
     * connects to share and bind it to {@code devName}
     */
    private void connectDrive () {
        int id = MaiLogger.logTask("Connect share " + locatShare + " to " + devName);
        if (devName.length() != 1) {
            MaiLogger.failedTask(id);
            MaiLogger.logCritical("Drive letter is invalid");
        }

        StringBuilder errMsg = new StringBuilder();
        try {
            Process p = Runtime.getRuntime().exec("cmd /c net use " + devName + ": " + locatShare + (user.equals("") ? " /user:" + user + " /persistent:no" : ""));
            if (!p.waitFor(20, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                MaiLogger.failedTask(id);
                MaiLogger.logCritical("Unable to connect: Timeout");
            }
            BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String s;
            while ((s = error.readLine()) != null) {
                errMsg.append(s);
            }
            p.destroy();
        } catch (IOException | InterruptedException e) {
            MaiLogger.failedTask(id);
            MaiLogger.logCritical(e.getMessage());
        }
        if (errMsg.length() == 0) {
            MaiLogger.succeededTask(id);
        } else {
            MaiLogger.failedTask(id);
            MaiLogger.logCritical("Unable to connect: " + errMsg.toString());
        }
    }

    /**
     * unbind share {@code devName}
     */
    private void disconnectDrive () {
        int id = MaiLogger.logTask("Disconnect share " + locatShare + " to " + devName);
        if (devName.length() != 1) {
            MaiLogger.failedTask(id);
            MaiLogger.logCritical("Drive letter is invalid");
        }

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
            MaiLogger.logCritical("Unable to disconnect: " + errMsg.toString());
        }
    }

    /**
     * This method rotates the backups. The names of the directories in which the backups are, start with a number
     * This method rotates "01" -> "02", ..., "06" -> "07". <br>
     * If "07" exists, then this method rotates "07" -> "11", "11" -> "12", ..., "20" -> "21" <br>
     * The following table gives an overview over these numbers:
     * <ul>
     *     <li>00: latest backup</li>
     *     <li>01: 1 week old</li>
     *     <li>...</li>
     *     <li>07: 7 weeks old</li>
     *     <li>11: 2 months old</li>
     *     <li>...</li>
     *     <li>20: 11 months old</li>
     *     <li>21: 1 year old</li>
     * </ul>
     */
    private void rotateBackups () {
        int id = MaiLogger.logTask("Rotate Backups");
        Path[] files = null;
        try (Stream<Path> str = Files.list(Paths.get(dest))) {
            files = str.toArray(Path[]::new);
        } catch (IOException e) {
            MaiLogger.failedTask(id);
            MaiLogger.logCritical("Unable to rotate Backups: Cannot open: " + (Paths.get(dest).toAbsolutePath()) + ": " + e.getMessage());
        }
        Path[] filesSorted = new Path[22];
        try {
            for (Path f : files) {
                int index = Integer.parseInt("" + f.getFileName().toString().charAt(0) + f.getFileName().toString().charAt(1));
                if (filesSorted[index] == null) {
                    filesSorted[index] = f;
                } else {
                    MaiLogger.failedTask(id);
                    MaiLogger.logCritical("Unable to rotate Backups: Folders are named incorrectly: Number " + index + " exists twice");
                }
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException | NullPointerException e) {
            MaiLogger.failedTask(id);
            MaiLogger.logCritical("Unable to rotate Backups: Folders are named incorrectly: " + e.getMessage());
        }
        try {
            if (filesSorted[7] != null) {
                //delete 21 (1 year) - if 7 exists
                if (filesSorted[21] != null) {
                    deleteDirectory(filesSorted[21]);
                }
                //rotate 11-20 (2 months - 11 months) - if 7 exists
                for (int i = 21; i > 10; i--) {
                    if (filesSorted[i-1] != null) {
                        Files.move(filesSorted[i-1],Paths.get(dest + "\\" + i + "_" + filesSorted[i - 1].getFileName().toString().substring(3)));
                    }
                }
                //move 7 - if exists - to 11 (7 weeks to 2 months)
                Files.move(filesSorted[7],Paths.get(dest + "\\11_" + filesSorted[7].getFileName().toString().substring(3)));
                filesSorted[7] = null;

                //delete 4-6 (4 weeks - 6 weeks)
                for (int i = 4; i < 7; i++) {
                    if (filesSorted[i] != null) {
                        deleteDirectory(filesSorted[i]);
                        filesSorted[i] = null;
                    }
                }
            }
            //rotate 1-7 (1 week - 7 weeks) - if exists
            for (int i = 7; i > 1; i--) {
                if (filesSorted[i - 1] != null) {
                    Files.move(filesSorted[i - 1], Paths.get(dest + "\\0" + i + "_" + filesSorted[i - 1].getFileName().toString().substring(3)));
                }
            }
            //create/rename folder 0
            String date = new SimpleDateFormat("yyyy.MM.dd").format(new Date());
            if (filesSorted[0] == null) {
                Files.createDirectories(Paths.get(dest + "\\00_" + date));
            } else {
                Files.move(filesSorted[0],Paths.get(dest + "\\00_" + date));
                //create folder 1
                Files.createDirectories(Paths.get(dest + "\\01_" + filesSorted[0].getFileName().toString().substring(3)));
            }
        } catch (IOException | NullPointerException e) {
            MaiLogger.failedTask(id);
            MaiLogger.logCritical("Unable to rotate Backups: Error exceeded during renaming or deletion of directories");
        }

        MaiLogger.succeededTask(id);
    }

    /**
     * copies all directories and files listed in {@code src}
     */
    private void copyFiles () {
        //check for duplicate directory names
        List<String> srcNames = new ArrayList<>();
        for (String s : src) {
            String fileName = Paths.get(s).getFileName().toString();
            if (srcNames.contains((fileName))) {
                MaiLogger.logCritical("Unable to copy files: Multiple Source Folders with the same name");
            } else {
                srcNames.add(fileName);
            }
        }

        //copy files
        MaiLogger.logInfo("Start Copying files");
        for (String s : src) {
            copyFile(Paths.get(s), Paths.get(s).getParent().toString());
        }
        MaiLogger.logInfo("Finished Copying files");
    }

    /**
     * Copies a file or folder to {@code dst}
     * @param p Path to the file to copy
     * @param src root directory in which all files and folders are located which are wanted to backup
     */
    private void copyFile (Path p, String src) {
        if (Files.isDirectory(p)) {
            try (Stream<Path> allDstDir = Files.list(Paths.get(dest))) {
                Path dst0Path = Paths.get(allDstDir.filter(f -> f.getFileName().toString().startsWith("00")).findFirst().get().toString() + "\\" + p.toString().substring(src.length()));
                if (!Files.exists(dst0Path)) {
                    Files.createDirectories(dst0Path);
                }
                try (Stream<Path> files  = Files.list(p)) {
                    files.forEach(f -> copyFile(f, src));
                }
            } catch (NoSuchElementException | IOException e) {
                MaiLogger.logError("Failed to copy \"" + p.toString() + "\": Unable to list directories");
                return;
            }
        } else {
            statcopy[0]++;
            String dstFile = p.toString().substring(src.length());   //file without src-prefix
            Path dst0Path = null;
            Path dst1Path = null;
            try (Stream<Path> allDstDir = Files.list(Paths.get(dest))) {//all folders/files in this.dst
                Path[] dirs = allDstDir.filter(f -> f.getFileName().toString().startsWith("00") || f.getFileName().toString().startsWith("01")).toArray(Path[]::new);
                for (Path s : dirs) {
                    if (s.getFileName().toString().startsWith("00")) {
                        dst0Path = Paths.get( s.toString() + "\\" + dstFile);
                    } else {
                        dst1Path = Paths.get( s.toString() + "\\" + dstFile);
                    }
                }
            } catch (IOException e) {
                MaiLogger.logError("Failed to copy \"" + p.toString() + "\": Unable to list directories");
                return;
            }
            try {
                if (!Files.exists(dst0Path)) {
                    try {
                        statcopy[3]++;
                        Files.copy(p, dst0Path, StandardCopyOption.COPY_ATTRIBUTES);
                        statcopy[4]++;
                        MaiLogger.logInfo("Copied: \"" + p.toString() + "\"");
                    } catch (IOException e) {
                        statcopy[5]++;
                        MaiLogger.logError("Failed to copy \"" + p.toString() + "\"" + ": " + e.getMessage());
                    }
                }
                else if (Files.size(p) != Files.size(dst0Path) || !Files.getLastModifiedTime(p).equals(Files.getLastModifiedTime(dst0Path))) {
                    try {
                        statcopy[1]++;
                        copy(p, dst0Path, dst1Path);
                        statcopy[2]++;
                        MaiLogger.logInfo("Copied: \"" + p.toString() + "\"");
                    } catch (IOException e) {
                        statcopy[6]++;
                        MaiLogger.logError("Failed to copy \"" + p.toString() + "\"");
                    }
                }
            } catch (IOException e) {
                MaiLogger.logError("Failed to copy \"" + p.toString() + "\": Unable to read files");
            } catch (NullPointerException e) {
                MaiLogger.logCritical("Failed to copy\"" + p.toString() + "\": Missing directory \"00_...\"");
            }

        }
    }

    /**
     * Copies a file represent by its path {@code dst0Path} to {@code dst1Path} and
     * copies a file represent by its path {@code srcPath} to {@code dst0Path}
     * @param srcPath
     * @param dst0Path
     * @param dst1Path
     * @throws IOException if it failed to copy files
     * @throws FileAlreadyExistsException if file represent by {@code dst1Path} exists already
     */
    private void copy (Path srcPath, Path dst0Path, Path dst1Path) throws IOException {
        //create directories in dst1Path
        Files.createDirectories(dst1Path.getParent());
        //copy files
        Files.copy(dst0Path, dst1Path, StandardCopyOption.COPY_ATTRIBUTES);
        Files.copy(srcPath, dst0Path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    }

    //checks all files of the backup, if they have been deleted from this.src
    //if so, then they will be moved to the directory 01_...
    private void moveDeletedFiles () {
        MaiLogger.logInfo("Start Moving Deleted files to directory 01");
        for (String s : src) {
            moveFile(Paths.get(s), Paths.get(s).getParent().toString());
        }
        MaiLogger.logInfo("Finished Moving Deleted files");
    }

    /**
     * checks if all files in backup (dir 00) are also in scrPath.
     * If not so, then the missing files will be moved to dir 01.
     * @param srcPath Path to the src Directory of which all files should be checked
     * @param srcRoot src root directory in which all files and folders are located which are wanted to backup
     */
    private void moveFile (Path srcPath, String srcRoot) {
        //check if srcPath is a directory
        if (!Files.isDirectory(srcPath)) {
            return;
        }
        //define dst0Path (dst Path in 00 dir), dst1Path (dst Path in 01 dir)
        String dstFile = srcPath.toString().substring(srcRoot.length());   //dir without srcRoot-prefix
        Path dst0Path = null;
        Path dst1Path = null;
        try (Stream<Path> allDstDir = Files.list(Paths.get(dest))) {//all folders/files in this.dst
            Path[] dirs = allDstDir.filter(f -> f.getFileName().toString().startsWith("00") || f.getFileName().toString().startsWith("01")).toArray(Path[]::new);
            for (Path s : dirs) {
                if (s.getFileName().toString().startsWith("00")) {
                    dst0Path = Paths.get( s.toString() + "\\" + dstFile);
                } else {
                    dst1Path = Paths.get( s.toString() + "\\" + dstFile);
                }
            }
        } catch (IOException e) {
            MaiLogger.logError("Failed to check if directory \"" + srcPath.toString() + "\" should be removed from Backup. Unable to list directories");
            return;
        }

        //list all dirs/files of dst
        Path[] dst0PathChildren = null;
        try (Stream<Path> dst0Dirs = Files.list(dst0Path)) {
            dst0PathChildren = dst0Dirs.toArray(Path[]::new);
        } catch (IOException e) {
            MaiLogger.logError("Failed to check if directory \"" + srcPath.toString() + "\" should be removed from Backup. Unable to list directories");
            return;
        }

        //check if all files/dirs in dst0Path exist in srcPath
        for (Path p : dst0PathChildren) {
            Path srcFile = Paths.get(srcPath.toString() + "\\" + p.getFileName().toString());
            if (!Files.exists(srcFile)) {
                //move to 01
                try {
                    if (!Files.exists(dst1Path)) {
                        Files.createDirectories(dst1Path);
                    }
                    Files.move(p, Paths.get(dst1Path.toString() + "\\" + p.getFileName().toString()));
                    MaiLogger.logInfo("Moved " + p.toString() + " -> " + (dst1Path.toString() + "\\" + p.getFileName().toString()));
                } catch (IOException e) {
                    MaiLogger.logError("Failed to remove \"" + srcFile.toString() + "\" from Backup. Unable to move directory");
                    return;
                }
            } else {
                //check p
                moveFile(srcFile, srcRoot);
            }
        }


    }

    private void deleteDirectory (Path file) throws IOException, NullPointerException {
        Path[] files;
        try (Stream<Path> str = Files.list(file)) {
            files = str.toArray(Path[]::new);
        }
        for (Path f : files) {
            if (Files.isDirectory(f)) {
                deleteDirectory(f);
            }
            else {
                Files.delete(f);
            }
        }

        Files.delete(file);
    }

    private void evaluateStats () {
        int filesToCopy = statcopy[1] + statcopy[3];
        int allFilesCopied = statcopy[2] + statcopy[4];
        MaiLogger.logInfo("Result:\n" +
                "Number of files to backup: " + statcopy[0] + "\n" +
                "Number of new or changed files: " + filesToCopy + "\n" +
                "Number of new files: " + statcopy[3] + "\n" +
                "Number of successful copied new files: " + statcopy[4] + "/" + statcopy[3] + " (" + (statcopy[3] == 0 ? 100 : ((statcopy[4] * 100) / statcopy[3])) + "%)\n" +
                "Number of changed files: " + statcopy[1] + "\n" +
                "Number of successful copied changed files: " + statcopy[2] + "/" + statcopy[1] + " (" + (statcopy[1] == 0 ? 100 : ((statcopy[2] * 100) / statcopy[1])) + "%)\n" +
                "Number of all successful copied files: " + allFilesCopied + "/" + filesToCopy + " (" + (filesToCopy == 0 ? 100 : ((allFilesCopied * 100) / filesToCopy)) + "%)");
    }

    private void createDefaultProperties() {
        StringBuilder comment = new StringBuilder();
        comment.append("Description:\n");
        comment.append("pathShare: Path to the network share. Be sure to use \"\\\\\" instead of \"\\\"Default: \n");
        comment.append("deviceName: Letter, which the network drive will bind to. To use local drive, leave this empty. Default: X\n");
        comment.append("user: Required User name to connect to share. If a login by user and password is not required leave this empty. Default: \n");
        comment.append("password: Required password to connect to share. If a login by user and password is not required leave this empty. Default: \n");
        comment.append("destination: Folder in which the files should be copied. Be sure to use \"\\\\\" instead of \"\\\"Default: C:\\Backup \n");
        comment.append("sourceX: Folder from which the files should be copied. For the first folder use: source1; for the second: source2; .... Be sure to use \"\\\\\" instead of \"\\\"Default: C:\\Users \n");
        Properties p = new Properties();
        p.setProperty("pathShare", "");
        p.setProperty("deviceName", "X");
        p.setProperty("user", "");
        p.setProperty("password", "");
        p.setProperty("destination", "C:\\Backup");
        p.setProperty("source1", "C:\\Users");
        try {
            p.store(new FileWriter(dir + "\\settings.cfg"), comment.toString());
        } catch (IOException e) {
            MaiLogger.logCritical(e.getMessage());
        }
        MaiLogger.logInfo("Created new settings.cfg");
    }

    @Override
    public void stop() {
        JOptionPane.showMessageDialog(null, "Failed with Error: " + (lastLog != null ? lastLog : "unknown Error"),"Abort", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    @Override
    public void sendErrMsg(String s) {
        JOptionPane.showMessageDialog(null, "Failed with Error: " + s, "Abort", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void sendLog(String s) {
        try {
            lastLog = s.substring(28);
        } catch (StringIndexOutOfBoundsException e) {
            MaiLogger.logError("Log entries have wrong syntax");
        }
        System.out.println(s);
    }
}
