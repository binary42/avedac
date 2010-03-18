%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% Script to collect classes, given a list of training classnames
% This is intended to be run from the JNI layer and not the
% Matlab command-line. This is intended for us with the graphical
% user interface through the JNI layer and not through the Matlab
% command interface
%
% Author: dcline@mbari.org
% Date: July 2005
%
% @param kill kill boolean to break this from a GUI
% @param dbroot database root directory
% @param classalias the alias for this class
% @param classnames array of class names to use in this training class
% @param description description of this training class

function train_classes_ui(kill, dbroot, classalias, classnames, description)

%declare the training model data ris global
global RIS;

%format the training data file
trainingdir = [dbroot '/training/class/' ];

%format feature directory root for saving feature info from tests
featurerootdir = [dbroot '/features/class/'];

%create directories if they don't exist
if(isdir(trainingdir) == 0)
    mkdir(trainingdir);
end

if(isdir(featurerootdir)  == 0)
    mkdir(featurerootdir);
end

color_space = -1;

%insert the training classes matlab files names into an array
for ii=1:length(classnames)
    datafiles{ii} = [featurerootdir classnames{ii} '_data_collection_avljNL3_cl_pcsnew'];
    resolfiles{ii} = [featurerootdir classnames{ii} '_resol_collection_avljNL3_cl_pcsnew'];
    metadata = [featurerootdir classnames{ii} '_metadata_collection_avljNL3_cl_pcsnew' '.mat'];
    if(~exist([metadata]))
        error('%s does not exist', metadata);
    end
    load(metadata);
    % check if classes have the same color space
    % this tries to match the first class loaded
    if (color_space >=0 && color_space ~= class_metadata.color_space)
        fprintf(1, 'Error - color space %d for class: %s and must have color space %d\n', class_metadata.color_space, classnames{ii}, color_space);
        error('All classes must have the same color space');
    else
        color_space = class_metadata.color_space;
    end
    
end

trainingfile = [trainingdir classalias '_training_data'];

%run training
fprintf(1, 'training classes %s\n', trainingfile);
RIS = train_classes(kill, classnames, resolfiles, datafiles, trainingfile); 

%format the metadata file name and data structure
metadatafile = [trainingdir classalias '_training_metadata.mat'];
training_metadata.dbroot = dbroot;
training_metadata.classalias = classalias;
training_metadata.description = description;
training_metadata.color_space = color_space;
training_metadata.classes = classnames;

%save the metadata
fprintf(1,'saving %s\n', metadatafile);
save(metadatafile, 'training_metadata');
end
