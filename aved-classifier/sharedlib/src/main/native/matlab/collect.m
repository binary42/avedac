%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% Script to collect data files to be used for training and testing actual classes
%
% Use this in conjunction with collect_tests and collect_classes
%
% Author: Marc'Aurelio Ranzato
% Date: Dec 2003
%
% @param kill kill boolean to break this from a GUI
% @param dirct directory to start search
% @param pattern directory pattern to search
% @param dbroot database root directory
% @param color_space GRAY = 1, RGB = 2, YCBCR = 3
%
%Modified by doliver@mbari.org on December 5, 2004
%Modified by dcline@mbari.org on April 19, 2005 - created from
%collectclasses - factored out functions in collectclassese common to
%both collectclasses and collect_tests
%Modified by Marco Aurelio Moreira marco@mbari.org (lelinhosgp@yahoo.com.br)
%on August 21, 2009 to support 3-channel feature analysis.
%Modified by dcline@mbari.org  Mar 25, 2010 appended color space to
%metadata name

function [filenames, resolfiles, datafiles] = collect(kill, dirct, pattern, dbroot, color_space)

GRAY = 1;
RGB = 2;
YCBCR = 3;

if ( (color_space ~= GRAY) && (color_space ~= RGB) && (color_space ~= YCBCR) )
    color_space = GRAY;
    fprintf(1, '\nWarning: Color space should be 1, 2, or 3. Converting to 1.\n');
end

% if pattern not defined, default to reading all subdirectories within the
% specified direct
if( ~isempty(pattern) )
    pat = pattern;
    tmpfiles(1,:) = pat(:);
    a=regexp(dirct,'\.*?/\.*?','start');
    str = dirct(a(end)+1:length(dirct));
else
    pat={''};
    %if no pattern defined, name this class the parent directory name
    a=regexp(dirct,'\.*?/\.*?','start');
    str = dirct(a(end)+1:length(dirct));
    tmpfiles(1,:) = {str(:)};
end

%number of classes
lc = length(pat);
cl = 1 : lc;

%scale for local jets
scale = 3;

%resolution, data and test files
resolfiles = {};
datafiles = {};
classnames = {};
imageclass = {};
namefiles = {};

% check if directory exists to save output to, if not create it
if(isdir(dbroot) == 0)
    mkdir(dbroot);
end

%initialize string to concatenate class names to an event identifier
str = '';
basedir = '';

for jj = 1 : lc
    
    % if kill signaled return
    if (isKill(kill))
        error('Killing collect');
    end
    
    ii = 0;
    mm = 0;
    ppat = '';
    id = '-';
    filenames = '';
    store = [];
    resol = [];
    Mi = {};
    Ii = {};
    beta = 0.75;
    areas = {};
    vals = {};
    yP = {};
    xP = {};
    p0 = 0; v0 = 0;
    init = false;
    ttlImages = 0;
    savemotion = true;
    numPts = 24; %number of contour points per segment
    segments = 8;
    display = 1;
    nc = 12; %number of cepstral coefficients excluding 0'th coefficient (default 12)
    w = 'M';
    C = [];
    
    % Initialize empty V
    for j = 1: segments*2 - 1
        C = [C zeros(2,numPts)];
    end
    
    %*modified store the file names for all jpeg or ppm images
    if(length(pat) > 1)
        ppat = [ pat{cl(jj)} '/' ];
        str = pat{jj};
    else
        ppat = '';
        str = tmpfiles{1,:}';
    end
    
    classname = str;
    filenames{1,1} = [str];
    
    if ~isempty(regexp(upper(classname), '/UNK|OTHER|JUNK|UNKNOWN/','match'))
        savemotion = false;
    end
    
    s1 = dir([dirct '/' ppat '*.ppm']);
    s2 = dir([dirct '/' ppat '*.jpg']);
    s3 = dir([dirct '/' ppat '*.jpeg']);
    
    %*added - concatenate together all the image files found
    s = [s1 s2 s3];
    
    if(~isempty(ppat))
        fprintf(1,'\nCollecting %s',ppat);
    else
        fprintf(1,'\nCollecting %s',dirct);
    end
    
    %set the basedirectory to the same
    %as the search directory pattern
    if(length(pat) > 1)
        basedir = [ dirct '/' pat{cl(jj)} ];
    else
        basedir = [ dirct ];
    end
    
    ttl = length(s);
    
    %added index for image class
    imageclass{end + 1} = jj * ones(1,ttl);
    
    %*added  - get filename from struct array
    filename = s(1).name;
    
    %find ending index of event identifier _evt
    m = regexp(filename, '\.*?evt[0-9]+\.*?','match');
    
    %get event id string - this assumes only one match
    id = m{:};
    newid = id;
    areas = {};
    vals = {};
    yP = {};
    xP = {};
    
    %*added comment now loop on this struct array of images
    while ( ii < ttl )
        
        % if kill signaled return
        if (isKill(kill))
            error('Killing collect');
        end
        
        %*added  - get filename from struct array
        filename = [basedir '/' s(ii+1).name];
        
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
            init = false;
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
            fprintf(1,'\n%s image %d of %d not square - image will be excluded', ii, ttl, filename);
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
        
        %if ~isempty(info) && ~isempty(info.Comment)
        %    [maxArea xPos yPos xTransposePos yTransposePos] = strread(info.Comment);
        %end
        
        areas{end+1} = maxArea;
        yP{end+1} = yPos;
        xP{end+1} = xPos;
        
        resol(mm+1,:) = size(im,1);
        
        filenames{mm+1,1} = [s(ii+1).name];
        
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
    mm = mm + 1;
    
    %if last index
    if  ii == ttl
        
        if savemotion
            
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

%modified - store the resolution, data and file names from linear 3d application
if (size(filenames,1) > 0)
    
    %append the color space to the name to make it unique
    if (color_space == RGB)
        rootname = [str '_rgb'];
    elseif (color_space == GRAY)
        rootname = [str '_gray'];
    elseif (color_space == YCBCR)
        rootname = [str '_ycbcr'];
    else
        rootname = str;
    end
    
    d=[dbroot rootname '_data_collection_avljNL3_cl_pcsnew.mat'];
    r=[dbroot rootname '_resol_collection_avljNL3_cl_pcsnew.mat'];
    n=[dbroot rootname '_names_collection_avljNL3_cl_pcsnew.mat'];     
    
    fprintf(1, 'Saving to %s\n', d);
    save(d,'store');
    
    fprintf(1, 'Saving to %s\n', r);
    save(r,'resol');
    
    fprintf(1, 'Saving to %s\n', n);
    save(n, 'filenames');
    
    resolfiles{end+1} = r;
    datafiles{end+1} = d;
    filenames{end+1} = n;  
end

fprintf(1,'\n');

end

