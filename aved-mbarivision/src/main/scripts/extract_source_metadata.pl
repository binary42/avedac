######################################################################
# This script outputs a file with metadata on AVED video source input
#!/usr/bin/perl
######################################################################
#
# The Apache Software License, Version 1.1
# 
# Copyright (c) 1999-2000 The Apache Software Foundation.  All rights 
# reserved.
# 
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions
# are met:
# 
# 1. Redistributions of source code must retain the above copyright
#    notice, this list of conditions and the following disclaimer. 
# 
# 2. Redistributions in binary form must reproduce the above copyright
#    notice, this list of conditions and the following disclaimer in
#    the documentation and/or other materials provided with the
#    distribution.
# 
# 3. The end-user documentation included with the redistribution,
#    if any, must include the following acknowledgment:  
#       "This product includes software developed by the
#        Apache Software Foundation (http://www.apache.org/)."
#    Alternately, this acknowledgment may appear in the software itself,
#    if and wherever such third-party acknowledgments normally appear.
# 
# 4. The names "Xerces" and "Apache Software Foundation" must
#    not be used to endorse or promote products derived from this
#    software without prior written permission. For written 
#    permission, please contact apache\@apache.org.
# 
# 5. Products derived from this software may not be called "Apache",
#    nor may "Apache" appear in their name, without prior written
#    permission of the Apache Software Foundation.
# 
# THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
# WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
# OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
# ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
# SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
# LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
# USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
# ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
# OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
# SUCH DAMAGE.
# ====================================================================
# 
# This software consists of voluntary contributions made by many
# individuals on behalf of the Apache Software Foundation, and was
# originally based on software copyright (c) 1999, International
# Business Machines, Inc., http://www.ibm.com .  For more information
# on the Apache Software Foundation, please see
# <http://www.apache.org/>.
#
######################################################################
use strict;
use Getopt::Long;
use vars qw();
use File::Copy;
use XML::Simple;
use XML::Writer;
use Data::Dumper;
use Time::Local;
use Switch;
use POSIX;

# Add AVED_BIN to the path if it exists
if ( defined $ENV{'AVED_BIN'} ) {
    $ENV{'PATH'} = "$ENV{'AVED_BIN'}: $ENV{'PATH'};";
}

#
#
# Read and validate command line args
#

my $USAGE = <<EOU;
USAGE: $0 [-i=video.avi|mov|mpeg|] [-o=file.xml] [-u=video URL]
Options:
    -o=file.xml    Output metadata to file
    -i=video.avi   Input video source to probe
    -u=file://nanomia.shore.mbari.org/RAID/video/video.avi defines URL reference in xml output
    -p=VENTANA     Platform where video was collected - optional
    -d=1234        Dive number for video - optional
EOU
my $VERSION = q[$Id: extract_source_metadata.pl,v 1.14 2009/08/26 18:51:02 dcline Exp $];
my %OPTIONS;
my $rc = GetOptions(\%OPTIONS,
		    'o=s','i=s','p=s','d=s','u=s');
die $USAGE unless $rc;

# Initialize
my $input = $OPTIONS{i} || 0;
my $input_URL = $OPTIONS{u} || "file:$input";
my $output = $OPTIONS{o} || 0;
my $platform = $OPTIONS{p} || 0;
my $dive_number =  $OPTIONS{d} || -1;
my $owner = $ENV{'USER'};
# Format time in ISO format
my $mtime = (stat($input))[9];
my $tzoffset=POSIX::strftime("%z",gmtime($mtime));
my $fdate = POSIX::strftime("%Y-%m-%dT%H:%M:%S",gmtime($mtime));	
my $tzoffset_hr = substr $tzoffset,0,3;
my $tzoffset_min = substr $tzoffset,3,2;
my $creation_date="$fdate$tzoffset_hr:$tzoffset_min"; 
my $frame_rate = 1;
my $frame_width = 0;
my $frame_height = 0;
my $pixel_width = 1; # Assume square pixels - this may be incorrect
my $pixel_height = 1; # Assume square pixels - this may be incorrect

# If this is a compressed file, then find the first image file 
# uncompress it, then use this with tcprobe
my $image_file= "";
if (($input =~ /\.tgz$/) || ($input =~ /\.tar\.gz$/)) {
    $image_file = `tar -ztf $input | grep -i -m 1 '[A-Z0-9._%+-]*[0-9]\.[DPX|dpx|JP*G|jp*g|PPM|ppm|PNG|png]'`;
    print "gunzip < $input | tar x $image_file"; 
    `gunzip < $input | tar x $image_file`;
    $input = $image_file;
    $_ = `tcprobe -i $input`;
    `rm $input`;
}
elsif ($input =~ /\.tar$/) { 
    $image_file = `tar -tf $input | grep -i -m 1 '[A-Z0-9._%+-]*[0-9]\.[DPX|dpx|JP*G|jp*g|PPM|ppm|PNG|png]'`; 
    `tar -x -f $input $image_file`;
    $input = $image_file;
    $_ = `tcprobe -i $input`;
    `rm $input`;
}
else {
    $_ = `tcprobe -i $input`;
}

# match the format wxh after the -g flag, e.g. -g 720x480
($frame_height,$frame_width)=m/-g\s([0-9]+)x([0-9]+)/g;

print "$_\n";

# match everything between -f and the next [ character to get frame rate
($frame_rate)=m/-f\s(.*)\s\[.*/g;

# create data
my $ref = {'CreationDate'=>$creation_date,
    'FrameWidth' => $frame_width,
    'FrameHeight' => $frame_height,
    'FrameRate' => $frame_rate,
    'Owner'=> $owner, 
    'SourceIdentifier'=> "$input_URL", 
	    };
# create XML object
my $xs = XML::Simple->new(NoAttr=>0,RootName=>'SourceMetadata');

# convert Perl data ref into XML document 
$xs->XMLout($ref, KeepRoot=>1, OutputFile=>$output, XMLDecl=>"<?xml version='1.0' encoding='UTF-8'?>" );

# print out xml content for checking
print Dumper($xs->XMLout($ref));

exit 0;
