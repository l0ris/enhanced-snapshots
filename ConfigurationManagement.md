# Configuration management

Enhanced Snapshots is rather flexible system, a lot of system setting can be configured depending on user needs and objects.   
Part of settings can be changed from UI and other in the configuration file.  

* [Properties editable from UI](#Properties-editable-from-UI)
* [Properties editable in the configuration file](#Properties-editable-in-the-configuration-file)

### Properties editable from UI
Properties editable from UI can be configured on Settings tab or during the initialization phase.  

While initialization user can configure:
- **SDFS volume size**. Possible volume size depends on available RAM, the more free RAM system has the bigger volume can be set. Minimum volume size is 50 GB
- **Bucket name.** It is possible to set custom S3 bucket name, but keep in mind that it should start with `enhancedsnapshots.` prefix and should meet [AWS requirements](http://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html). Once the system is initialized S3 bucket name can not be changed anymore.

![Settings(2)](https://cloud.githubusercontent.com/assets/13731468/16002643/714a3e68-3161-11e6-973f-38e2a1322c66.png)

After initialization next properties can be configured on Settings tab:
- **SDFS volume size.** As mentioned above max volume size depends on free RAM. User can only expand volume size.
- **SDFS local cache size.** SDFS stores all unique data in the cloud and uses a local writeback cache for performance purposes. This means the most recently accessed data is cached locally and only the unique chunks of data are stored at the cloud storage provider. Size of local cache depends on free storage size and can not exceed it. Current property can be applied only after SDFS restart, that's why it can be changed only when there aren't backup/restore tasks in progress.
- **Temporary Volume Type.** While backup Enhanced Snapshots creates temporal volume from original one, copy data from new volume to SDFS mount point and removes it once backup process is finished. By default, this volume will be `gp2` type. But it can be changed depending on user purposes. Next volume type are available for temporal volumes:  `standard`, `io1`, `gp2`. More information about AWS volume types can be found [here](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/EBSVolumeTypes.html)
- **Restore Volume Type.** Enhanced Snapshots creates a new volume to restore backup data. By default, this volume will be `gp2` but it can be changed to `standard` or `io1` as well. More information about AWS volume types can be found [here](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/EBSVolumeTypes.html)

- **Maximum Task Queue Size.** Currently, Enhanced Snapshots does not support concurrent tasks execution. In case there is one task in progress other will be added to the queue. By default queue can contain 20 tasks.
- **Amazon Retry Count:** In case one of AWS service returns AmazonServiceException Enhanced Snapshots will attempt to duplicate request. Current property set retry counts for such situations. 
- **Amazon Retry Sleep:** Current property defines timeout before new attempt to send a new request to AWS service after AmazonServiceException. Values for this property are in ms.

![Settings](https://cloud.githubusercontent.com/assets/13731468/16002646/7150056e-3161-11e6-8d4c-6b44dbf99019.png)

### Properties editable in the configuration file
Some properties not editable from UI can be changed in the configuration file. This file is available after system initialization and can be found by next path: `$CATALINA_HOME/conf/enhancedsnapshots.properties`. Changes in this file will be applied only after - system restart. Below are properties editable from config file:
- **enhancedsnapshots.retention.cron** - Cron schedule for retention policy. By default `00 00 * * ?`
- **enhancedsnapshots.polling.rate** - Polling rate for worker dispatcher to check whether there is a new task in the queue, in ms
- **enhancedsnapshots.wait.time.before.new.sync** - Wait time before new synchronization of Snapshot/Volume local data with AWS data after some changes with Snapshot/Volume, in seconds
- **enhancedsnapshots.max.wait.time.to.detach.volume** - Max wait time for volume to be detached, in seconds
