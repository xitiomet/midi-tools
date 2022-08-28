@echo off
cd target/
java -Djava.library.path=./natives/ -jar midi-tools-1.3.jar $*
cd ..
