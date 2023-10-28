package maibackup;

import logger.MaiLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

public class FileHandler {
    private static FileHandler instance = null;

    public static FileHandler getInstance () {
        if (instance == null) {
            instance = new FileHandler();
        }
        return instance;
    }

    private FileHandler() {

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
    public void rotateBackups () {
        MaiLogger.logInfo("STEP 1 / 3: Rotating Backups");
        Path[] files = null;
        try (Stream<Path> str = Files.list(Paths.get(SettingsLoader.getDest()))) {
            files = str.toArray(Path[]::new);
        } catch (IOException e) {
            MaiLogger.logCritical("Unable to rotate Backups: Cannot open: " + (Paths.get(SettingsLoader.getDest()).toAbsolutePath()) + ": " + e.getMessage());
        }
        Path[] filesSorted = new Path[22];
        try {
            for (Path f : files) {
                int index = Integer.parseInt("" + f.getFileName().toString().charAt(0) + f.getFileName().toString().charAt(1));
                if (filesSorted[index] == null) {
                    filesSorted[index] = f;
                } else {
                    MaiLogger.logCritical("Unable to rotate Backups: Directories are named incorrectly: Number " + index + " exists twice");
                }
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException | NullPointerException e) {
            MaiLogger.logCritical("Unable to rotate Backups: Directories are named incorrectly: " + e.getMessage());
        }
        try {
            if (filesSorted[7] != null) {
                //delete 21 (1 year) - if 7 exists
                int taskDel21 = MaiLogger.logNewTask("Deleting directory 21");
                if (filesSorted[21] != null) {
                    deleteDirectory(filesSorted[21]);
                    MaiLogger.succeededTask(taskDel21);
                }
                //rotate 11-20 (2 months - 11 months) - if 7 exists
                for (int i = 21; i > 10; i--) {
                    int taskRot;
                    if (filesSorted[i-1] != null) {
                        taskRot = MaiLogger.logNewTask("Rotating directories " + (i-1) + " to " + i);
                        Files.move(filesSorted[i-1],Paths.get(SettingsLoader.getDest() + FixPaths.SEP + i + "_" + filesSorted[i - 1].getFileName().toString().substring(3)));
                        MaiLogger.succeededTask(taskRot);
                    }
                }
                //move 7 - if exists - to 11 (7 weeks to 2 months)
                int taskRot = MaiLogger.logNewTask("Rotating directories 7 to 11");
                Files.move(filesSorted[7],Paths.get(SettingsLoader.getDest() + FixPaths.SEP + "11_" + filesSorted[7].getFileName().toString().substring(3)));
                filesSorted[7] = null;
                MaiLogger.succeededTask(taskRot);

                //delete 4-6 (4 weeks - 6 weeks)
                for (int i = 4; i < 7; i++) {
                    if (filesSorted[i] != null) {
                        taskRot = MaiLogger.logNewTask("Deleting directory " + i);
                        deleteDirectory(filesSorted[i]);
                        filesSorted[i] = null;
                        MaiLogger.succeededTask(taskRot);
                    }
                }
            }
            //rotate 1-7 (1 week - 7 weeks) - if exists
            for (int i = 7; i > 1; i--) {
                if (filesSorted[i - 1] != null) {
                    int taskRot = MaiLogger.logNewTask("Rotating directory " + (i-1) + " to " + i);
                    Files.move(filesSorted[i - 1], Paths.get(SettingsLoader.getDest() + FixPaths.SEP + "0" + i + "_" + filesSorted[i - 1].getFileName().toString().substring(3)));
                    MaiLogger.succeededTask(taskRot);
                }
            }
            //create/rename folder 0
            String date = new SimpleDateFormat("yyyy.MM.dd").format(new Date());
            if (filesSorted[0] == null) {
                int taskCreate = MaiLogger.logNewTask("Creating directory 0");
                Files.createDirectories(Paths.get(SettingsLoader.getDest() + FixPaths.SEP + "00_" + date));
                MaiLogger.succeededTask(taskCreate);
            } else {
                int taskRot = MaiLogger.logNewTask("Rotating directory 0 to 1");
                Files.move(filesSorted[0],Paths.get(SettingsLoader.getDest() + FixPaths.SEP+ "00_" + date));
                MaiLogger.succeededTask(taskRot);
                //create folder 1
                taskRot = MaiLogger.logNewTask("Creating directory 0");
                Files.createDirectories(Paths.get(SettingsLoader.getDest() + FixPaths.SEP + "01_" + filesSorted[0].getFileName().toString().substring(3)));
                MaiLogger.succeededTask(taskRot);
            }
        } catch (IOException | NullPointerException e) {
            MaiLogger.logCritical("Unable to rotate Backups: Error exceeded during renaming or deletion of directories");
        }

        MaiLogger.logInfo("Finished step 1 (Rotating Backups)");
    }

    /**
     * copies all directories and files listed in {@code src}
     */
    public String[] copyFiles(String[] sources) {
        //check for duplicate directory names
        List<String> srcNames = new ArrayList<>();
        for (String s : SettingsLoader.getSrc()) {
            String fileName = Paths.get(s).getFileName().toString();
            if (srcNames.contains((fileName))) {
                MaiLogger.logCritical("Unable to copy files: Multiple Source directories with the same name");
            } else {
                srcNames.add(fileName);
            }
        }

        //Find 00 and 01 directory
        String dst0Path = "";
        String dst1Path = "";
        try (Stream<Path> allDstDir = Files.list(Paths.get(SettingsLoader.getDest()))) {//all folders/files in this.dst
            Path[] dirs = allDstDir.filter(f -> f.getFileName().toString().startsWith("00") || f.getFileName().toString().startsWith("01")).toArray(Path[]::new);
            for (Path s : dirs) {
                if (s.getFileName().toString().startsWith("00")) {
                    dst0Path = s.toString();
                } else {
                    dst1Path = s.toString();
                }
            }
            if (dst0Path.equals("")) {
                MaiLogger.logCritical("Failed to copy files: Destination directories are named incorrectly");
            }
        } catch (IOException e) {
            MaiLogger.logCritical("Failed to copy files: Unable to list destination directories");
        }

        //copy files
        MaiLogger.logInfo("STEP 2 / 3: Processing changed/new files");
        for (int i = 0; i < sources.length; i++) {
            // check if stop program
            if (MaiBackup.isStopAtNext()) {
                return Arrays.copyOfRange(sources, i, sources.length);
            }

            MaiLogger.logInfo(sources[i] + ": (" + (i+1) + "/" + sources.length + ")");
            copyFile(Paths.get(sources[i]), Paths.get(sources[i]).getParent().toString(), dst0Path, dst1Path, SettingsLoader.getIgnore());
        }
        MaiLogger.logInfo("Finished step 2 / 3 (Processing changed/new files)");

        return new String[]{};
    }

    /**
     * Copies a file or directory to {@code dst}
     * @param p Path to the file to copy
     * @param src root directory in which all files and folders are located which are wanted to be backed up
     */
    private void copyFile (Path p, String src, String dst0, String dst1, List<Path> ignores) {
        while(MaiBackup.isPaused()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {

            }
        }
        // check if folder should be ignored and remove non-subdirectories from list
        List<Path> ignores_sub = new ArrayList<>();
        for (Path folder : ignores) {
            if (folder.equals(p)) {
                MaiLogger.logInfo("Ignored \"" + p + "\"");
                return;
            }
            else if (folder.startsWith(p)) {
                ignores_sub.add(folder);
            }
        }

        // copy files/folders
        if (Files.isDirectory(p)) {
            MaiLogger.logInfo("Checking for new/changed files in: " + p);
            try {
                Path dst0Path = Paths.get(dst0 + FixPaths.SEP + p.toString().substring(src.length()));
                if (!dst0Path.toFile().exists()) {
                    Files.createDirectories(dst0Path);
                }
                try (Stream<Path> files  = Files.list(p)) {
                    files.forEach(f -> copyFile(f, src,dst0,dst1, ignores_sub));
                }
            } catch (NoSuchElementException | IOException e) {
                MaiLogger.logError("Failed to copy \"" + p + "\": Unable to list directories");
            }
        } else {
            MaiBackup.getStats().incAllFiles();
            String dstFile = p.toString().substring(src.length());   //file without src-prefix
            if (!dstFile.startsWith(FixPaths.SEP)) {
                dstFile = FixPaths.SEP + dstFile;
            }
            Path dst0Path = Paths.get( dst0 + dstFile);
            Path dst1Path = Paths.get( dst1 + dstFile);
            try {
                if (!dst0Path.toFile().exists()) {
                    MaiBackup.getStats().incNewFiles();
                    MaiBackup.getStats().addSizeNewFiles(Files.size(p));
                    try {
                        Files.copy(p, dst0Path, StandardCopyOption.COPY_ATTRIBUTES);
                        MaiBackup.getStats().incSuccNewFiles();
                        MaiLogger.logInfo("Copied: \"" + p + "\"");
                    } catch (IOException e) {
                        MaiLogger.logError("Failed to copy \"" + p + "\"" + ": " + e.getMessage());
                    }
                }
                else if (Files.size(p) != Files.size(dst0Path) || !Files.getLastModifiedTime(p).equals(Files.getLastModifiedTime(dst0Path))) {
                    MaiBackup.getStats().incChangedFiles();
                    MaiBackup.getStats().addSizeChangedFiles(Files.size(p));
                    try {
                        copy(p, dst0Path, dst1Path);
                        MaiBackup.getStats().incSuccChangedFiles();
                        MaiLogger.logInfo("Copied: \"" + p + "\"");
                    } catch (IOException e) {
                        MaiLogger.logError("Failed to copy \"" + p + "\"");
                    }
                }
            } catch (IOException e) {
                MaiLogger.logError("Failed to copy \"" + p + "\": Unable to read files");
            } catch (NullPointerException e) {
                MaiLogger.logCritical("Failed to copy\"" + p + "\": Missing directory \"00_...\"");
            }

        }
    }

    /**
     * Copies a file represent by its path {@code dst0Path} to {@code dst1Path} and
     * copies a file represent by its path {@code srcPath} to {@code dst0Path}
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
    public String[] moveDeletedFiles(String[] sources) {
        //Find 00 and 01 directory
        String dst0Path = "";
        String dst1Path = "";
        try (Stream<Path> allDstDir = Files.list(Paths.get(SettingsLoader.getDest()))) {//all folders/files in this.dst
            Path[] dirs = allDstDir.filter(f -> f.getFileName().toString().startsWith("00") || f.getFileName().toString().startsWith("01")).toArray(Path[]::new);
            for (Path s : dirs) {
                if (s.getFileName().toString().startsWith("00")) {
                    dst0Path = s.toString();
                } else {
                    dst1Path = s.toString();
                }
            }
            if (dst0Path.equals("")) {
                MaiLogger.logCritical("Failed to copy files: Destination directories are named incorrectly");
            }
        } catch (IOException e) {
            MaiLogger.logCritical("Failed to copy files: Unable to list destination directories");
        }
        //move
        MaiLogger.logInfo("STEP 3 / 3: Processing removed files");
        for (int i = 0; i < sources.length; i++) {
            // check if stop program
            if (MaiBackup.isStopAtNext()) {
                return Arrays.copyOfRange(sources, i, sources.length);
            }
            moveFile(Paths.get(sources[i]), Paths.get(sources[i]).getParent().toString(),dst0Path,dst1Path);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        MaiLogger.logInfo("Finished step 3 / 3 (Processing removed files)");
        return new String[]{};
    }

    /**
     * checks if all files in backup (dir 00) are also in scrPath.
     * If not so, then the missing files will be moved to dir 01.
     * @param srcPath Path to the src Directory of which all files should be checked
     * @param srcRoot src root directory in which all files and directories are located which are wanted to backed up
     */
    private void moveFile (Path srcPath, String srcRoot, String dst0, String dst1) {
        while(MaiBackup.isPaused()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {

            }
        }
        //check if srcPath is a directory
        if (!Files.isDirectory(srcPath)) {
            return;
        }
        MaiLogger.logInfo("Checking for removed files in: " + srcPath);
        //define dst0Path (dst Path in 00 dir), dst1Path (dst Path in 01 dir)
        String dstFile = srcPath.toString().substring(srcRoot.length());   //dir without srcRoot-prefix
        if (!dstFile.startsWith(FixPaths.SEP)) {
            dstFile = FixPaths.SEP + dstFile;
        }
        Path dst0Path = Paths.get( dst0 + dstFile);
        Path dst1Path = Paths.get( dst1 + dstFile);

        //list all dirs/files of dst
        Path[] dst0PathChildren;
        try (Stream<Path> dst0Dirs = Files.list(dst0Path)) {
            dst0PathChildren = dst0Dirs.toArray(Path[]::new);
        } catch (IOException e) {
            MaiLogger.logError("Failed to check if directory \"" + srcPath + "\" should be removed from Backup. Unable to list directories");
            return;
        }

        //check if all files/dirs in dst0Path exist in srcPath
        for (Path p : dst0PathChildren) {
            String srcFile = srcPath + FixPaths.SEP + p.getFileName().toString();
            if (!new File(srcFile).exists()) {
                //move to 01
                try {
                    MaiBackup.getStats().incRemovedFiles();
                    MaiBackup.getStats().addSizeRemovedFiles(Files.size(p));
                    if (!dst1Path.toFile().exists()) {
                        Files.createDirectories(dst1Path);
                    }
                    Files.move(p, Paths.get(dst1Path + FixPaths.SEP + p.getFileName().toString()));
                    MaiBackup.getStats().incSuccRemovedFiles();
                    MaiLogger.logInfo("Moved " + p + " -> " + (dst1Path + FixPaths.SEP + p.getFileName().toString()));
                } catch (IOException e) {
                    MaiLogger.logError("Failed to remove \"" + srcFile + "\" from Backup. Unable to move directory");
                    return;
                }
            } else {
                //check p
                moveFile(Paths.get(srcFile), srcRoot, dst0, dst1);
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
}
