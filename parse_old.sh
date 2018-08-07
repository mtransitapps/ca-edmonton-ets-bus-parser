#!/bin/bash
echo ">> Parsing...";
PARSER_DIRECTORY="../parser";
PARSER_CLASSPATH=$(cat "$PARSER_DIRECTORY/classpath")
CLASS=$(cat "parser_class")
PARSER_CLASSPATH=$(cat "$PARSER_DIRECTORY/classpath")
# java -Xms4096m -Xmx16384m -Dfile.encoding=UTF-8 \
java -Xms2048m -Xmx8192m -Dfile.encoding=UTF-8 \
-classpath \
bin:\
$PARSER_CLASSPATH \
$CLASS;
RESULT=$?;
echo ">> Parsing... DONE";
exit $RESULT;