
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
% @param predictedclassname predictedclassname this represents 
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
%Modified by dcline@mbari.org Jan 15, 2010 added predictedclassname name and
%description
%Modified by dcline@mbari.org  Mar 25, 2010 appended color space to
%metadata name 
        
function [filenames] = collect_ui(kill, rawdirct, sqdirct, classname, dbroot, color_space, predictedclassname, description)

GRAY = 1;
RGB = 2;
YCBCR = 3;

if ( (color_space ~= GRAY) && (color_space ~= RGB) && (color_space ~= YCBCR) )
    color_space = GRAY;
    fprintf(1, '\nWarning: Color space should be 1, 2, or 3. Converting to 1 (GRAY).\n');
end
 
featurerootdir=[dbroot '/features/class/'];

% check if directory exists to save output to, if not create it
if(isdir(featurerootdir) == 0)
    mkdir(featurerootdir);
end

ii=0; 
id = '-';
scale = 3; 
filenames = '';
savemotion = false;
numPts = 24; %number of contour points per segment
segments = 8;
C = [];
ppat = '';
yP = {};
xP = {};  
areas = {};
vals = {}; 
store = [];
resol = [];
  
%*modified store the file names for all jpeg or ppm images
fprintf(1,'Getting file names from directory ...\n');
     
% Initialize empty V
for j = 1: segments*2 - 1
    C = [C zeros(2,numPts)];
end
 
if ~isempty(regexp(upper(classname), '/UNK|OTHER|JUNK|UNKNOWN/','match'))
    savemotion = false;
end

if(isdir(sqdirct))
    s1 = dir([sqdirct '/*.ppm']);
    s2 = dir([sqdirct '/*.jpg']);
    s3 = dir([sqdirct '/*.jpeg']);
    
    fprintf(1,'Searching for images in %s \n', sqdirct);
    
    %*added - concatenate together
    s = [s1 s2 s3];
    
    if(size(s,1) == 0)
        error('Error %s class empty\n', classname); 
    end
else
    fprintf(1,'\nError %s is not a valid directory',sqdirct);
    return
end
 
fprintf(1,'Computing invariants for %s files...',classname);

ttl = length(s); 

%*added  - get filename from struct array
filename = [sqdirct '/' s(1).name];

%find ending index of event identifier _evt
m = regexp(filename, '\.*?evt[0-9]+\.*?','match');

%get event id string - this assumes only one match
id = m{:};
newid = id; 

