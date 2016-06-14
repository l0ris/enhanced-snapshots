#!/bin/bash
#Use this pattern to deploy ami into regions
# ./ami_deploy.sh source_ami_id

regions=( 	#set regions where you want to deploy
	us-east-1 \
	us-west-1 \
	us-west-2 \
	eu-west-1 \
	eu-central-1	\
	ap-northeast-1 \
	ap-northeast-2 \
	ap-southeast-1 \
	ap-southeast-2 \
	sa-east-1
)
name="SungardAS-AMI-Amazon-Linux-x86_64-HVM-Enhanced-Snapshots-2016.05.13"	#set name of the new AMI
ami_description="Enhanced Snapshots "						#set ami description
filename=latest.json								#set output file name
ami_owner="self" #Set ami owner: "self", or you can specify "AWS account ID"

#Check for the "AMI ID" parameter
if [ -z $1 ]; then
	echo "No parameter. Set source AMI ID. Please, use following pattern:"
	echo $'\n ./ami_deploy.sh source_ami_id \n'
	exit 1
else
	echo "Source AMI ID = $1"
fi

#Check AMI's region
for i in ${regions[@]}; do
	check_region=$(aws ec2 describe-images --region $i --owners "$ami_owner" |grep "ImageId" |grep $1)
	if [ -n "$check_region" ]; then
		source_region=$i
	fi
done

#Set source region if found
if [ -n "$source_region" ]; then
	echo "Source region = $source_region"
else
	echo "Cant find specified AMI ID in all regions. Check AMI ID"
	exit 1
fi

#Info message
echo "--------------------------"
echo "{" >$filename
for i in ${regions[@]}; do
#Skip if source region
        if [ "$i" == "$source_region" ]
        then
                continue
        fi
#Copy AMI, parse and output into file
        aws ec2 copy-image --source-image-id $1 --source-region  "$source_region" --region $i --name $name  --description "$ami_description" |grep ImageId |sed "s/ImageId/$i/g" |sed 's/:/:{"ami":/g' |sed 's/$/},/g' >>$filename
done

#Add source region and AMI id into file
echo "    \"$source_region\":{\"ami\": \"$1\"}" >>$filename
echo "}" >>$filename

#Result output
echo "Use \"cat $filename\" to get new ami IDs"
cat $filename
