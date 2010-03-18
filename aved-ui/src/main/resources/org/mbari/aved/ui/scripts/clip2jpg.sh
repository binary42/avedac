#!/bin/bash
#set -x
# Copyright (c) MBARI 2007
# Author: D. Cline
# Date: April 28, 2007 - Created
#
#exit on error; this will exit this bash script when any command exists with
#a non-zero exit code
set -e
SUCCESS=0
FAILURE=-1
export PATH=$PATH:/opt/local/bin:/usr/local/bin
###################################################################################
# Print usage
print_usage()
{
  echo "  "
  echo "  "
  echo -e "USAGE:  clip2jpg [OPTIONS] -i filename.AVI|MOV|MPG -o jpg_output_directory"
  echo "  "
  echo "  "
  echo "  "
  echo "OPTION"
  echo "  "
  echo -e "\033[1m -s \033[0m"
  echo "     output jpg directory is defined relative to local machine users scratch directory"
  echo "      (Example:  clip2jpg -s -i filename.AVI -o mytest/output )"
  echo "  "
  echo -e "\033[1m -t \033[0m"
  echo "     options to pass to the transcoder - these must work with transcode binary"
  echo "      (Example:  clip2jpg -s -t '-J smartdeinter=diffmode=2:highq=1:cubic=1,xsharpen=strength=25' -i filename.AVI -o mytest/output )"
  echo "  "
  echo "  "
}
frame_number=0
##################################################################################
isdigit ()    # Tests whether *entire string* is numerical.
{             # In other words, tests for integer variable.
  [ $# -eq 1 ] || return $FAILURE

  case $1 in
      [!0-9]*) return $FAILURE;;
           *) return $SUCCESS;;
  esac
}
###################################################################################
# Frame number from 29.97 frame/s drop frame timecode, assume 0 frames
#
# Usage: calculate_2997dropframes hours minutes seconds
#  
#  e.g. calculate_2997dropframes 10 12 10
###################################################################################
calculate_2997dropframes()
{
    hours=$1
    minutes=$2
    seconds=$3    
    frame=0
    total_minutes=$(echo "scale=0; 60*$hours + $minutes" | bc)
    b=$(echo "scale=0; ($total_minutes - $total_minutes/10)*2" | bc)
    frame_number=$(echo "scale=0; (108000*$hours + 1800*$minutes + 30*$seconds  + $frame) - $b" | bc)
}
###################################################################################
# Frame number from timecode, assume 0 frames
#
# Usage: calculate_frames hours minutes seconds
#  
#  e.g. calculate_frames 10 12 10
###################################################################################
calculate_frames()
{
    rate=$1
    hours=$2
    minutes=$3
    seconds=$4    
    frame=0

    frame_number=$(echo "scale=0;(3600*$hours + 60*$minutes + $seconds)*$rate + $frame" | bc)
}
###################################################################################
# Determines what method to use to convert timecode to a counter based on frame rate
###################################################################################
timecode2counter()
{   
    rate=$1
    tc=$2
    hours=`echo $tc | cut -c 1-2`
    minutes=`echo $tc | cut -c 3-4`
    seconds=`echo $tc | cut -c 5-6`
    case $rate in
	29.97 )calculate_2997dropframes $hours $minutes $seconds ;;
	* ) calculate_frames $rate $hours $minutes $seconds ;;
    esac
}
###################################################################################
rmall()
{
   tmp=./tmp.$RANDOM
   echo "find ./ -type f -name '$1' -exec rm {} \;" > $tmp 
   chmod u+x $tmp
   $tmp
   rm -f $tmp
}
###################################################################################
# Initialize variables
input=0
output=0
use_condor_scratch=0
use_system_scratch=0
has_timecode=0
#transcode_opts='-c 0-100'
transcode_opts=
codec=""
extra_args=
time_code=

# Check arguments
while getopts i:o:t:s option 
do
  case $option in   
   i)  input="$OPTARG";;
   s)  use_system_scratch=1;;
   t)  transcode_opts="$OPTARG";;
   o)  output="$OPTARG";;
   *)  echo "Unimplemented option chosen."
       echo "  "
       print_usage;;
  esac
done

if [ $input == 0 -o $output == 0 ]
then print_usage
exit 1
fi

# Add the path to the aved binaries if AVED_BIN is set
if [ $AVED_BIN ]; then
    PATH=$PATH:$AVED_BIN
    export PATH
fi

if [ $_CONDOR_SCRATCH_DIR ]; then
    PATH=$PATH:$_CONDOR_SCRATCH_DIR
    export PATH
fi

# Format the output directory
if [ $use_system_scratch = 1 ]; then
    if [ $SCRATCH_DIR -a $USER ]; then
	outputdir=$SCRATCH_DIR/$USER/$output
    else
	echo "Error \$SCRATCH_DIR or \$USER environment variable not defined and -s option chosen in clip2jpg script"
	exit 1
    fi
