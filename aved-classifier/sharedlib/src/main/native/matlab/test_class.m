
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% Script to test a class.
% 
% @param kill kill boolean to break this from a GUI
% @param dbroot database root directory 
% @param testclassname test class name 
% @param trainingalias name of the training library 
% @param threshold the probability threshold between 0-1.0
%        
function [testfiles, finalclassindex, finalstoreprob] = test_class(kill, dbroot, testclassname, trainingalias, threshold, colorspace)
        
GRAY = 1;
RGB = 2;
YCBCR = 3;

%test for correct arguments
if(size(testclassname,1) > 1)
    error('Error - argument %s invalid - can only test one class at a time', testclassname);    
end

%append the color space to the name to make it unique
if (colorspace == RGB)
    rootname = [testclassname '_rgb'];
elseif (colorspace == GRAY)
    rootname = [testclassname '_gray'];
elseif (colorspace == YCBCR)
    rootname = [testclassname '_ycbcr'];
else
    rootname = testclassname;
end
        
fprintf(1,'TESTING STARTING...\n')     

% the test class file - this should already be collected
data = [rootname '_data_collection_avljNL3_cl_pcsnew'];
testclassdata  = [dbroot '/features/class/' data '.mat'];
names = [rootname '_names_collection_avljNL3_cl_pcsnew'];
testclassfiles  = [dbroot '/features/class/' names '.mat']; 
resol = [rootname '_resol_collection_avljNL3_cl_pcsnew'];
testclassresol  = [dbroot '/features/class/' resol '.mat']; 

% load and test class data
if(~exist(testclassdata, 'file'))
    error('Error - file %s does not exist', testclassdata);
end
if(~exist(testclassfiles, 'file'))
    error('Error -  file %s does not exist', testclassfiles);
end
if(~exist(testclassresol, 'file'))
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

ttlfiles = size(filenames,1); 

%calculate random indexes using the length of the test class
randomindex = randperm(ttlfiles);

%if there is enough to test, 
%pick 10% of random index to use for testing
if(ttlfiles > 10)
    numtest = round(ttlfiles*0.10);
else
    numtest = ttlfiles;
end
    
indxtst = randomindex(1:numtest); 
%initialize the classifier with the training classes
trd = [dbroot '/training/class/' trainingalias '_training_data' '.mat'];
trcl = [dbroot '/training/class/' trainingalias '_training_data_cls' '.mat'];

% input parameters
if (~exist(trd, 'file'))
    %%
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
[recfiles, storeprob, classindex, probtable] = test_ljmNL3(kill, classnames, tcf(indxtst,:), tcd(indxtst,:), tcr(indxtst,:), ris, threshold);
 
finalclassindex = num2cell(classindex);
finalstoreprob = num2cell(storeprob) ;
testfiles = tcf(indxtst,1);

fprintf(1,'TESTING DONE...\n')     


% save the results as the same name as the test class and color space appended
basedir = [dbroot '/features/tests/'];

%check if  directories exist to save test data to, 
%if not create it
if(isdir(basedir) == 0)
    mkdir(basedir);   
end

% save the results as the same name as the test class and color space appended
saveresultsname = [basedir rootname];

%modified - calculate the accuracy of the test and display the results
%storage for results and for continued calculation of junk files
class_table_header = [{'Unknown'}, classnames];

% determines, outputs and saves class totals, correct detections and
% detections for each class

fprintf(1, '\nTotal classes: %d\n', length(class_table_header));
fprintf(1, 'Total files tested: %d\n', numtest);

%if in the class we are testing put the total in the last row
%otherwise, the total is zero because we are only testing within the
%class
numclasses = length(class_table_header);
for jj = 1:numclasses 
    if(strcmp(class_table_header{jj}, testclassname))
        stats(numclasses + 1, jj)=length(classindex);
    else
        stats(numclasses + 1, jj)=0;
    end
end
 
%create a nxn matrix and capture detections
for ii = 1:length(class_table_header)
   
     for jj = 1:length(class_table_header)   
        if (ii==jj)
            stats(ii,jj) = sum(cellfun('length',recfiles(2:end,ii)) > 0);
        end
    end
end
% if kill signaled return
if (isKill(kill))
    error('Killing test_class');
end
        
statsfinal(1,:)=class_table_header;
for ii = 1:length(class_table_header) + 1
    for jj = 1:length(class_table_header)
        statsfinal(ii+1,jj) = {stats(ii,jj)};
    end
end

fprintf(1,'Saving event classifier results to %s\n', [saveresultsname '-CM.mat']);
save([saveresultsname '-CM.mat'], 'statsfinal');

fprintf(1,'Saving results to %s\n', [saveresultsname '.mat']);
save([saveresultsname '.mat'],'names');

fprintf(1,'Saving event classifier results to %s\n', [saveresultsname '-storeprob.mat']);
save([saveresultsname '-storeprob.mat'], 'storeprob');

fprintf(1,'Saving event classifier results to %s\n', [saveresultsname '-recfiles.mat']);
save([saveresultsname '-recfiles.mat'], 'recfiles');
         
end
