%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% Class to train the collected data sets.  No stats performed to verify
% accuracy of training class.  Assumption - already have good training sample.
% Simplifies process by only doing the necessary training routine but no
% testing to speed up the processing.  What is used after the training set
% has been verified to be good sampling.  Use collect_classes.  Training
% results are saved.
%
% @param kill kill boolean to break this from a GUI
%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

% input parameters
function ris = train_classes(kill, classnames, resolfiles, datafiles, trainingdatafile)

%modified variable to store class names, number of classes and index
classestr = classnames(1,:);
cltr = 1:length(classestr);
ncltr = length(cltr); 
filemetadata = []; 

%initialize class data to pass to training function
for cc = 1 : ncltr
    
    N_GAUSS{cc} = 4;
    %covar_type{cc} = 'spherical';
    covar_type{cc} = 'full';
    
    f1=[ resolfiles{cc} '.mat'];
    
    if(exist(f1,'file'))
        fprintf(1,'Loading %s\n', f1);
        load(f1);
        filemetadata{cc} = resol;
    else
        fprintf(1, 'Cannot find %s\n', f1);
        error('Class not built - run build_class first');
    end
end

n_dimensions = 3;
%n_dimensions = 8;
%n_dimensions = 12;

% modified determine size of each class and create index for it
sizes = sum(~cellfun('isempty',classnames))-1;

for i = 1:ncltr
    indxtr{i} = 0:length(filemetadata{i})-1;
end

% modified TRAINING - using new function
ris = training_ljmNL3 (kill, classnames, resolfiles, datafiles, cltr, indxtr, N_GAUSS, n_dimensions, covar_type);

% modified save training results
fprintf(1,'Saving %s\n',[trainingdatafile '.mat']);
save (trainingdatafile, 'ris');

fprintf(1,'Saving %s\n',[trainingdatafile '_cls.mat']);
save ([trainingdatafile '_cls.mat'], 'classnames');
end
