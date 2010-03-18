######################################################################
#!/usr/bin/perl -w
######################################################################
# @(#) randomize Effectively _unsort_ a text file into random order.
# 96.02.26 / drl.
# Based on Programming Perl, p 245, "Selecting random element ..."

# Set the random seed, PP, p 188
srand(time|$$);

# Suck in everything in the file.
@a = <>;

# Get random lines, write 'em out, mark 'em done.
while ( @a ) {
$choice = splice(@a, rand @a, 1);
print $choice;
}

exit 0;
