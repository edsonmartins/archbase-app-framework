package br.com.archbase.maven.plugin.codegen.util;

import org.apache.maven.plugin.MojoExecutionException;


public class ArchbaseDataMojoException extends MojoExecutionException {

    public ArchbaseDataMojoException() {
        super("");
        ArchbaseDataLogger.printGeneratedTables(true);
    }

    public ArchbaseDataMojoException(String message) {
        super(message);
        ArchbaseDataLogger.printGeneratedTables(true);
    }

    public ArchbaseDataMojoException(String message, Throwable cause) {
        super(message, cause);
        ArchbaseDataLogger.printGeneratedTables(true);
    }

}