%*added comment now loop on this struct array of images
while ( ii < ttl )
   
          
        % if kill signaled return
        if (isKill(kill))
            error('Killing collect');
        end
        
        %*added  - get filename from struct array
        filename = [sqdirct '/' s(ii+1).name];
        
        % update status in console
        fprintf(1,'\nCollecting %d of %d %s', ii + 1, ttl, filename );
        
        %*modified  - read in the image
        im = imread( filename );
        
        %*added - get the event identifier
        %find ending index of event identifier evt
        m = regexp(filename, '\.*?evt[0-9]+\.*?','match');
        
        %get event id string - this assumes only one match
        newid = m{:};
        
        if ~strncmp(id,newid, length(id)) 
            C = [];
            % Initialize empty V
            for j = 1: segments*2 - 1
                C = [C zeros(2,numPts)];
            end
        end
        
        %found new id
        if ~strncmp(id,newid, length(id))
            
            if savemotion
                
                % update status in console            
                fprintf(1,'\n Calculating motion for filename: %s', filename );
            
                % to plot the 3D quiver plot
                x=cell2mat(xP');
                y=cell2mat(yP');
                
                %calculate mean velocity and concatenate to matrix
                A = sqrt(abs(sum([gradient(x).^2 gradient(y).^2],2)))';
                vabs = A';
                
                %minimum points needed for mfcc
                if length(vabs) > 8
                    aabs = gradient(vabs);
                    a = [cell2mat(vals') ones(length(vals),1)*mean(vabs) ones(length(vals),1)*mean(aabs)];
                else
                    aabs = gradient(vabs);
                    a = [cell2mat(vals')  ones(length(vals),1)*mean(vabs) ones(length(vals),1)*mean(aabs)];
                end
                
            else
                a = [cell2mat(vals') zeros(length(vals),2)];
            end
            
            store = vertcat(store, a);
            
            areas = {};
            vals = {};
            yP = {};
            xP = {};
            id = newid;
        end
        
        %*added - read file info
        iminfo = imfinfo(filename);
        
        %if image is not square throw it out
        if( iminfo.Height ~= iminfo.Width )
            fprintf(1,'\n%s image %d of %d not square - image will be excluded', filename, ii, ttl);
            ii = ii + 1;
            continue;
        end
        
        % store resolution info
        info = pnmimpnminfo(filename);
        maxArea = 0;
        xPos = 0;
        yPos = 0;
        xTransposePos = 0; %current unused; placeholder only
        yTransposePos = 0; %current unused; placeholder only
        
        if ~isempty(info) && ~isempty(info.Comment)
            [maxArea xPos yPos xTransposePos yTransposePos] = strread(info.Comment);
        end
        
        areas{end+1} = maxArea;
        yP{end+1} = yPos;
        xP{end+1} = xPos;
        
        resol(ii+1,:) = size(im,1);
        
        filenames{ii+1,1} = [s(ii+1).name];
        
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
            vals{end+1} = double([val{1}, val{2}, val{3}]);
        else
            % COMPUTE INVARIANTS
            im = rgb2gray(im);
            data = calcola_invarianti(im, scale); % compute invariants at different scales
            val = apply_non_lin3(data,scale); % Apply non linearity
            vals{end+1} = double(val); % store feature vector
        end 
    
    ii = ii + 1;   
    
    %if last index
    if  ii == ttl
        
        if savemotion
            
            % update status in console
            fprintf(1,'\n Saving motion for filename: %s', filename );
            
            x=cell2mat(xP');
            y=cell2mat(yP');
            
            %calculate mean velocity and concatenate to matrix
            A = sqrt(abs(sum([gradient(x).^2 gradient(y).^2],2)))';
            vabs = A';
            if length(vabs) > 8
                %vabs = interp(vabs,3);
                aabs = gradient(vabs);
                a = [cell2mat(vals') ones(length(vals),1)*mean(vabs) ones(length(vals),1)*mean(aabs)];
                
            else
                aabs = gradient(vabs);
                a = [cell2mat(vals')  ones(length(vals),1)*mean(vabs) ones(length(vals),1)*mean(aabs)];
            end
            
        else
            a = [cell2mat(vals') zeros(length(vals),2)];
        end
        
        store = vertcat(store, a);
    end
end
     

fprintf(1, '\n');                     

if(ttl > 0)
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
    d = [featurerootdir rootname '_data_collection_avljNL3_cl_pcsnew.mat'];
    r = [featurerootdir rootname '_resol_collection_avljNL3_cl_pcsnew.mat']; 
    n = [featurerootdir rootname '_names_collection_avljNL3_cl_pcsnew.mat']; 
    m = [featurerootdir rootname '_metadata_collection_avljNL3_cl_pcsnew.mat'];
    
    fprintf(1, '\nSaving to %s', d);
    save(d,'store');
    
    fprintf(1, '\nSaving to %s', r);
    save(r,'resol'); 
    
    fprintf(1, '\nSaving to %s', n);
    save(n, 'filenames');     
    
    class_metadata.raw_directory = rawdirct;
    class_metadata.square_directory = sqdirct;
    class_metadata.classname = classname;
    class_metadata.dbroot = dbroot;
    class_metadata.predictedclassname = predictedclassname;
    class_metadata.description = description; 
    class_metadata.color_space = color_space;
    
    fprintf(1, '\nSaving %s', m); 
    save(m,'class_metadata'); 
            
end
            
end

