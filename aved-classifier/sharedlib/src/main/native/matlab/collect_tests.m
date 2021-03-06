%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% Script to collect data of all test classes - Averaged LJ NL 3 
%
% Author: Marc'Aurelio Ranzato
% Date: Dec 2003
%
% @param kill kill boolean to break this from a GUI
%Modified by doliver@mbari.org on December 5, 2004 
%modified directory path which stores test files

function testmfiles = collect_tests(kill, testdir, dbroot, color_space)

    if(isdir(testdir) == 0)
        fprintf(1,'Error - %s is not a valid directory\n', testdir);
        return;
    end

    %check directory and format subdirectory names
    [subdir, subdirstr] = check_directory(testdir);

    %format feature directory root for saving feature info from tests
    featurerootdir = [dbroot '/features/tests/'];

    %check if directory exists to save training data to, 
    if(isdir(featurerootdir) == 0)
        mkdir(featurerootdir);   
    end

    %collect classes
    fprintf(1, 'Collecting test file features\n');
    if(~isempty(subdir))
        testmfiles = collect(kill, testdir, subdir, featurerootdir, color_space);
    else
        testmfiles = collect(kill, testdir, '', featurerootdir, color_space);
    end
    
end % end of function collect_tests
