#=======================Parameters check=================
if [ -z $1 ]; then
	echo "No parameter. Set alias name. Please, use following pattern:"
	echo $'\n ./pem_to_jks.sh alias_name password \n'
	exit 1
else
	echo "Alias name = $1"
fi

if [ -z $2 ]; then
        echo "No parameter. Set password. Please, use following pattern:"
        echo $'\n ./pem_to_jks.sh alias_name password \n'
        exit 1
else
        echo "Password = $2"
fi

#====================Variables==========================
alias=$1
password=$2
pem=$3
jks=$4

if [ -z $pem ]; then
        echo "There is no *.pem file in $pem_dir"
        exit 1
else
        echo "pem = $pem "
fi
pkcs=$(echo "$pem" |sed "s/pem/pkcs12/")

#====================Body===================================
openssl pkcs12 -export -out $pkcs -in $pem -passin pass:$password  -passout pass:$password

#keytool -genkey -keyalg RSA -alias $alias -keystore keystore.ks -dname "CN=Unknown, OU=Java, O=Oracle, L=Unknown, S=Unknown, C=U" -keypass $password -storepass $password
#keytool -delete -alias $alias -keystore keystore.ks -keypass $password  -storepass $password

keytool -v -importkeystore -srckeystore $pkcs -srcstoretype PKCS12 -destkeystore $jks -deststoretype JKS -srcstorepass $password -keypass $password -storepass $password
keytool -changealias -alias 1 -destalias $alias -keypass $password -keystore $jks -storepass $password