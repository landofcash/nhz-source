javac -sourcepath src/java/ -classpath conf/:classes/:lib/* -d . ./src/java/nhz/*.java 

jar cf nhz.jar nhz

echo "nhz.jar generated successfully"
