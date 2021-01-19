package com.worth;

public class WorthMain {
    private static final int servicePort = 6660;
    private static final int registryPort = 6661;

    public static void main(String[] args) {

        Cli cli = new Cli(servicePort, registryPort);
        cli.runCli();
    }
}
