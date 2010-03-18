######################################################################
# Test script to extract AVED video frame rate from source metadata xml file
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
# use blib;
use XML::Xerces;
use Getopt::Long;
use vars qw();
use File::Copy;
# use module
use XML::Simple;
use XML::Writer;
use Data::Dumper;
use Switch;

$ENV{'PATH'}='/home/aved/bin:/usr/local/bin:/usr/bin:/bin';

#
# Read and validate command line args
#

my $USAGE = <<EOU;
USAGE: $0 [-i=file.xml]
Options:
    -i=file.xml    AVED input metadata to file
EOU
my $VERSION = q[$Id: extract_source_framerate.pl,v 1.2 2007/11/27 00:38:28 dcline Exp $];
my %OPTIONS;
my $rc = GetOptions(\%OPTIONS,
		    'i=s');
die $USAGE unless $rc;

# Initialize
my $input = $OPTIONS{i};
my $owner = $ENV{'USER'};

my $xs = new XML::Simple(KeepRoot=>1,searchpath=>".",forcearray=>1,suppressempty=>0);
my $ref = $xs->XMLin($input);

#If hash value exists and is non-zero then simply print out the rate, otherwise print out a default rate
if ( exists ($ref->{SourceMetadata}->[0]->{FrameRate}->[0]) && $ref->{SourceMetadata}->[0]->{FrameRate}->[0] != 0 ) {	    
    print "$ref->{SourceMetadata}->[0]->{FrameRate}->[0] \n";
}
else {
    print "29.97\n";
}
exit 0;
