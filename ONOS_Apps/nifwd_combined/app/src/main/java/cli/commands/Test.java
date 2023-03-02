import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "Test", description = "Test function to help understand picoCLI", mixinStandardHelpOptions = true)
public class Test implements Runnable {

    @Parameters(paramLabel = "<word>", defaultValue = "default", description = "Message to append to print statement")
    private String[] message = { "default" };

    public void run(){
        System.out.println("Command Test ran" + message);
    }

    public static void main(String[] args){
        int exitCode = new CommandLine(new Test()).execute(args);
        System.exit(exitCode);
    }
}
