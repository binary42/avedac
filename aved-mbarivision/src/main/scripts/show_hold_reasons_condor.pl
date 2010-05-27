#!/usr/bin/perl
# Simple perl script to show the hold reasons for all held condor jobs
# by the current user
$username = `whoami`;
chomp($username);

$results = `condor_q -const 'Owner==\"$username\" && JobStatus==5' -format "%d" ClusterId -format ".%d\t" ProcId -format \"%s\\n\" HoldReason`;

print $results;

