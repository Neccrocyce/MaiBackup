package maibackup;

import logger.MaiLogger;

import java.io.IOException;
import java.util.Scanner;

public class Console extends Thread {
    private static Console instance = null;
    private Command[] commands = new Command[] {
            new Command(0, "stop", "Stops the program. The Backup won't be complete and further runs of the program won't complete it!", false),
            new Command(1, "pause", "Pauses the program. To continue use \"resume\"", false),
            new Command(2, "resume", "Resumes the program. Alternate: type \"Enter\"", false),
            new Command(3, "status", "Shows the state of the program and the file on which the program is currently working on. Furthermore it shows the current statistics", false),
            new Command(4, "p", "pause", true),
            new Command(5, "s", "status", true)
    };

    public Console() {

    }

    @Override
    public void run () {
        Scanner in = new Scanner(System.in);
        while (!Thread.interrupted()) {
            try {
                runCommand(in.nextLine());
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }
        in.close();
    }

    private void runCommand (String command) throws InterruptedException {
        switch (findCommand(command).getId()) {
            case 0:
                runStop();
                break;
            case 1:
            case 4:
                runPause();
                break;
            case 2:
                runResume();
                break;
            case 3:
            case 5:
                runStatus();
                break;
            default:
                runUnknownCommand(command);
                break;
        }
    }

    private void runStop() throws InterruptedException {
        runPause();
        System.out.println("Are you sure to stop the application. The backup won't be complete and further runs of the program won't complete it! (yes/no)");
        Scanner sc = new Scanner(System.in);
        while (true) {
            String res = sc.nextLine();
            if (res.equals("yes") || res.equals("y")) {
                sc.close();
                MaiLogger.logInfo(MaiBackup.getStats().toString());
                Connector.getInstance().disconnectDrive();
                MaiLogger.logInfo("    ABORTED BY USER");
                MaiLogger.on_exit();
                MaiBackup.stop("ABORTED BY USER");
                throw new InterruptedException();
            } else if (res.equals("no") || res.equals("n")) {
                sc.close();
                runResume();
                break;
            } else {
                System.out.println("Type either \"yes\" or \"no\"");
            }
        }
    }

    private void runPause() {
        MaiBackup.pause();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            //
        }
    }

    private void runResume() {
        MaiBackup.resume();
    }

    private void runStatus() {
        runPause();
        System.out.println("Status: " + MaiBackup.getStats().getStatus());
        MaiLogger.logInfo(MaiBackup.getStats().toString());
        System.out.println("Type \"Enter\" to continue");
    }

    private void runUnknownCommand(String cmd) {
        if (cmd.toLowerCase().equals("help")) {
            runPause();
            for (Command c : commands) {
                if (!c.isHiddenCommand()) {
                    System.out.println(c.toString());
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
