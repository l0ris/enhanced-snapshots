#!/bin/bash

COPYCAT_LOG="/var/log/copycat/copycat.log"

function ccstart() {
    APP_USER="copycat"
    APP_PATH="/opt/copycat" 
    APP_CONFIG="$APP_PATH/config.json"
    APP_LIBS="$APP_PATH/cc.jar:$APP_PATH/libs/*"
    JAVA_HOME="/usr/lib/jvm/java"
    APP_PRE_OPTION="$JAVA_HOME/bin/java -cp "
    APP_POST_OPTION="com.datish.copycat.Server $APP_CONFIG"

    su - $APP_USER -s "/bin/sh" -c "nohup $APP_PRE_OPTION $APP_LIBS $APP_POST_OPTION &> $COPYCAT_LOG < /dev/null &"
    sleep 2
}

function ccpid() {
    local PID=$(ps -Fu copycat | grep com.datish.copycat.Server | awk {'print $2'})
    if [ -z "$PID" ]; then
        echo 0
    fi

    echo $PID
}

#############################################################################


commandName="$1"

case "$commandName" in


############################### sdfs mount ##################################
--mount) echo "Mounting"
    is_restore="${2:-false}"
    echo 'SDFS restore: ' $2

    ### creating mountpoint
    if [[ ! -e /mnt/awspool ]]; then
               mkdir /mnt/awspool
            fi
            sleep 5

    touch /var/log/sdfs_mount.log

    ### mounting SDFS file system to /mnt/awspool
    sdfs_pid=`ps aux | grep "[f]use.SDFS.MountSDFS" | awk '{ print $2}'`
    if [ "$sdfs_pid" != "" ]; then
        echo 'SDFS is already mounted'
        exit 0
    else
        if ($is_restore) then
            echo 'Restoring SDFS from existed bucked'
            mount.sdfs awspool /mnt/awspool -cfr &> /var/log/sdfs_mount.log &
        else
            mount.sdfs awspool /mnt/awspool &> /var/log/sdfs_mount.log &
        fi

        tail -f /var/log/sdfs_mount.log | while read LOGLINE
        do
            [[ "${LOGLINE}" == *"Mounted Filesystem"* ]] && pkill -P $$ tail && echo "SDFS mounted successfully"
            [[ "${LOGLINE}" == *"Exception"* ]] && pkill -P $$ tail && echo 'Failed to mount SDFS'
            [[ "${LOGLINE}" == *"Service exit with a return value of 1"* ]] && pkill -P $$ tail && echo 'Failed to moint SDFS'
	    done
        if grep -q "Mounted Filesystem" "/var/log/sdfs_mount.log"; then
           exit 0;
        else
           exit 1;
        fi
    fi
    ;;
############################# sdfs unmount ##################################
--unmount) echo "Unmounting"
    sdfs_pid=`ps aux | grep "[f]use.SDFS.MountSDFS" | awk '{ print $2}'`
    if [ "$sdfs_pid" != "" ]; then
        umount /mnt/awspool > /dev/null
        trap "kill $sdfs_pid 2> /dev/null" EXIT
        while kill -0 $sdfs_pid 2> /dev/null; do
         sleep 1
        done
        trap - EXIT
        echo 'SDFS sucessfully unmounted  '
        exit 0
    else
        echo 'SDFS is already unmounted'
        exit 0
    fi
    ;;

############################# sdfs state ####################################
--state) echo "Determine sdfs state"
    sdfs_pid=`ps aux | grep "[f]use.SDFS.MountSDFS" | awk '{ print $2}'`
    if [ "$sdfs_pid" != "" ]; then
        echo 'sdfs is running';
        exit 0
    else
        echo 'sdfs is not running';
        exit 1
    fi
    ;;


############################# configure sdfs ####################################
--configure) echo "Configure SDFS"
    sdfs_volume_size="$2"
    bucket_name="$3"
    location="${4:-US Standard}"
    localCacheSize="${5:-1GB}"
    echo 'SDFS volume size: ' $2
    echo 'Bucket name: ' $3
    echo 'Location: ' $4
    echo 'Local cache size: '$5

    ### creating SDFS file system
    if [[ -e /etc/sdfs/awspool-volume-cfg.xml ]]; then
        echo 'SDFS already configured'
        exit 0
    else
        /sbin/mkfs.sdfs  --volume-name=awspool --volume-capacity=$sdfs_volume_size --aws-enabled=true --aws-aim --cloud-bucket-name=$bucket_name --aws-bucket-location=$location --local-cache-size=$localCacheSize --chunk-store-encrypt=true
        echo 'SDFS is configured'
        exit 0
    fi
    ;;

