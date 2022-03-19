package maibackup;

public class Stats {
    /**
     * Current status of the run
     */
    private static int status = 0;
    final String[] statuses = new String[] {
        "Starting",
        "Loading settings",
        "Connecting",
        "Rotating",
        "Processing new and changed files",
        "Processing Removed Files",
        "Disconnecting",
        "Evaluating",
        "Shutting down"
    };

     /**  new:
     *  0: number of all files,
     *  1: number of new files,
     *  2: number of changed files,
     *  3: number of removed files,
     *  4: number of successful processed new files
     *  5: number of successful processed changed files
     *  6: number of successful processed removed files
     */
    private final int[] stats = new int[7];

    /**
     *  0: size of all new files
     *  1: size of all changed files
     *  2: size of all removed files
     */
    private final long[] stats_size = new long[3];

    public Stats() {

    }

    String getStatus () {
        return statuses[status];
    }

    /**
     *
     * @return true if any file was not processed successfully
     */
    public boolean isError() {
        return stats[1] != stats[4] || stats[2] != stats[5] || stats[3] != stats[6];
    }

    @Override
    public String toString() {
        int failedNewFiles = stats[1] - stats[4];
        int failedChangedFiles = stats[2] - stats[5];
        int failedRemovedFiles = stats[3] - stats[6];
        return "Result:\n" +
                "Number of files to backup: \n" +
                "\tall files: \t\t" + stats[0] + createStringSize(stats_size[0] + stats_size[1]) + "\n" +
                "\tnew files: \t\t" + stats[1] + createStringSize(stats_size[0]) + "\n" +
                "\tchanged files: \t" + stats[2] + createStringSize(stats_size[1]) +"\n" +
                "\tremoved files: \t" + stats[3] + createStringSize(stats_size[2]) +"\n" +
                "Number of files FAILED to backup: \n" +
                "\tall files: \t\t" + createStringFail(failedNewFiles + failedChangedFiles + failedRemovedFiles, stats[0]) + "\n" +
                "\tnew files: \t\t" + createStringFail(failedNewFiles, stats[1]) + "\n" +
                "\tchanged files: \t" + createStringFail(failedChangedFiles, stats[2]) + "\n" +
                "\tremoved files: \t" +  createStringFail(failedRemovedFiles, stats[3]);
    }

    private String createStringFail(int numFails, int allFiles) {
        return "" + numFails + "/" + allFiles + " (" + (allFiles == 0 ? 0 : ((numFails * 100) / allFiles)) + "%)";
    }

    private String createStringSize (long size) {
        double dsize = (double) size;
        StringBuilder s = new StringBuilder();
        s.append(" (");
        if (size < 1000) {
            s.append(size).append(" B");
        } else if (size < 1000000) {
            s.append(String.format("%.2f KiB", dsize / 1024));
        } else if (size < 1000000000) {
            s.append(String.format("%.2f MiB", dsize / 1048576));
        } else if (size < 1000000000000L) {
            s.append(String.format("%.2f GiB", dsize / 1073741824));
        } else if (size < 1000000000000000L) {
            s.append(String.format("%.2f TiB", dsize / 1099511627776L));
        }
        return s.append(")").toString();
    }

    public void incStatus () {
        status++;
    }

    public void incAllFiles () {
        stats[0]++;
    }

    public void incNewFiles () {
        stats[1]++;
    }

    public void incChangedFiles () {
        stats[2]++;
    }

    public void incRemovedFiles () {
        stats[3]++;
    }

    public void incSuccNewFiles () {
        stats[4]++;
    }

    public void incSuccChangedFiles () {
        stats[5]++;
    }

    public void incSuccRemovedFiles () {
        stats[6]++;
    }

    public void addSizeNewFiles(long size) {
       stats_size[0] += size;
    }

    public void addSizeChangedFiles(long size) {
        stats_size[1] += size;
    }

    public void addSizeRemovedFiles(long size) {
        stats_size[2] += size;
    }


}
