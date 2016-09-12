## AMI build process


#### Requirements
- When running from EC2 instance check role policies and make sure that instance has all required permissions for a build. Otherwise set `aws_access_key` and `aws_secret_key` with permissions in `snapdirector_ami.json` file.
- [Download](https://www.packer.io/downloads.html) and [Install](https://www.packer.io/intro/getting-started/setup.html)  packer version 0.10.0 or later

#### Usage
1. Download or clone enhanced-snapshots [repo](https://github.com/SungardAS/enhanced-snapshots.git).

  `` git clone https://github.com/SungardAS/enhanced-snapshots.git``
2. Go to the cloned directory:

  ``cd enhanced-snapshots ``
3. Set  `aws_access_key` and `aws_secret_key` in `snapdirector_ami.json` file if needed.
4. Change `region`, `ami_name`, `name` and `description` tags in `snapdirector_ami.json` file tags if needed.
5. There some TRAVIS env variables that are used to specify ami name and direct bucket for automation process. If you are building AMI manually - replace those variables or remove them.
6. Run build process:

  ``packer build snapdirector_ami.json``
7. In the end of the build new `ami id` will be displayed on the screen.

#### How it works
1. Main tool that is participating in the build process is [packer](https://www.packer.io/intro/).
2. All build steps are described in `snapdirector_ami.json`.
3. Packer uses [Amazon AMI Builder](https://www.packer.io/docs/builders/amazon.html) to build new AMI.
4. `Builders` section in `snapdirector_ami.json` file contains information to customize AMI (E.g. ami_name, instanse_type, user etc. ).
  - Source AMI id - `ami-60b6c60a` (Amazon Linux AMI 2015.09.1 x86_64 HVM GP2).
  - Builder type - [amazon-ebs](https://www.packer.io/docs/builders/amazon-ebs.html)

5. `Provisioners` section in `snapdirector_ami.json` file is the most important part. These steps are installing all necessary tools and components to prepare AMI. As provisioners  packer uses:
  - shell
  - ansible

  `Shell` provisioner is used to execute some light commands and ansible installation.

  `Ansible` provisioner does all other more complicate operations that are described in `ansible/ec2-playbook.yml`. (E.g. java, maven, nodejs, opendedup sdfs, tomcat, nginx, awslogs and logrotate installation)  
6. Also when `ansible` have installed all the stuff, `shell` downloads latest version of the application `enhancedsnapshots_latest.war` from `com.sungardas.releases` bucket.
7. In the end unnecessary tools are removed from ami to take it clear.

## AMI deploy
When new AMI is created and ready to work you can deploy it into all the regions using `ami_deploy.sh` script.
#### Usage
 - Make sure that you enough permission to copy ami.

 - Make script executable if it is not. `chmod +x ami_deploy.sh`

 - Use this pattern to deploy ami into the all regions
  ```
      ./ami_deploy.sh source_ami_id
  ```
### How it works

1. Deploy script has few parameters:

  `name` - set name of the new AMI that will be created.

  `ami_description` - add some description.

  `filename` - name of the output json file with regions and AMI ids .

  `ami_owner` - specify source AMI owner.

2. Script will automatically find region of the `source ami`. In other case you will receive an Error massage.
3. Also you can specify regions where to deploy. Just comment line in case if you don't need all of them.

  `But remember ami source region need to be in that list.`
```
regions=( 	#set regions where you want to deploy
	us-east-1 \
	us-west-1 \
	us-west-2 \
	eu-west-1 \
	eu-central-1   \
	ap-northeast-1 \
	ap-northeast-2 \
	ap-southeast-1 \
	ap-southeast-2 \
	sa-east-1
)
```

## Travis build process

1. Travis automatically builds application when commits or pull requests are made into `develop` and `master` branches. Also there are allowed builds for tags `beta.*` and `release.*`.
  ```
    branches:
    only:
    - master
    - develop
    - /^(beta|release).+$/
  ```
2. Travis uses java 8.
  ```
  jdk:
    - oraclejdk8
  ```
3. Travis uses encrypted AWS id and key env variables to deploy AMI.
4. Travis prepares build
  ```
  before_install:
    - sudo apt-get update
    - curl -sL https://deb.nodesource.com/setup | sudo sh
  install:
      - sudo apt-get install -y nodejs
  ```
5. Travis builds application with following commands:
  ```
    install:
      - cd './WebContent'
      - sudo npm install -g bower
      - bower install --config.interactive=false

    before_script:
      - cd '..'

    script:
      - mvn clean install
  ```
6. Travis renames artifact before deployment
  ```
  before_deploy:
    - mkdir build_artifacts
    - cp target/enhancedsnapshots*.war build_artifacts/enhancedsnapshots_latest.war
    - mv -f target/enhancedsnapshots*.war build_artifacts/enhancedsnapshots_0.0.2_${TRAVIS_BUILD_NUMBER}_${TRAVIS_COMMIT}.war
  ```

7. Deployment is allowed only after tag commit:

    `release.*` for `master` (e.g. release.3.0)

    `beta.*` for `develop` (e.g. beta.3.0)

  - For `master branch`:  
    ```
    on:
          branch: master
          tags: true
          condition: "$TRAVIS_TAG =~ ^release.+$"
    ```
     New `enhancedsnapshots_latest.war` will be deployed into `com.sungardas.releases` bucket.
     ```
     deploy:
      provider: s3
      bucket: "com.sungardas.releases"
      endpoint: "s3-us-east-1.amazonaws.com"
      skip_cleanup: true
      region: "us-east-1"
      local_dir: build_artifacts
      upload-dir: "travis-builds"
      file: "*.war"
     ```
  - For `develop branch`:
```
  on:
      branch: develop
      tags: true
      condition: "$TRAVIS_TAG =~ ^beta.+$"
```
New `enhancedsnapshots_latest.war` will be deployed into `com.sungardas.beta` bucket.
  ```
    - provider: s3
      bucket: "com.sungardas.releases.beta"
      endpoint: "s3-us-east-1.amazonaws.com"
      skip_cleanup: true
      region: "us-east-1"
      local_dir: build_artifacts
      upload-dir: "travis-builds"
    file: "*.war"
  ```
8. After build CI sends notifications to recipients.
