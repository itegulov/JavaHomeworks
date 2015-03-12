#!/bin/bash

rm -Rf doc
mkdir doc

src_files=/tmp/src_files.txt

find src/ru/ifmo/ctddev/itegulov/implementor -type f -not -path "*src/ru/ifmo/ctddev/itegulov/implementor/example*" -not -path "*src/ru/ifmo/ctddev/itegulov/implementor/test*" -name '*.java' > "$src_files"
javadoc -private -d doc -sourcepath "/usr/lib/jvm/java-8-oracle/src.zip!/:/usr/lib/jvm/java-8-oracle/javafx-src.zip!/:src" -classpath /home/itegulov/Downloads/junit-4.11.jar:/home/itegulov/Downloads/hamcrest-core-1.3.jar:/home/itegulov/Downloads/ImplementorTest.jar "@$src_files"

rm -f "$src_files"
