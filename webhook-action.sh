#!/bin/bash

if [ "$EUID" -ne 0 ]
  then echo "Please run as root"
  exit
fi

REPOSITORY_NAME=$1;
BRANCH_NAME=$2;

systemctl restart $REPOSITORY_NAME"-"$BRANCH_NAME

exit 0;