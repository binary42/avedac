#!/usr/bin/perl

$username = `whoami`;
chomp($username);

$results = `condor_q -const 'Owner==\"$username\" && JobStatus==5' -format "%d" ClusterId -format ".%d\t" ProcId -format \"%s\\n\" HoldReason`;

print $results;