else  
    outputdir=$output
fi

# Create output directory if it doesn't exist
# If it exists and it is in scratch, then clean it first
if [ -d $outputdir -a $use_system_scratch = 1 ]; then
     # Change into output directory
    pushd $outputdir
    echo $PWD
    echo "Found $outputdir, removing previous results in directory"
    rmall "*"
    popd
else
    echo "Executing mkdir -p $outputdir"
    mkdir -p $outputdir
    chmod a+rwx $outputdir    
fi

# Format the absolute name of the input file
basefile=$(basename $input)
filestem=${basefile%.*} 
D=$(dirname $input)
abspath=`cd $D && pwd `

# Format the full path to the file
input=$abspath/$basefile

# Now do the work in the output directory
pushd $outputdir

# Initialize the file seed
outputfileseed=f

# Remove any files the file seed and extension
#echo "Removing files with the file stem $outputfileseed*.jpg"
#rmall "$outputfileseed*.jpg"

# Check if this file has a ISO8601 timecode timestamp and extract
# timestamp from the name. This is a crude test so far that only
# checks if there is a set of numbers appended with a T
a=`echo $filestem | grep "T" | cut -f1 -d "T"`
if isdigit $a
then
    time_code=`echo $filestem | cut -f2 -d "T" `
    has_timecode=1
fi

# Run transcode or scripts needed to convert to jpgs
case $input in
    *.avi ) 
	if which avidump &>/dev/null; then 
	   if [ $has_timecode = 1 ]; then
	      tmp=`avidump $input | grep "Video frame rate"`
	      rate=`echo $tmp | cut -f2 -d ":" | cut -f2 -d " "`
	      timecode2counter $rate $time_code
	      extra_args="-f $rate -start_timecode $frame_number"
	   fi
	  tmp=`avidump $input | grep compressor`
	  codec=`echo $tmp | cut -f2 -d ":" | cut -f2 -d " "`
	fi
	case "$codec" in
	    "DX50" )  	    
		cmd="transcode -q 0 -i $input -o $outputfileseed -x ffmpeg,null -y jpg,null $extra_args $transcode_opts"		     
		;;
	    "mpg2" )
		cmd="transcode -q 0 -i $input -o $outputfileseed -x mpeg2,null -y jpg,null $extra_args $transcode_opts"
		;;
	    * ) 	       	    	
		cmd="transcode -i $input -o $outputfileseed -y jpg,null $extra_args $transcode_opts"
		;;
	esac
	echo "Executing $cmd"
	$cmd
	;;
    *.mov )  delimx="x"
	tmp=./tmp.$RANDOM
	if which oqtinfo &>/dev/null; then
	    oqtinfo $input > $tmp
	    if [ $has_timecode = 1 ]; then  
	    # This assumes the rate is constant.
            # Get the rate from the the timecode track if it exists
	    # If the rate is not valid, then get the rate from the video track
		if `grep "timecode track" $tmp` ; then
		    r=`grep "timecode scale" $tmp | cut -f2,3 -d " " | cut -f2 -d " "`
		    rate=$(echo "scale=2; $r/100" | bc)
		else
		    r=`grep "Rate:" $tmp | cut -f6 -d " "`   
		    rate=$(echo "scale=2; $r" | bc)
		fi	    
		timecode2counter $rate $time_code	    
		extra_args="-f $rate -start_timecode $frame_number"
	    fi
	fi
	if which qtinfo &>/dev/null; then
	    qtinfo $input > $tmp
	    # Search for 3-letter code, e.g. [8BPS] in the output
	    codec=`grep 'compressor' $tmp | cut -d " " -f 6 | cut -d "." -f 1`;
	    rm $tmp
	fi
	case "$codec" in
	    "mp4v" )
		cmd="transcode -q 0 -i $input -o $outputfileseed -y jpg,null $extra_args $transcode_opts";;
	    * )
		  # If the import-oqtmov.so library is found, use it in case there is a timecode track 
		if ls -1 `tcmodinfo -p`/import_oqtmov.so >/dev/null ; then 
		    cmd="transcode -q 0 -i $input -o $outputfileseed -x oqtmov,null -y jpg,null $extra_args $transcode_opts"
		else
		    cmd="transcode -q 0 -i $input -o $outputfileseed -V rgb24 -x mov,null -y jpg,null $extra_args $transcode_opts"
		fi;;
	esac
	echo "Executing $cmd"
	$cmd
	;;
    *.mpeg | *.mpg )  
	cmd="transcode -q 0  -i $input -x mpeg2,null -y jpg,null -o $outputfileseed $extra_args $transcode_opts"
	echo "Executing $cmd"
	$cmd;;
    * ) 
	cmd="transcode -q 0 -i $input -o $outputfileseed -y jpg,null $extra_args $transcode_opts" 
	echo "Executing $cmd"
	$cmd;;
esac

popd

exit 0

