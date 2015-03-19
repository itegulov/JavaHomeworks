#!/bin/bash

rm -rf doc
mkdir doc

src_files=/tmp/implementor_src_files.txt
api="http://docs.oracle.com/javase/8/docs/api/"

find src/ru/ifmo/ctddev/itegulov/iterativeparallelism -not -path "*Test*" -type f -name '*.java' > "$src_files"
find src/info/kgeorgiy/java/advanced/concurrent/* -type f -name '*.java' >> "$src_files"
javadoc -linkoffline "$api" "$api" -d doc -sourcepath "/usr/lib/jvm/java-8-oracle/src.zip!/:/usr/lib/jvm/java-8-oracle/javafx-src.zip!/:/home/itegulov/Documents/Programming/Java/Java4sem/java-advanced-2015/java/" -classpath /home/itegulov/Downloads/junit-4.11.jar:/home/itegulov/Downloads/hamcrest-core-1.3.jar:/home/itegulov/Downloads/IterativeParallelismTest.jar "@$src_files"

rm -f "$src_files"
