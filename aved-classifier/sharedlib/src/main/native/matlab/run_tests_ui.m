%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% Test a class against the training set and check the results.  Use
% collect_tests to collect data on test cases.
%
% doliver@mbari.org Dec 15, 2004.
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% if more than method, specify as follows to ensure the method is parsed
% as a string, not a cell array
% method1='majority';
% method2='probability';
% methods=[{method1} {method2}];
function [eventids, majoritywinnerindex, probabilitywinnerindex, probability] = run_tests_ui(kill, dbroot, color_space, testclassname, trainingalias, threshold)
 
GRAY = 1;
RGB = 2;
YCBCR = 3;

%test for correct arguments
if(size(testclassname,1) > 1)
    error('Error - argument %s invalid - can only test one class at a time', testclassname);
end

fprintf(1,'TESTING %s STARTING...\n', testclassname)

%append the color space to the name to make it unique
if (color_space == RGB)
    rootname = [testclassname '_rgb'];
elseif (color_space == GRAY)
    rootname = [testclassname '_gray'];
elseif (color_space == YCBCR)
    rootname = [testclassname '_ycbcr'];
else
    rootname = testclassname;
end

% the test files - this should already by collected
data = [rootname '_data_collection_avljNL3_cl_pcsnew'];
testclassdata  = [dbroot '/features/tests/' data '.mat'];
names = [rootname '_names_collection_avljNL3_cl_pcsnew'];
testclassfiles  = [dbroot '/features/tests/' names '.mat'];
resol = [rootname '_resol_collection_avljNL3_cl_pcsnew'];
testclassresol  = [dbroot '/features/tests/' resol '.mat'];

%load and test class data
if(~exist(testclassdata, 'file'))
    error('Error - file %s does not exist', testclassdata);
end
if(~exist(testclassfiles,'file'))
    error('Error -  file %s does not exist', testclassfiles);
end
if(~exist(testclassresol,'file'))
    error('Error -  file %s does not exist', testclassresol);
end

fprintf(1,'Loading %s\n',testclassdata);
load(testclassdata);
tcd = store;

fprintf(1,'Loading %s\n',testclassresol);
load(testclassresol);
tcr = resol;

fprintf(1,'Loading %s\n',testclassfiles);
load(testclassfiles);
tcf = filenames;

ttlfiles = size(filenames,1)-1;

%initialize the classifier with the training classes
trd = [dbroot '/training/class/' trainingalias '_training_data.mat'];
trcl = [dbroot '/training/class/' trainingalias '_training_data_cls.mat'];

% input parameters
if (~exist(trd, 'file'))
    error('%s does not exist\n', trd);
end
if (~exist(trcl, 'file'))
    error('%s does not exist\n', trcl);
end
fprintf(1, 'Loading training class data in %s\n', trd);
load(trd);
fprintf(1, 'Loading training classes in %s\n', trcl);
load(trcl);

%test classes aganst training classes
[recfiles, storeprob, classindex, probtable] = test_ljmNL3(kill, classnames, tcf, tcd, tcr, ris, threshold);

fprintf(1,'TESTING DONE...\n') 

% save the results as the same name as the test class appended with the
% trainingalias
saveresultsname = [testclassname '-' trainingalias];

%modified - calculate the accuracy of the test and display the results
%storage for results and for continued calculation of junk files
class_table_header = [{'Unknown'}, classnames];

% determines, outputs and saves class totals, correct detections and
% detections for each class

fprintf(1, '\nTotal classes: %d\n', length(class_table_header));
fprintf(1, 'Total files tested: %d\n', ttlfiles);

numclasses = length(class_table_header);
stats=zeros(numclasses + 1, numclasses);

%create a nxn matrix and capture detections
for ii = 1:length(class_table_header)
    % if kill signaled return 
        if (isKill(kill))
             error('Killing run_tests_ui');
        end
        
    %record the total events that were actually labeled as a given class
    stats(numclasses + 1, ii)=sum(classindex == ii);
    %create the confusion matrix
    for jj = 1:length(class_table_header)
        strlen = length(class_table_header{jj});
        stats(ii,jj) = sum(strncmp(class_table_header{jj}, recfiles(:,ii), strlen));
        if (ii==jj)
            stats(ii,jj) = stats(ii,jj)-1;
        end
    end
end

statsfinal(1,:)=class_table_header;
for ii = 1:length(class_table_header) + 1
    for jj = 1:length(class_table_header)
        statsfinal(ii+1,jj) = {stats(ii,jj)};
    end
end

% run all methods by default
method1='majority';
method2='probability';
methods=[{method1} {method2}];
 

%run event_classifier with specified methods
for k = 1:length(methods)
    method = methods{k};
    
    if(~isempty(filenames))
        [recfiles, event_classifier_results] = event_classifier(method, class_table_header, classindex, storeprob, filenames);
        
        format long
        display(event_classifier_results);
        
        switch lower(method)
            case {'majority','major'}
                % skip over the first few table column fields - these
                % aren't needed
                majoritywinnerindex =  event_classifier_results(3:end,3) ;
                eventids = event_classifier_results(3:end,1) ;
            case {'probability', 'prob'}
                probability = event_classifier_results(3:end,6) ;
                probabilitywinnerindex =  event_classifier_results(3:end,3) ;
                eventids = event_classifier_results(3:end,1) ;
            otherwise
                disp('Unknown method.')
                return;
        end
        
        %modified - calculate the accuracy of the test and display the results
        %storage for results and for continued calculation of junk files
        class_table_header = [{'Unknown'}, classnames];
        
        % determines, outputs and saves class totals, correct detections and
        % detections for each class
        fprintf(1, '\nTotal classes: %d\n', length(class_table_header));
        fprintf(1, 'Total files tested: %d\n', ttlfiles);
        
        numclasses = length(class_table_header);
        stats=zeros(numclasses + 1, numclasses);
        winners=[event_classifier_results{3:end,3}]
        
        %create a nxn matrix and capture detections
        for ii = 1:length(class_table_header)
            %record the total events
            stats(numclasses + 1, ii)=sum(winners == ii);
            for jj = 1:length(class_table_header)
                strlen = length(class_table_header{jj});
                stats(ii,jj) = sum(strncmp(class_table_header{jj}, recfiles(:,ii), strlen));
                if (ii==jj)
                    stats(ii,jj) = stats(ii,jj)-1;
                end
            end
        end
        
        statsfinal(1,:)=class_table_header;
        for ii = 1:length(class_table_header) + 1
            for jj = 1:length(class_table_header)
                statsfinal(ii+1,jj) = {stats(ii,jj)};
            end
        end        
        
    end
end
end % end of function

