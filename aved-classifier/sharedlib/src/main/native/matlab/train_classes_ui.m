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
% @param color_space GRAY = 1, RGB = 2, YCBCR = 3
% @param classalias the alias for this class
% @param classnames array of class names to use in this training class
% @param description description of this training class

function train_classes_ui(kill, dbroot, color_space, classalias, classnames, description)

GRAY = 1;
RGB = 2;
YCBCR = 3;

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


%insert the training classes matlab files names into an array
for ii=1:length(classnames)
    
    %append the color space to the name to make it unique
    if (color_space == RGB)
        rootname = [classnames{ii} '_rgb'];
    elseif (color_space == GRAY)
        rootname = [classnames{ii} '_gray'];
    elseif (color_space == YCBCR)
        rootname = [classnames{ii} '_ycbcr'];
    else
        rootname = classnames{ii};
    end
    
    datafiles{ii} = [featurerootdir rootname '_data_collection_avljNL3_cl_pcsnew'];
    resolfiles{ii} = [featurerootdir rootname '_resol_collection_avljNL3_cl_pcsnew'];
    metadata = [featurerootdir rootname '_metadata_collection_avljNL3_cl_pcsnew' '.mat'];
    if(~exist([metadata]))
        error('%s does not exist', metadata);
    end
    load(metadata);
end

trainingfile = [trainingdir classalias '_training_data'];

%run training
fprintf(1, 'training classes %s\n', trainingfile);
train_classes(kill, classnames, resolfiles, datafiles, trainingfile); 

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
