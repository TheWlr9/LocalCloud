#!/bin/sh
SERVICE_NAME=strmServer
PATH_TO_DIR=/home/pi/JavaPrograms/StrmServer
PATH_TO_JAR=/home/pi/JavaPrograms/StrmServer/StrmServerBE.jar
PATH_TO_OUT=/home/pi/JavaPrograms/StrmServer/etc/StrmServer.out
PID_PATH_NAME=/tmp/strmServer-pid
#I need to remember the PID so the system can send the termination signal to it properly.
case $1 in
    start)
        echo "Starting $SERVICE_NAME ..."
        if [ ! -f $PID_PATH_NAME ]; then
	    ROOTDIR=$(pwd)
            cd $PATH_TO_DIR
            sudo nohup java -jar -server StrmServerBE.jar myPassword docs/ > $PATH_TO_OUT 2>&1 &
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
	    cd $PATH_TO_DIR
	    cd etc/
	    #echo "0" > log.cfg
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
	    cd $PATH_TO_DIR
	    cd etc/
	    #echo "0" > log.cfg
            echo "$SERVICE_NAME starting ..."
            ROOTDIR=$(pwd)
	    cd $PATH_TO_DIR
            sudo nohup java -jar -server StrmServerBE.jar myPassword docs/ > $PATH_TO_OUT 2>&1 &
            cd $ROOTDIR
            echo $! > $PID_PATH_NAME
            echo "$SERVICE_NAME started ..."
        else
            echo "$SERVICE_NAME is not running ..."
        fi
    ;;
esac
