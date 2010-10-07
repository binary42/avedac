######################################################################
# Script to extract video frame rate from source metadata xml file
#!/usr/bin/perl
######################################################################
use Getopt::Long;
use vars qw();
use File::Copy;

# use module
use XML::Simple;
use XML::Writer;
use Data::Dumper;
use Switch;

$ENV{'PATH'}='/home/aved/bin:/usr/local/bin:/usr/bin:/bin';

# Read and validate command line args
my $USAGE = <<EOU;
USAGE: $0 [-i=file.xml]
Options:
    -i=file.xml    AVED input metadata to file
EOU
my $VERSION = q[$Id: extract_source_framerate.pl,v 1.3 2010/04/22 20:55:40 dcline Exp $];
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
