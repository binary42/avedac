#!/bin/bash
set -x
E_ERR=2

# Print usage
print_usage()
{
  echo "  "
  echo "  "
  echo -e "\033[1m USAGE:  build.sh [OPTION] -p <password> \033[0m"
  echo "  "
  echo "  "
  echo "OPTION"
  echo -e "\033[1m -p \033[0m"
  echo "      Password for access to iLab saliency code"
  echo "  "
}

# Check arguments
args=`getopt -o p: -- "$@" `
if test $? != 0; then
    print_usage
    exit $E_ERR
fi

password=""

eval set -- "$args" 
for i
do
  case $i in  
   -p)  shift;password="$1";shift;;   
  esac
done

if [ "$password" == "" ]; then 
    print_usage
    exit $E_ERR
fi
	

eval "sed 's/SALIENCY_SVN_PASSWORD/$password/g' Dockerfile.template > Dockerfile"
docker build -t danellecline/avedac .
