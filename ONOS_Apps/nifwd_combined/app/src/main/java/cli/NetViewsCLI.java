import CommandLine;

public class NetViewsCLI {
    public static void main(String[] args){
        TestCommand app = new TestCommand();
        CommandLine cmd = new CommandLine(app);

        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int exitCode = cmd.execute("Test123");
        assertEquals(0, exitCode);
        assertEquals("Command Test ran Test123", sw.toString());   
    }
}
