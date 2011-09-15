
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% Script to collect data files to be used for training and testing
% actual classes. This is intended for us with the graphical user interface
% through the JNI layer and not through the Matlab command interface
%
% Use this in conjunction with collect_tests and collect_classes
%
% Author: Marc'Aurelio Ranzato
% Date: Dec 2003
%
% @param kill file to break this from a GUI
% @param sqdirct directory to start search
% @param pattern directory pattern to search
% @param dbroot database root directory 
% @param color_space GRAY = 1, RGB = 2, YCBCR = 3; 
% @param varsclassname varsclassname this represents 
% @param description description of this class
%Modified by doliver@mbari.org on December 5, 2004
%Modified by dcline@mbari.org on April 19, 2005 - created from
%collectclasses - factored out functions in collectclassese common to
%both collectclasses and collect_tests
%Modified by Marco Aurelio Moreira marco@mbari.org (lelinhosgp@yahoo.com.br)
%on August 21, 2009 to support 3-channel feature analysis.
%Modified by dcline@mbari.org created from collect.m modified to
%pass a select number of files, or directories for use with the
%user-interface.  
%Modified by dcline@mbari.org Jan 15, 2010 added varsclassname name and
%description
%Modified by dcline@mbari.org  Mar 25, 2010 appended color space to
%metadata name 
        
function [filenames,store] = collect_ui(kill, rawdirct, sqdirct, classname, dbroot, color_space, varsclassname, description)

GRAY = 1;
RGB = 2;
YCBCR = 3;

if ( (color_space ~= GRAY) && (color_space ~= RGB) && (color_space ~= YCBCR) )
    color_space = GRAY;
    fprintf(1, '\nWarning: Color space should be 1, 2, or 3. Converting to 1 (GRAY).\n');
end

%resolution and data files
resolfiles={};
datafiles={};

featurerootdir=[dbroot '/features/class/'];

% check if directory exists to save output to, if not create it
if(isdir(featurerootdir) == 0)
    mkdir(featurerootdir);
end

jj=0;
ii=0;
data = [];
filenames =[];
scale = 3;
sz = 0; 
 
%*modified store the file names for all jpeg or ppm images
fprintf(1,'Getting file names from directory ...\n');

if(isdir(sqdirct))
    s1 = dir([sqdirct '/*.ppm']);
    s2 = dir([sqdirct '/*.jpg']);
    s3 = dir([sqdirct '/*.jpeg']);
    
    fprintf(1,'Searching for images in %s \n', sqdirct);
    
    %*added - concatenate together
    s = [s1 s2 s3];
    
    if(size(s,1) == 0)
        error('Error %s class empty\n', classname);
    else
        sz = length(s);
    end
else
    fprintf(1,'\nError %s is not a valid directory',sqdirct);
    return
end
 
fprintf(1,'Computing invariants for %s files...',classname);


%*added comment now loop on this struct array of images
while ( ii < sz )
   
    % if kill signaled return
    if (isKill(kill))
         error('Killing collect_ui');
    end
        
    %*added  - get filename from struct array
    if(~iscell(sqdirct) & isdir(sqdirct))
        filename = [sqdirct '/' s(ii+1).name];
    else
        filename = s{1,ii+1};
    end
     
    fprintf(1,'\nCollecting %s',filename);
     
    %*modified  - read in the images
    im = imread( filename );
    
    %*added - read file info
    iminfo = imfinfo(filename);
    
    %if image is not square throw it out
    if( iminfo.Height ~= iminfo.Width )
        fprintf(1,'\n%s image not square, ignoring image', filename);
        ii = ii + 1;
        continue;
    end
    
    % Return image size
    imsize = size(im,1);
    
    % store resolution info 
    resol(jj+1,:) = imsize;
  
    if color_space > 1
            if strcmp(iminfo.ColorType,'grayscale') 
                source = im;
                im(:,:,1) = source; 
                im(:,:,2) = source; 
                im(:,:,3) = source; 
            end
            
            if (color_space == YCBCR) 
                im = rgb2ycbcr(im);
            end
             
            for kk=1:3
                data = calcola_invarianti(im(:,:,kk), scale);
                val{kk} = apply_non_lin3(data,scale);
            end
            % store feature vector (stack of feature vectors for each channel)
            store(jj+1,:) = [val{1}, val{2}, val{3}];
    else
        % COMPUTE INVARIANTS 
        im = rgb2gray(im);
        data = calcola_invarianti(im, scale); % compute invariants at different scales
        val = apply_non_lin3(data,scale); % Apply non linearity
        store(jj+1,:) = val; % store feature vector
    end
    
    %modified store name and size of current file
    if(~iscell(sqdirct) && isdir(sqdirct))
        filenames{jj+1,1} = [s(ii+1).name];
    else
        filenames{jj+1,1} = s{1,ii+1};
    end
        
    filenames{jj+1,2} = imsize;
 
    ii = ii + 1;
    jj = jj + 1;
     
end


fprintf(1, '\n');                     

if(sz > 1)
    %append the color space to the name to make it unique
    if (color_space == RGB)
        rootname = [classname '_rgb'];
    elseif (color_space == GRAY)
        rootname = [classname '_gray'];
    elseif (color_space == YCBCR)
        rootname = [classname '_ycbcr'];
    else
        rootname = classname;
    end
    
    %modified - store the data from linear 3d application
    str = [featurerootdir rootname '_data_collection_avljNL3_cl_pcsnew' '.mat'];
    fprintf(1, 'Saving %s\n', str);
    save(str,'store');
    
    %modified - store the resolution from linear 3d application
    str = [featurerootdir rootname '_resol_collection_avljNL3_cl_pcsnew' '.mat'];
    fprintf(1, 'Saving %s\n', str);
    save(str,'resol');
    
    %modified - store name of class files from the directory
    str = [featurerootdir rootname '_names_collection_avljNL3_cl_pcsnew' '.mat'];
    fprintf(1, 'Saving %s\n', str);
    save(str,'filenames');
    
    str = [featurerootdir rootname '_metadata_collection_avljNL3_cl_pcsnew' '.mat'];
    fprintf(1, 'Saving %s\n', str);
    class_metadata.raw_directory = rawdirct;
    class_metadata.square_directory = sqdirct;
    class_metadata.classname = classname;
    class_metadata.dbroot = dbroot;
    class_metadata.varsclassname = varsclassname;
    class_metadata.description = description; 
    class_metadata.color_space = color_space; 
    save(str,'class_metadata'); 
            
end

end

