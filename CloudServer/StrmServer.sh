#!/bin/sh
SERVICE_NAME=strmServer
PATH_TO_DIR=/home/pi/JavaPrograms/CloudServerApplication
PATH_TO_CLASS=/home/pi/JavaPrograms/CloudServerApplication/StrmServer.class
PATH_TO_OUT=/home/pi/JavaPrograms/CloudServerApplication/StrmServer.out
PID_PATH_NAME=/tmp/strmServer-pid
#I need to remember the PID so the system can send the termination signal to it properly.
case $1 in
    start)
        echo "Starting $SERVICE_NAME ..."
        if [ ! -f $PID_PATH_NAME ]; then
	    ROOTDIR=$(pwd)
            cd $PATH_TO_DIR
            nohup java StrmServer > $PATH_TO_OUT 2>&1 &
            cd $ROOTDIR
            echo $! > $PID_PATH_NAME
            echo "$SERVICE_NAME started ..."
        else
            echo "$SERVICE_NAME is already running ..."
        fi
    ;;
    stop)
        if [ -f $PID_PATH_NAME ]; then
            PID=$(cat $PID_PATH_NAME);
            echo "$SERVICE_NAME stoping ..."
            kill $PID;
            echo "$SERVICE_NAME stopped ..."
            rm $PID_PATH_NAME
        else
            echo "$SERVICE_NAME is not running ..."
        fi
    ;;
    restart)
        if [ -f $PID_PATH_NAME ]; then
            PID=$(cat $PID_PATH_NAME);
            echo "$SERVICE_NAME stopping ...";
            kill $PID;
            echo "$SERVICE_NAME stopped ...";
            rm $PID_PATH_NAME
            echo "$SERVICE_NAME starting ..."
            ROOTDIR=$(pwd)
	    cd $PATH_TO_DIR
            nohup java StrmServer > $PATH_TO_OUT 2>&1 &
            cd $ROOTDIR
            echo $! > $PID_PATH_NAME
            echo "$SERVICE_NAME started ..."
        else
            echo "$SERVICE_NAME is not running ..."
        fi
    ;;
esac
