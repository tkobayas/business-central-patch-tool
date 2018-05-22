## How to create a business-central patch when GWT client changes involved

1. Create 'diff-work' directory. Create 'target' and 'patch' directories under it.

2. Copy business-central.war (e.g. BRMS 6.4.6) under 'diff-work/target' and kie-wb-distributions/kie-drools-wb/kie-drools-wb-distribution-wars/target/kie-drools-wb-XXXXX under 'diff-work/patch'.

for example, it  would be like this:

~~~
diff-work/target/business-central.war/
diff-work/patch/kie-drools-wb-6.5.0.Final-redhat-16-RHBRMS-3095-eap6_4-redhat/
~~~

# These directories will be read-only

3. Check diff

Note, we'd like to focus on changes other than jar files, so ignore "jar differ" at the moment.

$ diff -qr diff-work/target/business-central.war/ diff-work/patch/kie-drools-wb-6.5.0.Final-redhat-16-RHBRMS-3095-eap6_4-redhat/ | grep -v "jar differ" > diff.txt

You can classify the output into 4 types:

A) "Only in" .js files

~~~
Only in diff-work/target/business-central.war/org.kie.workbench.drools.KIEDroolsWebapp: 0061613D1959A08EFD1883479E445A2D.cache.js
Only in diff-work/target/business-central.war/org.kie.workbench.drools.KIEDroolsWebapp: 02E09437F341438442FC087838416FA6.cache.js
Only in diff-work/patch/kie-drools-wb-6.5.0.Final-redhat-16-eap6_4-redhat/org.kie.workbench.drools.KIEDroolsWebapp: 240F8A2285B4451B39516C8C4D66941C.cache.js
...
~~~

 -> Remove from target business-central.war. Copy from patch war

B) "Only in" other than .js

 -> I don't see it in my case. If you find, you may need a closer look. Probably, remove from target business-central.war and copy from patch war.

C) "differ" .class files or .js files

~~~
Files diff-work/target/business-central.war/org.kie.workbench.drools.KIEDroolsWebapp/org.kie.workbench.drools.KIEDroolsWebapp.nocache.js and diff-work/patch/kie-drools-wb-6.5.0.Final-redhat-16-eap6_4-redhat/org.kie.workbench.drools.KIEDroolsWebapp/org.kie.workbench.drools.KIEDroolsWebapp.nocache.js differ
Files diff-work/target/business-central.war/WEB-INF/classes/org/jboss/errai/ServerMarshallingFactoryImpl.class and diff-work/patch/kie-drools-wb-6.5.0.Final-redhat-16-eap6_4-redhat/WEB-INF/classes/org/jboss/errai/ServerMarshallingFactoryImpl.class differ
Files diff-work/target/business-central.war/WEB-INF/classes/org/jboss/errai/ServerMarshallingFactoryImpl$M3jkkSA.class and diff-work/patch/kie-drools-wb-6.5.0.Final-redhat-16-eap6_4-redhat/WEB-INF/classes/org/jboss/errai/ServerMarshallingFactoryImpl$M3jkkSA.class differ
Files diff-work/target/business-central.war/WEB-INF/classes/org/jboss/errai/ServerMarshallingFactoryImpl$McwvvqB.class and diff-work/patch/kie-drools-wb-6.5.0.Final-redhat-16-eap6_4-redhat/WEB-INF/classes/org/jboss/errai/ServerMarshallingFactoryImpl$McwvvqB.class differ
...
~~~

 -> Copy over from patch war 

D) "differ" other than .class / .js files

~~~
Files diff-work/target/business-central.war/META-INF/MANIFEST.MF and diff-work/patch/kie-drools-wb-6.5.0.Final-redhat-16-eap6_4-redhat/META-INF/MANIFEST.MF differ
Files diff-work/target/business-central.war/org.kie.workbench.drools.KIEDroolsWebapp/compilation-mappings.txt and diff-work/patch/kie-drools-wb-6.5.0.Final-redhat-16-eap6_4-redhat/org.kie.workbench.drools.KIEDroolsWebapp/compilation-mappings.txt differ
~~~

 -> Take a closer look. In my case,
   -> Don't copy MANIFEST.MF
   -> Copy compilation-mappings.txt over from patch war
   -> If you included other one-off patch (e.g. backend service jar), it will show up here. Basically it's good to remove old jar and copy new jar.

4. Generate patch script

PatchScriptGen.java will generate a script file to copy/remove files following step 3.

If you have patches which were not handled in D), edit PatchScriptGen.java after the comment "// HERE: Add other one-off patch jar files"

Just run PatchScriptGen in Eclipse.

Check bc-patch-apply.sh generated

5. Create 'apply-work' directory

6. Copy 'diff-work/target' and 'diff-work/patch' to 'apply-work'.

~~~
apply-work/target/business-central.war/
apply-work/patch/kie-drools-wb-6.5.0.Final-redhat-16-RHBRMS-3095-eap6_4-redhat/
~~~

# Only apply-work/target/business-central.war/ will be modified with the next step

7. Run bc-patch-apply.sh

Confirm the updated (= patched) business-central.war has only a few intended differences against kie-drools-wb-6.5.0.Final-redhat-16-eap6_4-redhat

~~~
$ diff -qr apply-work/patch/kie-drools-wb-6.5.0.Final-redhat-16-RHBRMS-3095-eap6_4-redhat/ apply-work/target/business-central.war/ | grep -v "jar differ"
Files apply-work/patch/kie-drools-wb-6.5.0.Final-redhat-16-RHBRMS-3095-eap6_4-redhat/META-INF/MANIFEST.MF and apply-work/target/business-central.war/META-INF/MANIFEST.MF differ
~~~

Well done!
