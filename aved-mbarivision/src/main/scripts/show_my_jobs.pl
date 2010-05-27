#!/usr/bin/perl
# Simple perl script to show the jobs by the current user

$username = `whoami`;
chomp($username);

$results = `condor_q $username`;

print $results;

