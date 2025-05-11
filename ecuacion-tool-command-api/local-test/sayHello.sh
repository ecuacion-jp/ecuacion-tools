#!/bin/bash

LOG_DIR_PATH=~/Desktop/development/git/ecuacion-tools/ecuacion-tool-command-api/target/local-test
LOG_FILE_PATH=${LOG_DIR_PATH}/test.log

mkdir -p ${LOG_DIR_PATH}
date >> ${LOG_FILE_PATH}
echo Hello! >> ${LOG_FILE_PATH}
echo Paramters : $@ >> ${LOG_FILE_PATH}
