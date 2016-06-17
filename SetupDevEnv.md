## Setting development environment for Enhanced Snapshots version 0.0.2

Detailed steps to set development environment:  

1) Install Java 8  
http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
2) Set JAVA_HOME environment variable which points to your JDK installation
3) Install Tomcat8  
https://tomcat.apache.org/download-80.cgi
4) Set CATALINA_HOME environment variable which points to your Tomcat8 installation
5) Install maven 3.3.3 according to description from official site  
https://maven.apache.org/install.html
6) Install git   
https://git-scm.com/downloads
7) Download enhanced-snapshots sources from GitHub repo:  
* For UNIX users: 
`git clone https://github.com/SungardAS/enhanced-snapshots.git ~/enhanced-snapshots-repo`  
* For WINDOWS users:  
`git clone https://github.com/SungardAS/enhanced-snapshots.git %CATALINA_HOME%\enhanced-snapshots-repo` 

## Starting Enhanced Snapshots version 0.0.2 locally
By default Enhanced Snapshots application should be started with AWS instance but for developments purposes there is a way to start it locally in special "dev" mode. Below are detailed steps to start Enhanced Snapshots locally:
1) Set `spring.profiles.active` property in ~/enhanced-snapshots-repo/WebContent/WEB-INF/web.xml file to `dev`
2) Build enhancedsnapshots-0.0.2.war file and copy it to Tomcat webapp directory
* For UNIX users:
```sh
$ cd enhanced-snapshots-repo
$ mnv clean package
$ cp ~/enhanced-snapshots-repo/target/enhancedsnapshots-0.0.2.war $CATALINA_HOME/webapp
```
* For WINDOWS users:
```sh
$ CD %HOMEPATH%\enhanced-snapshots-repo
$ mnv clean package
$ copy target\enhancedsnapshots-0.0.2.war %CATALINA_HOME%\webapp
```
4) Add enhancedsnapshots.properties files to Tomcat conf directory with next content:  
>*amazon.aws.accesskey= `Access Key ID encrypted with PBEWithMD5AndDES algorithm`*  
>*amazon.aws.secretkey= `Secret Access Key encrypted with PBEWithMD5AndDES algorithm`*  
>*amazon.s3.bucket=`DEV`*  
>*amazon.aws.region=`eu-west-1`*  
5) Start Tomcat with enhanced snapshots application  
* For UNIX users:
```sh
$ cd $CATALINA_HOME
$ bin/catalina.sh start
```
* For WINDOWS users:
```sh
$ CD %CATALINA_HOME%
$ bin/catalina.bat start
```
## Testing changes at EC2 instance
To test changes at ec2 instance in prod mode next steps can be taken:
1) Set `spring.profiles.active` property in ~/enhanced-snapshots-repo/WebContent/WEB-INF/web.xml file to `prod`
2) Create new enhancedsnapshots-0.0.2.war file with required changes by executing command `mnv clean package`
3) Start new instance from latest AMI
4) At newly created instance replace enhancedsnapshots-0.0.2.war file under `$CATALINA_HOME/webapp` directory with the new one:
```sh
$ scp -i yourKey.pem enhancedsnapshots-0.0.2.war ec2-user@instanceIP:/opt/apache-tomcat-8.0.24/webapps/ROOT.war
```