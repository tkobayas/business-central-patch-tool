package com.redhat.gss.support;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PatchScriptGen {

    private static final String DIFF_FILE = "diff.txt";

    private static final String SCRIPT_FILE = "bc-patch-apply.sh";

    private static final String DIFF_TARGET = "diff-work/target/business-central.war/";
    private static final String DIFF_PATCH = "diff-work/patch/kie-drools-wb-6.5.0.Final-redhat-16-RHBRMS-3095-eap6_4-redhat/";

    private static final String APPLY_TARGET = "apply-work/target/business-central.war/";
    private static final String APPLY_PATCH = "apply-work/patch/kie-drools-wb-6.5.0.Final-redhat-16-RHBRMS-3095-eap6_4-redhat/";

    private List<String> removeList = new ArrayList<String>();
    private List<String> copyList = new ArrayList<String>();

    public static void main(String[] args) {

        PatchScriptGen patchScriptGen = new PatchScriptGen();

        patchScriptGen.parseDiff();
        patchScriptGen.writeScript();

        System.out.println("---- finished ----");
    }

    private void parseDiff() {
        Path diffFilePath = Paths.get(DIFF_FILE);

        try (BufferedReader br = Files.newBufferedReader(diffFilePath)) {
            for (String line; (line = br.readLine()) != null;) {
                if (line.startsWith("Only in ")) {
                    String[] split = line.split(" ");
                    String dir = split[2].substring(0, split[2].length() - 1);
                    String file = split[3];
                    String filePath = dir + "/" + file;
                    if (!file.endsWith(".js") && !dir.endsWith("deferredjs")) {
                        System.out.println("WARN: It's not a .js file. Confirm if it's okay to replace. Adding to list anyway -> " + line);
                    }
                    if (dir.startsWith(DIFF_TARGET)) {
                        removeList.add(filePath);
                    } else if (dir.startsWith(DIFF_PATCH)) {
                        copyList.add(filePath);
                    } else {
                        throw new RuntimeException("unexpected filePath!! -> " + line);
                    }
                    continue;
                } else if (line.endsWith(" differ")) {
                    String[] split = line.split(" ");
                    String filePath = split[3].substring(0, split[3].length());
                    if (filePath.endsWith(".class") || filePath.endsWith(".js")) {
                        copyList.add(filePath);
                    } else if (filePath.endsWith("MANIFEST.MF")) {
                        System.out.println("WARN: MANIFEST.MF will not be copied -> " + line);
                    } else {
                        System.out.println("WARN: It's not .class nor .js file. Confirm if it's okay to replace. Adding to list anyway -> " + line);
                        copyList.add(filePath);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void writeScript() {

        StringBuilder sb = new StringBuilder();
        sb.append("#!/bin/bash\n");
        sb.append("\n");

        for (String diffTargetFilePath : removeList) {
            diffTargetFilePath = diffTargetFilePath.replace("$", "\\$");

            String removeFilePath = diffTargetFilePath.replace(DIFF_TARGET, APPLY_TARGET);

            sb.append("rm -r " + removeFilePath);
            sb.append("\n");
        }

        sb.append("\n");

        for (String diffPatchFilePath : copyList) {
            diffPatchFilePath = diffPatchFilePath.replace("$", "\\$");

            String copyFromFilePath = diffPatchFilePath.replace(DIFF_PATCH, APPLY_PATCH);
            String copyToFilePath = diffPatchFilePath.replace(DIFF_PATCH, APPLY_TARGET);

            sb.append("cp -r " + copyFromFilePath + " " + copyToFilePath);
            sb.append("\n");
        }

        // HERE: Add other one-off patch jar files
        //----------------------------
//        sb.append("\n");
//        sb.append("rm " + APPLY_TARGET + "WEB-INF/lib/kie-wb-common-services-backend-6.5.0.Final-redhat-16.jar");
//        sb.append("\n");
//        sb.append("cp " + APPLY_PATCH + "WEB-INF/lib/kie-wb-common-services-backend-6.5.0.Final-redhat-16.jar "
//                        + APPLY_TARGET + "WEB-INF/lib/kie-wb-common-services-backend-6.5.0.Final-redhat-16-02029113-testpatch01.jar");
//        sb.append("\n");
        //-----------------------------

        //System.out.println(sb.toString());

        Path path = Paths.get(SCRIPT_FILE);

        try (BufferedWriter bw = Files.newBufferedWriter(path)) {
            bw.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        try {
            Runtime.getRuntime().exec("chmod u+x " + SCRIPT_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
