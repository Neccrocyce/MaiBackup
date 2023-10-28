package maibackup;

import logger.MaiLogger;
import java.util.Scanner;

public class Console extends Thread {
    private final int cStop = 0;
    private final int cPause = 1;
    private final int cResume = 2;
    private final int cStatus = 3;
    private final int cP = 4;
    private final int cS = 5;
    private final int cStopSave = 6;
    private final Command[] commands = new Command[] {
            new Command(cStop, "stopnow", "Stops the program immediately. The Backup won't be completed and further runs of the program won't complete it!", false),
            new Command(cPause, "pause", "Pauses the program. To continue use \"resume\"", false),
            new Command(cResume, "resume", "Resumes the program. Alternate: type \"Enter\"", false),
            new Command(cStatus, "status", "Shows the state of the program and the file on which the program is currently working on. Furthermore it shows the current statistics", false),
            new Command(cP, "p", "pause", true),
            new Command(cS, "s", "status", true),
            new Command(cStopSave, "stop", "Stops the program and saves the progress at the next possibility. To continue backup, start the program with the argument \"continue\"", false)
    };

    Scanner scanner;

    public Console() {

    }

    @Override
    public void run () {
        scanner = new Scanner(System.in);
        while (!Thread.interrupted()) {
            try {
                runCommand(scanner.nextLine());
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }
        scanner.close();
    }

    private void runCommand (String command) throws InterruptedException {
        switch (findCommand(command).getId()) {
            case cStop -> runStop();
            case cPause, cP -> runPause(true);
            case cResume -> runResume();
            case cStatus, cS -> runStatus();
            case cStopSave -> runStopSave();
            default -> runUnknownCommand(command);
        }
    }

    private void runStop() throws InterruptedException {
        runPause(false);
        System.out.println("Are you sure to stop the application. The backup won't be complete and further runs of the program won't complete it! (yes/no)");
        while (true) {
            String res = scanner.nextLine();
            if (res.equals("yes") || res.equals("y")) {
                MaiLogger.logInfo(MaiBackup.getStats().toString());
                Connector.getInstance().disconnectDrive();
                MaiLogger.logInfo("    ABORTED BY USER");
                MaiLogger.on_exit();
                MaiBackup.stop("ABORTED BY USER");
                throw new InterruptedException();
            } else if (res.equals("no") || res.equals("n")) {
                runResume();
                break;
            } else {
                System.out.println("Type either \"yes\" or \"no\"");
            }
        }
    }

    private void runStopSave() {
        runPause(false);
        System.out.println("Are you sure to stop the application at the next possibility. The progress will be saved. The backup can be completed by starting it with the parameter \"continue\" (yes/no)");
        while (true) {
            String res = scanner.nextLine();
            if (res.equals("yes") || res.equals("y")) {
                MaiLogger.logInfo("Stop the program at the next possibility.");
                MaiBackup.stopAtNext();
                runResume();
                break;
            } else if (res.equals("no") || res.equals("n")) {
                runResume();
                break;
            } else {
                System.out.println("Type either \"yes\" or \"no\"");
            }
        }
    }

    private void runPause(boolean continueMessage) {
        MaiBackup.pause();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            //
        }
        if (continueMessage) {
            System.out.println("Type \"Enter\" to continue");
        }
    }

    private void runResume() {
        MaiBackup.resume();
    }

    private void runStatus() {
        runPause(false);
        System.out.println("Status: " + MaiBackup.getStats().getStatus());
        MaiLogger.logInfo(MaiBackup.getStats().toString());
        System.out.println("Type \"Enter\" to continue");
    }

    private void runUnknownCommand(String cmd) {
        if (cmd.equalsIgnoreCase("help")) {
            runPause(false);
            for (Command c : commands) {
                if (!c.isHiddenCommand()) {
                    System.out.println(c);
                }
            }
            System.out.println("Type \"Enter\" to continue");
        } else if (cmd.equals("")) {
            runResume();
        } else {
            System.out.println("Unknown Command: \"" + cmd + "\"");
        }
    }

    private Command findCommand (String name) {
        for (Command cmd : commands) {
            if (cmd.getCommand().equals(name)) {
                return cmd;
            }
        }
        return new Command(-1, "unknown", "Unknown Command", true);
    }


    public static class Command {
        private final int id;
        private final String command;
        private final String description;
        private final boolean hiddenCommand;

        public Command (int id, String command, String description, boolean hiddenCommand) {
            this.id = id;
            this.command = command;
            this.description = description;
            this.hiddenCommand = hiddenCommand;
        }

        public String getCommand() {
            return command;
        }

        public String getDescription() {
            return description;
        }

        public int getId() {
            return id;
        }

        public boolean isHiddenCommand() {
            return hiddenCommand;
        }

        @Override
        public String toString () {
            return command + "\t" + description;
        }
    }
}
