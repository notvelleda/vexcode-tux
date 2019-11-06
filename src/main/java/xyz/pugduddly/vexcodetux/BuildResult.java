package xyz.pugduddly.vexcodetux;

// Simple class to contain the result of a build/upload
public class BuildResult {
    private String log;
    private int exitCode;

    public BuildResult(String log, int exitCode) {
        this.log = log;
        this.exitCode = exitCode;
    }

    public String getLog() {
        return this.log;
    }

    public int getExitCode() {
        return this.exitCode;
    }
}