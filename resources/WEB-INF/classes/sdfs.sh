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
esac
