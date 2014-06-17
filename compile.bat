"C:\Program Files\Java\jdk1.7.0_45\bin\javac" -sourcepath src/java/ -classpath conf/;classes/;lib/* -d . src/java/nhz/*.java 

"C:\Program Files\Java\jdk1.7.0_45\bin\jar.exe" cf nhz.jar nhz

echo "nhz.jar generated successfully"
pause
