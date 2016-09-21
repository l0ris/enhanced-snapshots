## Setting email notification for Enhanced Snapshots version 0.0.3

ESS tool can send email notifications about task and system status. 
You can setup SMTP connection to your mail server at initialization stage 
![Enhanced Snapshots initialization stage](https://cloud.githubusercontent.com/assets/3055547/18709946/524537e0-800b-11e6-8f9b-44bf8147961b.png)

or at the settings page. 

![Enhanced Snapshots settings page](https://cloud.githubusercontent.com/assets/3055547/18710305/2f3736d4-800d-11e6-863c-2a5096f8385b.png)

Settings are the same on both pages.

![Enhanced Snapshots settings page](https://cloud.githubusercontent.com/assets/3055547/18710297/295064f2-800d-11e6-8025-334eff8cbb99.png)

There are:
Domain - using for callback links
Sender email address - not reply address for notification
User name - SMTP server account user name
Password - SMTP server account password
Email SMTP host - SMTP server host name
Email SMTP port - SMTP server port
Connection type - connection type to SMTP server. TLS or SSL
Type(s) of event - types, about which system will inform users 
Recipients - list of recipients

Check connection is an option to check settings. The system will try to send the test message to the email address that entered in this field. 
If the message received successfully, then settings are correct.

![Enhanced Snapshots email settings example](https://cloud.githubusercontent.com/assets/3055547/18710301/2d13d448-800d-11e6-98f3-204f69bcdd4f.png)

This is setup with default email templates. There are 3 types of templates: Success, Fail, Info.

ESS tool using [Apache FreeMarker](http://freemarker.org/) template engine. So, all default templates can be replaced with custom.

It can be done in few steps:

1) Create template like the default one ([Default success template](https://github.com/SungardAS/enhanced-snapshots/blob/develop/resources/WEB-INF/classes/success.ftl))

Template can contain HTML tags, styles and link to images.
In templates are defined few variables to do it more interactive.

#### Variables for successful template:

${domain} - domain value from settings

${task.id} - task id

${task.worker} - worker id (usually identify node that run task)

${task.status} - task status (waiting, running, queued, complete, canceled, error)

${task.type} - task type (backup, restore, delete, system_backup, unknown)

${task.volume} - source volume id

#### Variables for error template:

${domain} - domain value from settings
${task.id} - task id
${task.worker} - worker id (usually identify node that run task)
${task.status} - task status (waiting, running, queued, complete, canceled, error)
${task.type} - task type (backup, restore, delete, system_backup, unknown)
${task.volume} - source volume id
${errorMessage} - error message

#### Variables for info template:

${domain} - domain value from settings
${message} - information message

2) Put custom templates into ESS tool file system
3) Add properties to EnhancedSnapshots.properties file (for AMI users: /opt/tomcat-latest/conf/EnhancedSnapshots.properties)
There are applicable next options for email notification:

enhancedsnapshots.mail.success.template.path=/templates/success.ftl      **(path to success template)**
enhancedsnapshots.mail.success.subject=Success notification              **(success subject)**
enhancedsnapshots.mail.error.template.path=/templates/error.ftl          **(path to error template)**
enhancedsnapshots.mail.error.subject=Error notification                  **(error subject)**
enhancedsnapshots.mail.info.template.path=/templates/info.ftl            **(path to information template)**
enhancedsnapshots.mail.info.subject=Information letter                   **(information subject)**
enhancedsnapshots.mail.test.message.subject=Test subject                 **(test message subject)**
enhancedsnapshots.mail.test.message=Test message                         **(test message body)**

4) Restart tomcat at ESS tool instance (for AMI users: **sudo service tomcat8 restart**)
5) Enjoy