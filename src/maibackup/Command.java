package maibackup;

public class Command {
    private final int id;
    private final String command;
    private final String description;

    public Command (int id, String command, String description) {
        this.id = id;
        this.command = command;
        this.description = description;
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

    @Override
    public String toString () {
        return command + "\t" + description;
    }
}