############################# configure sdfs node  ####################################
--configurenode) echo "Configure SDFS node"
    sdfs_volume_size="$2"
    bucket_name="$3"
    location="${4:-US Standard}"
    localCacheSize="${5:-1GB}"
    encryptionKey="$6"
    storeIV="$7"
    cliPass="${8:-apassword}"
    echo 'SDFS volume size: ' $2
    echo 'Bucket name: ' $3
    echo 'Location: ' $4
    echo 'Local cache size: '$5
    echo 'Encryption Key: '$6
    echo 'StoreIV: '$7
    echo 'CLI password: '$8

    ### creating SDFS file system
    if [[ -e /etc/sdfs/awspool-volume-cfg.xml ]]; then
        echo 'SDFS node already configured'
        exit 0
    else
        /sbin/mkfs.sdfs  --volume-name=awspool --volume-capacity=$sdfs_volume_size --aws-enabled=true --aws-aim --cloud-bucket-name=$bucket_name --aws-bucket-location=$location --local-cache-size=$localCacheSize --chunk-store-encrypt=true --chunk-store-encryption-key=$encryptionKey --chunk-store-iv=$storeIV --enable-replication-master --sdfscli-password=$cliPass
        echo 'SDFS node is configured'
        exit 0
    fi
    ;;

############################# list volumes (you can use regexp pattern for true/false column)  ####################################
--showvolumes) echo "Volumes"
    cliPass="${2:-apassword}"
    # true cause that local volume is listed, false remote, .* all
    pattern="${3:-true}"
    echo 'CLI pass: '$2
    echo "Pattern: $pattern"
    /sbin/sdfscli --list-cloud-volumes --password=$cliPass | awk 'NR > 3 && $12~/'"$pattern"'/ {print $4}' | grep -v '^$'
    ;;
        
############################# Syncing remote volumes ####################################
--syncvolumes) echo "Syncing volumes"
    cliPass="${2:-apassword}"
    pattern="false"
    echo 'CLI pass: '$2
    echo "Pattern: $pattern"
    for volume in `/sbin/sdfscli --list-cloud-volumes --password=$cliPass | awk 'NR > 3 && $12~/'"$pattern"'/ {print $4}' | grep -v '^$'`;
    do
        echo "Syncing volume ${volume}"
        /sbin/sdfscli --sync-remote-cloud-volume=${volume} --password=${cliPass}
    done
    ;;

############################# expand volume ####################################
--expandvolume) echo "Expanding volume"
    sdfs_mount_point="$2"
    sdfs_volume_size="$3"

    echo 'Expanding sdfs volume ' $3 'to '$2

    ### expanding SDFS volume
    cd $sdfs_mount_point
    sdfscli --expandvolume $sdfs_volume_size
    ;;

    ############################# cloud sync ####################################
--cloudsync) echo "Sync local sdfs metadata with cloud"

    ### sync sdfs metadata
    cd $sdfs_mount_point
    sdfscli --cloud-sync-fs
    ;;

############################# CopyCat start ####################################
--ccstart) echo "Starting CopyCat"
    PID=$(ccpid)
    if [ $PID -eq 0 ]; then
        ccstart
        tail -n 10 $COPYCAT_LOG
        PID=$(ccpid)
        if [ $PID -eq 0 ]; then
            echo "Problem while starting CopyCat. Check logs"
            exit 1
        fi
        exit 0
    else
        echo "CopyCat is running" 
        exit 0
    fi
    ;;

############################# CopyCat stop ####################################
--ccstop) echo "Stopping CopyCat"
    PID=$(ccpid)
    if [ $PID -ne 0 ]; then
        kill $PID
        sleep 2
        PID=$(ccpid)
        if [ $PID -ne 0 ]; then
            echo "It seems that there is problem with CopyCat"
            kill -9 $PID
        fi
        exit 0
    else
        echo "CopyCat is already stopped"
        exit 0
    fi
    ;;

############################# CopyCat restart ####################################
--ccrestart) echo "Restarting CopyCat"
    PID=$(ccpid)
    if [ $PID -ne 0 ]; then
        echo "Stopping CopyCat"
        kill $PID
        sleep 2
        PID=$(ccpid)
        if [ $PID -ne 0 ]; then
            echo "CopyCat still running - sending 9"
            kill -9 $PID
        fi
        echo "Starting CopyCat"
        ccstart
     	PID=$(ccpid) 
        if [ $PID -eq 0 ]; then
            echo "Problem while starting CopyCat. Check logs"
            tail -n 10 $COPYCAT_LOG
            exit 1
        fi
        tail -n 10 $COPYCAT_LOG
        exit 0
    else
        echo "Starting CopyCat"
        ccstart
        PID=$(ccpid)
        if [ $PID -eq 0 ]; then
            echo "Problem while starting CopyCat. Check logs"
            tail -n 10 $COPYCAT_LOG
            exit 1
        fi
        exit 0
    fi
    ;;

############################# CopyCat status ####################################
--ccstatus) echo "Checking status of CopyCat"
    PID=$(ccpid)
    if [ $PID -ne 0 ]; then
        echo "CopyCat is running"
        exit 0
    else
        echo "CopyCat is stopped"
        exit 1
    fi    
    ;;
esac
