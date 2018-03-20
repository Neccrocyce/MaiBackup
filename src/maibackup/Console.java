package maibackup;

import logger.MaiLogger;

import java.util.Scanner;

public class Console extends Thread {
    private static Console instance = null;
    private Command[] commands = new Command[] {
            new Command(0, "stop", "Stops the program. The Backup won't be complete and further runs of the program won't complete it!"),
            new Command(1, "pause", "Pauses the program. To continue use \"continue\""),
            new Command(2, "continue", "Continues the program. Alternate: type \"Enter\""),
            new Command(3, "status", "Shows the state of the program and the file on which the program is currently working on. Furthermore it shows the current statistics")
    };

    private Console () {

    }

    public static Console getInstance () {
        if (instance == null) {
            instance = new Console();
        }
        return instance;
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
                runPause();
                break;
            case 2:
                runContinue();
                break;
            case 3:
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
                MaiBackup.getInstance().evaluateStats();
                MaiBackup.getInstance().disconnectDrive();
                MaiLogger.logInfo("    ABORTED BY USER");
                MaiLogger.on_exit();
                MaiBackup.getInstance().stop();
                throw new InterruptedException();
            } else if (res.equals("no") || res.equals("n")) {
                sc.close();
                runContinue();
                break;
            } else {
                System.out.println("Type either \"yes\" or \"no\"");
            }
        }
    }

    private void runPause() {
        MaiBackup.getInstance().isPaused = true;
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            //
        }
    }

    private void runContinue() {
        MaiBackup.getInstance().isPaused = false;
    }

    private void runStatus() {
        runPause();
        System.out.println(MaiBackup.getInstance().getCurrentFile());
        MaiBackup.getInstance().evaluateStats();
        System.out.println("Type \"Enter\" to continue");
    }

    private void runUnknownCommand(String cmd) {
        if (cmd.toLowerCase().equals("help")) {
            for (Command c : commands) {
                System.out.println(c.toString());
            }
        } else if (cmd.equals("")) {
            runContinue();
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
        return new Command(-1, "unknown", "Unknown Command");
    }



}
