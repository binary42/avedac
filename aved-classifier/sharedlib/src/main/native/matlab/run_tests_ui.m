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
% method3='maximum';
% methods=[{method1} {method2} {method3}];
function [eventids, majoritywinnerindex, probabilitywinnerindex, maxwinnerindex, probability] = run_tests_ui(kill, dbroot, color_space, testclassname, trainingalias, threshold)
         
GRAY = 1;
RGB = 2;
YCBCR = 3;

%test for correct arguments
if(size(testclassname,1) > 1)
    error('Error - argument %s invalid - can only test one class at a time', testclassname);
end

fprintf(1,'TESTING %s STARTING...\n', testclassname)

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

% the test files - these should already by collected 
d  = [dbroot '/features/tests/' rootname '_data_collection_avljNL3_cl_pcsnew.mat'];
n = [dbroot '/features/tests/' rootname '_names_collection_avljNL3_cl_pcsnew.mat']; 
r = [dbroot '/features/tests/' rootname '_resol_collection_avljNL3_cl_pcsnew.mat']; 

%load and test class data
if(~exist(d, 'file'))
    error('Error - file %s does not exist', d);
end
if(~exist(n,'file'))
    error('Error -  file %s does not exist', n);
end
if(~exist(r,'file'))
    error('Error -  file %s does not exist', r);
end

fprintf(1,'Loading %s\n',d);
load(d);
tcd = store;

fprintf(1,'Loading %s\n',r);
load(r);
tcr = resol;

fprintf(1,'Loading %s\n',n);
load(n);
tcf = filenames;

ttlfiles = size(filenames,1)-1;

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
method3='maximum';
methods=[{method1} {method2} {method3}];
 

%run event_classifier with specified methods
for k = 1:length(methods)
    method = methods{k};
    
    if(~isempty(filenames))
        [recfiles, event_classifier_results] = event_classifier(method, class_table_header, classindex, storeprob, filenames);
        
        format long
        display(event_classifier_results);
        
        switch lower(method)
            case {'maximum','max'}
                % skip over the first few table column fields - these
                % aren't needed
                maxwinnerindex =  event_classifier_results(3:end,3) ;
                eventids = event_classifier_results(3:end,1) ;
                probability = event_classifier_results(3:end,4) ;
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
        winners=[event_classifier_results{3:end,3}];
        
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

