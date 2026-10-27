#!/usr/bin/env bash
set -e
separador=':'
case "$OSTYPE" in msys*|cygwin*) separador=';' ;; esac
java -Dstdout.encoding=UTF-8 -cp "out${separador}lib/*" mx.tecnm.orizaba.tp.veterinaria.app.PruebasModelo
