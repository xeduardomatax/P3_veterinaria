#!/usr/bin/env bash
# Compila sin Maven. Coloca SQLite JDBC 3.46.1.3, SLF4J API y FlatLaf 3.5.4 en lib/.
set -e
mkdir -p out
javac --release 21 -Xlint:all -encoding UTF-8 -d out -cp 'lib/*' $(find src/main/java -name '*.java')
