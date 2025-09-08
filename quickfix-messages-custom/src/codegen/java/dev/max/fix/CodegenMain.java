package dev.max.fix;

import org.quickfixj.codegenerator.MessageCodeGenerator;
import org.quickfixj.codegenerator.MessageCodeGenerator.Task;

import java.io.File;

public class CodegenMain {
    // args[0] = basePackage, args[1] = pathToDictionary, args[2] = outDir
    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: CodegenMain <basePackage> <dict.xml> <outDir>");
            System.exit(1);
        }
        String basePackage = args[0];
        File dict = new File(args[1]);
        File outDir = new File(args[2]);

        Task task = new Task();
        task.setName("Custom FIX 4.4");
        task.setSpecification(dict);
        task.setOutputBaseDirectory(outDir);
        task.setMessagePackage(basePackage + ".messages");
        task.setFieldPackage(basePackage + ".fields");

        // полезные флаги (при необходимости — снимите/измените):
        task.setDecimalGenerated(true);
        task.setOrderedFields(true);
        task.setUtcTimestampPrecision("NANOS");

        new MessageCodeGenerator().generate(task);
    }
}
