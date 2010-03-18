%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% Script to collect data files to be used for training actual classes
% Use this in conjunction with collect_tests to collect test data files.
%
% Author: Marc'Aurelio Ranzato
% Date: Dec 2003
%
% Modified by doliver@mbari.org on December 5, 2004

function build_classes(classdir, dbroot, color_space)
    
    %format direcotories to save training and feature data
    trainingrootdir = [dbroot '/training/'];
    featurerootdir = [dbroot '/features/class/'];

    [subdir, subdirstr] = check_directory(classdir);

    %check class directory
    if(isdir(classdir) == 0)
       fprintf(1, 'Error - %s is not a valid directory\n', classdir);
       return
    end

    %check if output directories exists to save training data to, 
    %if not create it
    if(isdir(trainingrootdir) == 0)
        mkdir(trainingrootdir);   
    end

    if(isdir(featurerootdir) == 0)
        mkdir(featurerootdir);   
    end

    kill = create_kill_handle();
    
    %format class data file
    if(isempty(subdirstr) == 0)
        classdatafile=[trainingrootdir 'trainingset' subdirstr '.mat'];
    else 
        a=regexp(classdir,'\.*?/\.*?','start');
        classname = classdir(a(end)+1:length(classdir))
        classdatafile=[trainingrootdir 'trainingset' classname '.mat'];    
    end

    %collect classes
    fprintf(1, 'Collecting training file features\n');
    [c, rfiles, dfiles] = collect(kill, classdir, subdir, featurerootdir, color_space);

    %train and save classes
    fprintf(1, 'Building training classes\n');
    train_classes(c, rfiles, dfiles, classdatafile);

end
