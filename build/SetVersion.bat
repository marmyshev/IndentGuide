rem setting the version in pom.xml and MANIFEST.MF files

cd ..
mvn org.eclipse.tycho:tycho-versions-plugin:set-version -Dtycho.mode=maven -DnewVersion=2.2.4
