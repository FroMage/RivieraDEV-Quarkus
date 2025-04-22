#! /bin/sh
#
# Startup script for rivieradev-quarkus-2024.
#
# Stephane Epardaud <stef@epardaud.fr>
# Copyright 2024, Stephane Epardaud
#

### BEGIN INIT INFO
# Provides: rivieradev-quarkus-2024
# Required-Start: $local_fs $network $named
# Required-Stop: $local_fs $network $named
# Default-Start:  2 3 4 5
# Default-Stop: 0 1 6
# Short-Description: start and stop rivieradev-quarkus-2024
# Description: rivieradev-quarkus-2024 application
### END INIT INFO

APP=rivieradev-quarkus-2024
USER=quarkus
CLUSTER=

PATH=/sbin:/bin:/usr/sbin:/usr/bin
QUARKUS_PROFILE=
QUARKUS_ARGS=-Djava.io.tmpdir=/tmp/$APP

if test -n "$CLUSTER"
then
	APP_FOLDER=$APP/$CLUSTER
	APP_NAME=$APP-$CLUSTER
	QUARKUS_PROFILE=prod-$CLUSTER
else
	APP_FOLDER=$APP
	APP_NAME=$APP
fi

APP_PATH=/usr/share/$APP_FOLDER
QUARKUS_PID_FOLDER=/var/run/$APP_FOLDER
export QUARKUS_PID_PATH=$QUARKUS_PID_FOLDER/$APP.pid
export QUARKUS_LOG_PATH=/var/log/$APP_FOLDER
DESC="$APP_NAME application"

. /lib/lsb/init-functions

if test -f /etc/default/$APP_NAME
then
	. /etc/default/$APP_NAME
fi

test -d $APP_PATH || exit 0

if test "$IS_QUARKUS_CONFIGURED" = 0
then
	log_failure_msg "You must configure $APP_NAME in /etc/default/$APP_NAME before running it"
	case "$1" in
    	stop)
    	exit 0
    ;;
    *)
		exit 1
	;;
	esac
fi

# setup the id
if test -n "$QUARKUS_PROFILE"
then
	QUARKUS_ARGS="$QUARKUS_ARGS -Dquarkus.profile=$QUARKUS_PROFILE"
fi

# make sure the PID can be written
if test \! -d $QUARKUS_PID_FOLDER
then
	mkdir -p $QUARKUS_PID_FOLDER
	chown $USER.$USER $QUARKUS_PID_FOLDER
fi

set -e

case "$1" in
    start)
        log_daemon_msg "Starting $DESC" $APP

		if start-stop-daemon --start --background --make-pidfile --chdir $APP_PATH --pidfile $QUARKUS_PID_PATH --user $USER --chuid $USER:$USER --quiet --exec $APP_PATH/$APP -- $QUARKUS_ARGS > /dev/null
		then
			log_end_msg 0
		else
			log_failure_msg "failed (already running?)."
			log_end_msg 1
			exit 1
		fi
	;;

    stop)
		log_daemon_msg "Stopping $DESC" $APP
		if start-stop-daemon --stop --oknodo --pidfile $QUARKUS_PID_PATH --user $USER --chuid $USER:$USER --quiet --retry 5
		then
			test -f $QUARKUS_PID_PATH && rm $QUARKUS_PID_PATH
			log_end_msg 0
		else
			log_failure_msg "failed (not running?)."
			log_end_msg 1
			exit 1
		fi
	;;

    restart|force-reload)
        $0 stop
        $0 start
    ;;

    status)
	if test -f $QUARKUS_PID_PATH
	then
		PID=$(cat $QUARKUS_PID_PATH)
		if (grep '(java)' /proc/$PID/stat && grep quarkus /proc/$PID/cmdline) > /dev/null
		then
			log_success_msg "$DESC is running"
			exit 0
		else
			log_success_msg "$DESC is not running"
			exit 3
		fi
	else
		log_success_msg "$DESC is not running"
		exit 3
	fi
    ;;

    *)
        log_action_msg "Usage: $0 {start|stop|restart|force-reload|status}" >&2
        exit 1
    ;;
esac

exit 0
