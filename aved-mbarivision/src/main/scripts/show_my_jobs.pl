#!/usr/bin/perl

$username = `whoami`;
chomp($username);

$results = `condor_q $username`;

print $results;

