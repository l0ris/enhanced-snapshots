# Enhanced Snapshots

![Enhanced Snapshots Logo](https://cloud.githubusercontent.com/assets/1557544/10324458/c19466ca-6c57-11e5-8318-a2eb1cd9e99b.png)

[![Version][github-image]][github-url]
[![Build Status][travis-image]][travis-url]

*Table of contents*
* [Product Description](#product-description)
* [Key Features](#key-features)
* [Limitations](#limitations)
* [Quick start](#quick-start)
* [Getting Started](#getting-started)
* [Management Tasks](#management-tasks)
* [Removing the Enhanced Snapshots system](#removing-the-enhanced-snapshots-system)
* [IAM Role creation (optional)](#iam-role-creation-optional)
* [License](#license)

# Product Description
Enhanced Snapshots, from Sungard Availability Services | Labs, manages EBS snapshots and performs data deduplication to S3 for greater cost savings. The product will be useful for AWS users who want to :
* Reduce the cost of storing snapshots
* Reduce the time IT engineers spend on routine snapshot management tasks
* Schedule recurring snapshots

Deduplication is run across all enabled snapshots in a region, which decreases the amount of total data stored.  Furthermore, deduplicated blocks are stored in S3 at a much lower cost than AWS’ standard EBS snapshots.  This tool provides users with a great way to pay less for long-term retention of snapshot based data in AWS.

Using an intuitive interface, users can easily automate routine tasks like the creation of snapshots and the deletion of old backups. Since these tasks are automated, risks associated with human error are minimized.

Technical support is not available for the first version of the product; however, please create a Github issue if you have any comments or suggestions. Customer support service may be added in a future release.

Enhanced Snapshots is open sourced and licensed under Apache v 2.0. Use of Enhanced Snapshots software is free and you only pay for the underlying infrastructure required to support it.

This tool will be available by launching the AMI from the [enhanced snapshots market place](https://aws.amazon.com/marketplace/pp/B01CIWY4UO) by choosing the "Enhanced Snapshots tool creation stack" option or by creating a role like es-admin-role while using the single AMI option. Cloud formation template mentioned in the [Quick start](https://github.com/sungardas/enhanced-snapshots#quick-start) section can be used to create the es-admin role. Similarly, the EC2 instance created and the associated resources can be removed by using the "Uninstall" button under for the Settings tab, however, this will not remove the customer from the AWS subscription for the SungardAS provided marketplace products. Refer to the [Removing the enhanced snapshots system](https://github.com/sungardas/enhanced-snapshots#removing-the-enhanced-snapshots-system) for more details.

# Key Features
## Backup & Recovery
* Performing backups of EBS volumes.
* The ability to perform recovery from historical backups.
* The ability to store backups of deleted volumes.
* Ability to quickly initiate recovery of backups.

## Schedule Policy
* Allows user to create scheduled tasks by minute, hour, day, week and month.
* Full support of CRON expressions.

## Retention Policy
* Allows user to automatically delete older backups based on the original volume’s size, count or age.
* A user can cancel any task before it is moved to the running state.

## Management
* The ability to assign different (two) roles for users.
* Simple and intuitive wizard for initial setup process.
* The ability to migrate to another EC2 instance with full restoring of data

# Limitations
* No support for management of volumes using OS level RAID
* EBS volumes using EBS encryption must be [pre-warmed](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ebs-prewarm.html) to avoid significant storage overhead

# Quick start
- If the "Enhanced Snapshots tool creation stack" is used in the marketplace , the es-admin role will be automatically created.
But for the single AMI option from the marketplace Create a es-admin role using the cloud formation template as prerequisite [es-admin-role](https://github.com/SungardAS/condensation-particles/blob/master/particles/cftemplates/sungardas_enhanced_snapshots_admin_role.template.json)
 To launch a CloudFormation stack based on the template, first decide which region you will deploy in. In that region you will need the following information:

* An [EC2 keypair](https://us-east-1.console.aws.amazon.com/ec2/v2/home?#KeyPairs)
* A [VPC id](https://console.aws.amazon.com/vpc/home?#vpcs:) (Not needed if you are using the Simple Stack option below.)
* An IP prefix from which inbound http/https connections will be allowed. If you're not sure, use your current [public IP address](http://www.myipaddress.com/show-my-ip-address/) with "/32" tacked on the end (like "1.2.3.4/32").
* An IP prefix from which inbound ssh connections will be allowed. If you're not sure, use your current [public IP address](http://www.myipaddress.com/show-my-ip-address/) with "/32" tacked on the end (like "1.2.3.4/32").
* Size of Instance EBS volume (in GB). Size can be as small as 8 or higher for large amount of data keys storage. By default, 48GB will be used.

**Note** When creating the CloudFormation stack, on the Review page, make sure to check the box at the bottom of the page in the Capabilities section.

![Capabilities_checkbox](https://cloud.githubusercontent.com/assets/1557544/10256747/2feb29cc-6921-11e5-9d3b-d7974fb5753f.png)

Once you have collected that information, find your target region, note the AMI ID for that region, and click the corresponding Launch Stack link.

| Region         | AMI ID        | Simple Stack (default VPC) | VPC Stack (your VPC) |
| -------------- | ------------- | ------------ |---------- |
| us-east-1      | ami-fd838b97  | [![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=us-east-1#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.us-east-1.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots.template.json)|[![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=us-east-1#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.us-east-1.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots_with_vpc.template.json)|
| us-west-1      | ami-b0b5c8d0  | [![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=us-west-1#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.us-west-1.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots.template.json)|[![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=us-west-1#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.us-west-1.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots_with_vpc.template.json)|
| us-west-2      | ami-e0fc1680  | [![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=us-west-2#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.us-west-2.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots.template.json)|[![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=us-west-2#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.us-west-2.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots_with_vpc.template.json)|
| eu-west-1      | ami-75a32506  | [![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=eu-west-1#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.eu-west-1.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots.template.json)|[![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=eu-west-1#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.eu-west-1.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots_with_vpc.template.json)|
| sa-east-1      | ami-0a2fa366  | [![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=sa-east-1#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.sa-east-1.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots.template.json)|[![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=sa-east-1#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.sa-east-1.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots_with_vpc.template.json)|
| eu-central-1      | ami-7148ae1e  | [![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=eu-central-1#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.eu-central-1.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots.template.json)|[![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=eu-central-1#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.eu-central-1.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots_with_vpc.template.json)|
| ap-southeast-1 | ami-0c2ce66f  | [![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=ap-southeast-1#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.ap-southeast-1.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots.template.json)|[![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=ap-southeast-1#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.ap-southeast-1.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots_with_vpc.template.json)|
| ap-southeast-2      | ami-ff01229c  | [![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=ap-southeast-2#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.ap-southeast-2.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots.template.json)|[![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=ap-southeast-2#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.ap-southeast-2.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots_with_vpc.template.json)|
| ap-norhteast-1      | ami-e7aabd89  | [![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=ap-northeast-1#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.ap-northeast-1.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots.template.json)|[![Launch Stack](https://s3.amazonaws.com/cloudformation-examples/cloudformation-launch-stack.png)](https://console.aws.amazon.com/cloudformation/home?region=ap-northeast-1#/stacks/new?stackName=enhanced-snapshots&templateURL=http://condensation-particles.ap-northeast-1.s3.amazonaws.com/master/particles/cftemplates/sungard_marketplace_enhanced_snapshots_with_vpc.template.json)|

Once the CloudFormation stack has finished building, go to its Outputs tab at the bottom of the AWS Console. Copy the instance ID (you will need it in a later step) and click the URL, then proceed to [Getting Started](#getting-started).

User also should:
* Select minimum m3.large instance and minimum 8 GB size for volume
* Select es_admin role while launching instance to get access to CloudWatch

![Role](https://cloud.githubusercontent.com/assets/13747035/11899135/bcaaee2c-a5a5-11e5-965b-78d60d64f3b8.png)

# Getting Started
**Note** If you have not followed the [Quick start](#quick-start) section above, then you will first need to manually [create an IAM role](#iam-role-creation-optional) and then create an EC2 instance using the Enhanced Snapshots AMI, which can be found in the first table above.

**Note** By default a new instance has a self-signed SSL certificate, so you will need to bypass your browser's security warning to start.

*Step 1*

For the first login please use the following credentials:
* Login: admin@enhancedsnapshots
* Password:  Your AWS EC2 Instance ID (available on the [AWS console](https://console.aws.amazon.com/ec2))
![Login](https://cloud.githubusercontent.com/assets/14750068/10096550/1bbf5294-637b-11e5-93d2-e1de26060c46.png)

*Step 2*

The next picture shows the list of additional resources that will be created: S3 bucket, SDFS settings, and DynamoDB tables. At this step, it is possible to change some default configuration settings as S3 bucket name and SDFS volume size. Name for S3 bucket should start with prefix `enhancedsnapshots.` and meet general [AWS requirements](http://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html) for bucket names. To view more information about these resources the user can click the question mark.
![Settings(2)](https://cloud.githubusercontent.com/assets/13731468/16002643/714a3e68-3161-11e6-973f-38e2a1322c66.png)

**Warning!**
Once "Setup" has been clicked on this screen, S3 bucket name cannot be changed.

*Step 3*

Time to create a first user. The first user always will receive admin rights. Email is used as user ID. For security reason password should not be less than 8 characters and must contain characters from three of the following four categories:
* Uppercase characters
* Lowercase characters
* Base 10 digits (0 through 9)
* Nonalphanumeric characters: ~!@#$%^&*_-+=`|\(){}[]:;"'<>,.?/

![New user](https://cloud.githubusercontent.com/assets/13731468/16002644/714e7ee2-3161-11e6-84e7-2e2c18821654.png)

*Step 4*

After the system is received all necessary input data it will create all necessary environment.
![Please wait](https://cloud.githubusercontent.com/assets/13731468/16002645/714ec082-3161-11e6-9865-fdb3849e5f4e.png)

After the configurations process will be successfully completed, the following notification will appear.
![Congratulations](https://cloud.githubusercontent.com/assets/13731468/16002636/6c8a9d46-3161-11e6-8b3c-d866630d7111.png)

*Step 5*

The system automatically redirects the user to the login page. Now the user will use their credentials that were created in step 4.

![Login](https://cloud.githubusercontent.com/assets/14750068/10096556/1bdcd81e-637b-11e5-86ff-205b3d992a13.png)

After logging in, the list of EBS volumes for the local region is displayed.
![Volumes](https://cloud.githubusercontent.com/assets/14750068/10096555/1bda2dd0-637b-11e5-86b5-87d4463e337d.png)


# Management Tasks
## Performing Manual Backups
To perform a backup, the user selects the appropriate EBS volume and clicks the button Backup selected. Also, the user can configure multi-backup, or several volumes during one task. For this, a user selects several appropriate volumes and clicks the button Backup selected. (Backups are performed one at a time.)
![Volumes (backup selected)](https://cloud.githubusercontent.com/assets/14750068/10023447/e410e4d8-615a-11e5-8bc3-93b9b9580528.png)

## Creating a Schedule
The user can automate the process of creating backups thanks to the Schedule feature. Schedules can be edited with Enhanced Snapshots web UI; schedules are displayed and stored in [Cron](https://en.wikipedia.org/wiki/Cron) format. The interval of backups is from one minute to one year. If necessary, schedules can be disabled.

![New schedule](https://cloud.githubusercontent.com/assets/14750068/10096546/1b936620-637b-11e5-859c-6e4e1b58d62e.png)

Users can also edit or delete existing schedules.
![Schedule menu](https://cloud.githubusercontent.com/assets/1557544/10000004/8c13719a-6067-11e5-9775-11e166d143d1.png)

## Managing Retention Policies
The retention policy function that allows the user to automatically delete backups according to certain conditions: size limit, count limit and days limit.
![Edit retention rule](https://cloud.githubusercontent.com/assets/1557544/9996404/c7337b3e-6054-11e5-8e12-5a0c7e139134.png)

Only one retention policy can be created for each volume. If a volume does not have any backups, the retention policy cannot be created.

Filters for users are available in order to sort according to different parameters: Volume ID, Name, Size, Instance ID and date of creation.
![Filter](https://cloud.githubusercontent.com/assets/13747035/11899172/ee749502-a5a5-11e5-829c-ec2a3721db73.png)

## Managing System Settings
Enhanced Snapshots is rather flexible system a lot of system setting can be configured depending on user needs and objects. More details about this can be found in [Configuration Management Document](https://github.com/SungardAS/enhanced-snapshots/blob/master/ConfigurationManagement.md).

## Managing System Migration
System can be easily migrated to another EC2 instance without data loss. Follow next steps to migrate system:
* ensure that there isn't any task in progress
* perform system backup by clicking the "Backup now" button on the Settings tab.
* note down bucket name, you will need it later
* uninstall current system as described [here](#removing-the-enhanced-snapshots-system) but leave S3 bucket untouched
 ![Delete](https://cloud.githubusercontent.com/assets/13731468/16002642/71467300-3161-11e6-9075-7d57274eee9e.png)
* start new instance with the same or newer Enhanced Snapshots version
* while initial configuration step choose from the drop-down list S3 bucket from the previous system

##  Other Management Tasks
A list of all active and pending tasks can be found in the tab Tasks. The user can cancel any task before it starts running.

A list of all users is available on the Users tab. All information about users except their passwords is displayed. Users with admin rights can make changes to all users (even other administrators). A user without administrative rights can edit only their user profile. If there is only one administrator user, administrator rights cannot be revoked.

# Removing the Enhanced Snapshots system
If you choose to remove Enhanced Snapshots, you can do so by clicking the Uninstall button on the Settings tab.

![Settings](https://cloud.githubusercontent.com/assets/13731468/16004054/60019302-3168-11e6-83b7-2e3b1331cea8.png)

Select "Yes" from the drop-down list to remove S3 bucket as well.
The system will continue with the removal of all resources once you enter the EC2 Instance Id for the EC2 instance that Enhanced Snapshots is running on.

![Delete](https://cloud.githubusercontent.com/assets/13731468/16002641/712427f0-3161-11e6-94fd-8430bef45f26.png)

The following resources are deleted:
* EC2 Instance
* S3 bucket and all backup data
* DynamoDB tables

**Note!** Though the EC2 instance will be deleted it will not remove the subscription to the software product from AWS and for more
  details on this refer to https://aws.amazon.com/marketplace/help/200799470

**Note!** It may take several minutes to delete all the resources, especially if backup data has been stored.

# IAM role creation (optional)
If you are creating an instance from the AMI directly without using the provided CloudFormation template, you must first create an IAM role with the following policy as defined in this template. [es-admin role cloud formation template](https://github.com/SungardAS/condensation-particles/blob/master/particles/cftemplates/sungardas_enhanced_snapshots_admin_role.template.json)
Once the role is created, also create and save an API key, which will be needed to configure Enhanced Snapshots.

Without a properly configured role, the following error message will appear during configuration:
![DynamoDBAccessDenied](https://cloud.githubusercontent.com/assets/14750068/10131876/08b816c8-65dc-11e5-871e-0f8d5fcdd303.png)

# Logging

Application uses AWS CloudWatch as logs storage. Logs can be found in the following location:
![Logs](https://cloud.githubusercontent.com/assets/13747035/11899181/fc56c14a-a5a5-11e5-9b26-c764b65bdbd6.png)

# License

See the [LICENSE.md](LICENSE.md) file for license rights and limitations (Apache 2).

Use of the provided AMIs is covered by a separate [End User License Agreement](https://s3-us-west-2.amazonaws.com/sgaslogo/EULA_Enhanced+Snapshots+for+AWS+2015-10-27.docx).

## Sungard Availability Services | Labs
[![Sungard Availability Services | Labs][labs-image]][labs-github-url]

This project is maintained by the Labs team at [Sungard Availability
Services][sgas-url]

GitHub: [https://sungardas.github.io](https://sungardas.github.io)

Blog:
[http://blog.sungardas.com/CTOLabs/](http://blog.sungardas.com/CTOLabs/)


[sgas-url]: https://sungardas.com
[labs-github-url]: https://sungardas.github.io
[labs-image]: https://raw.githubusercontent.com/SungardAS/repo-assets/master/images/logos/sungardas-labs-logo-small.png
[travis-image]: https://travis-ci.org/SungardAS/enhanced-snapshots.svg?branch=master
[travis-url]: https://travis-ci.org/SungardAS/enhanced-snapshot
[github-image]: https://d25lcipzij17d.cloudfront.net/badge.svg?id=gh&type=6&v=2.0.0&x2=0
[github-url]: https://badge.fury.io/gh/sungardas%2Fenhanced-snapshots
