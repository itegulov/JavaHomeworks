#!/bin/bash

rm -rf build

mkdir build

manifest=/tmp/MANIFEST.MF
src_files=/tmp/src_files.txt

find src/ru/ifmo/ctddev/itegulov/implementor -type f -not -path "*src/ru/ifmo/ctddev/itegulov/implementor/example*" -not -path "*src/ru/ifmo/ctddev/itegulov/implementor/test*" -name '*.java' > "$src_files"

javac -classpath /home/itegulov/Downloads/ImplementorTest.jar -d build "@$src_files"

echo "Manifest-Version: 1.0" > "$manifest"
echo "Class-Path: /home/itegulov/Downloads/ImplementorTest.jar" >> "$manifest"
echo "Main-Class: ru.ifmo.ctddev.itegulov.implementor.Runner" >> "$manifest"

jar cvfm implementor.jar "$manifest" -C build/ .

rm -f "$manifest" "$src_files"
