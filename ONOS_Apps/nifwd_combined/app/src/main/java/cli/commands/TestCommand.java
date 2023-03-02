package src.main.java.cli.commands;

import src.main.java.cli.CommandLine;

@Command(name = "TestCommand", description = "Test function to help understand picoCLI", mixinStandardHelpOptions = true)
public class TestCommand implements Runnable {

    @Parameters(paramLabel = "<word>", defaultValue = "default", description = "Message to append to print statement")
    private String[] message = { "default" };

    public void run(){
        System.out.println("Command Test ran " + message);
    }

    public static void main(String[] args){
        int exitCode = new CommandLine(new Test()).execute(args);
        System.exit(exitCode);
    }
}
