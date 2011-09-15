%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% Test a class against the training set and check the results.  Use
% collect_tests to collect data on test cases.
%
% doliver@mbari.org Dec 15, 2004.
%
% I
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% if more than method, specify as follows to ensure the method is parsed
% as a string, not a cell array
% method1='majority';
% method2='probability';
% methods=[{method1} {method2}];
function [probtable] = run_tests(trainingdir, testingdir, saveresultsname, methods)    

    %check class and test directories before doing anything
    if(isdir(trainingdir) == 0)
     fprintf(1, 'Error - %s is not a valid directory\n', trainingdir);
     return;
    end

    if(isdir(testingdir) == 0)
     fprintf(1, 'Error - %s is not a valid directory\n', testingdir);
     return;
    end

    trainclass=[];
    testclass=[];
    testfilenames=[];

    %modified - load the results from the class and test directories, then
    %assign variables for class and test names and for indexes
    classmfiles = find_mfile([trainingdir '/features/class/'], '_names_collection_avljNL3_cl_pcsnew');
    if(size(classmfiles,1) == 0)
      fprintf(1,'Error - no class names in %s', [trainingdir '/features/class']);
      return;
    end
    load(classmfiles{1}); 
    
    %if more than one test file, load the first found
    %TODO: change this to load all those found, or give user options to 
    %deselect
    trainclass = filenames(1,:);

    testmfiles = find_mfile([testingdir '/features/tests/'], '*_names_*');
    if(size(testmfiles,1) == 0)
      fprintf(1,'Error - no class names in %s', [testingdir '/features/class']);
      return;
    end
    load(testmfiles{1}); %if more than one test file, load the first found
    %TODO: change this to load all those found, or give user options to 
    %deselect
    testclass = filenames(1,:);

    cltr = 1:length(trainclass);
    clts = 1:length(testclass);

    %modified - determine number of files 
    ncltr = length(cltr);
    nclts = length(clts);
    ncl = ncltr + nclts;

    % assign parameters for testing
    Thres = 0.9;

    indxts = [];

    % modified determine sizes of all the test classes
    sizes = sum(~cellfun('isempty',filenames))-1;

    %modified assign indices for the entire length of a test class
    indxts{length(sizes)} = 0:sizes(end) - 1;

    trainingset = find_mfile([trainingdir '/training/training'], '*');
    load(trainingset{1}); %if more than one training set found, use the first one
    %TODO: change this to load all those found, or give user options to 
    %deselect

    % TEST ERROR
    % modified - call the test function and save the results    
    [C, names, probablilityindex, classindex, probtable] = test_ljmNL3(testingdir, filenames, trainclass, cltr, clts, indxts, ris, Thres);
    CMts = C;    

    %modified - calculate the accuracy of the test and display the results
    %storage for results and for continued calculation of junk files
    class_table_header = [{'Unknown'}, trainclass];

    % determines, outputs and saves class totals, correct detections and
    % detections for each class    
    fprintf(1, '\nTotal classes: %d\n', length(class_table_header));
    fprintf(1, 'Total files tested: %d\n', size(filenames,1)-1);

    numclasses = length(class_table_header);
    stats=zeros(numclasses, numclasses);

    %create a nxn matrix and capture detections
    for ii = 1:length(class_table_header)    
        for jj = 1:length(class_table_header)
            strlen = length(class_table_header{jj});
            stats(ii,jj) = sum(strncmp(class_table_header{jj}, names(:,ii), strlen));
            if (ii==jj)
                stats(ii,jj) = stats(ii,jj)-1;
            end
        end	     
    end

    for ii = 1:length(class_table_header)    
        fprintf(1, '\nclass %s: \ttotal detections: %d   correct detections: %d\n', class_table_header{ii}, CMts(ii), stats(ii,ii));
        for jj = 1:length(class_table_header)        
            fprintf(1, '%s found in class %s = %d\n', class_table_header{jj}, class_table_header{ii}, stats(ii,jj));
        end	     
    end

    fprintf(1,'Saving results to %s\n', [saveresultsname '.mat']);
    save([saveresultsname '.mat'],'names');

    %if last argument specified, run event_classifier with specified method(s)
    %multiple methods run when more than one method specified in space
    %delimited list
    for k = 1:length(methods)
        method = methods{k};
        event_classifier_results = event_classifier(method, class_table_header, classindex, probablilityindex, filenames);
        format long
        display(event_classifier_results);

        fprintf(1,'Saving event classifier results to %s\n', [saveresultsname '-' method '.mat']);
        save([saveresultsname '-' method '.mat'], 'event_classifier_results');
    end

end % end of function
    
