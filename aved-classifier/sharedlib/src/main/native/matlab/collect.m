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

function testmfiles = collect(kill, dirct, pattern, dbroot, color_space)

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
    scale = 3; 

    %resolution, data and test files
    resolfiles={};
    datafiles={};
    testmfiles={};

    % check if directory exists to save output to, if not create it
    if(isdir(dbroot) == 0)
        mkdir(dbroot);   
    end

    %initialize string to concatenate class names to
    str = '';
    basedir = '';
    
    % MAIN LOOP: OUT -> class, IN -> index
    for jj = 1 : lc
        
        % if kill signaled return 
        if (isKill(kill))
            error('Killing collect');
        end
        
        ii = 0;
        store = [];
        resol = [];
        ppat = '';
        filenames = '';
        
        %*modified store the file names for all jpeg or ppm images
        if(length(pat) > 1)
            ppat = [ pat{cl(jj)} '/' ];
            str = [str '-' pat{jj}];
        else
            ppat = '';
            str = tmpfiles{1,:}';
        end
        
        filenames{1,1} = [str]; 
         
        s1 = dir([dirct '/' ppat '*.ppm']);    
        s2 = dir([dirct '/' ppat '*.jpg']);    
        s3 = dir([dirct '/' ppat '*.jpeg']);    

        %*added - concatenate together all the image files found
        s = [s1 s2 s3];

        if(~isempty(ppat))
            fprintf(1,'Collecting %s',ppat);
        else            
            fprintf(1,'Collecting %s',dirct);
        end
        
        %set the basedirectory to the same
        %as the search directory pattern
        if(length(pat) > 1)
           basedir = [ dirct '/' pat{cl(jj)} ];
        else
           basedir = [ dirct ];
        end
        
        ttl = length(s);
        %*added comment now loop on this struct array of images
        while ( ii < ttl ) 
            
            % if kill signaled return
            if (isKill(kill))
                error('Killing collect');
            end
            
            
            %*added  - get filename from struct array
            filename = [basedir '/' s(ii+1).name];          
            
            %*modified  - read in the images
            im = imread( filename );        

            %*added - read file info         
            iminfo = imfinfo(filename);

            %if image is not square throw it out
            if( iminfo.Height ~= iminfo.Width )
                fprintf(1,'\n%s image not square - image will be excluded', filename);
                ii = ii + 1;
                continue;
            end
            
            filenames{end + 1,1} = [s(ii+1).name];
            
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
                store(ii+1,:) = [val{1}, val{2}, val{3}];
            else
                % COMPUTE INVARIANTS
                im = rgb2gray(im);
                data = calcola_invarianti(im, scale); % compute invariants at different scales
                val = apply_non_lin3(data,scale); % Apply non linearity
                store(ii+1,:) = val; % store feature vector
            end

            % store resolution info 
            resol(ii+1) = size(im,1); 
            
            ii = ii + 1;        

            % update status in console 
            fprintf(1,'Collecting %d of %d\r', ii, ttl );
            
        end
        
        %modified - store the resolution, data and file names from linear 3d application
        if (size(filenames,1) > 1)
            
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
            
            d=[dbroot rootname '_data_collection_avljNL3_cl_pcsnew' ];
            r=[dbroot rootname '_resol_collection_avljNL3_cl_pcsnew'];
            n=[dbroot rootname '_names_collection_avljNL3_cl_pcsnew'] ;
            
            fprintf(1, '\nSaving collection to %s', d);
            save(d,'store');
            
            fprintf(1, '\nSaving collection to %s', r);
            save(r,'resol'); 
                        
            fprintf(1, '\nSaving collection to %s', n);
            save(n, 'filenames');
            
            resolfiles{end+1} = r;
            datafiles{end+1} = d; 
            %n{end+1} = [n '.mat'];
        end
        
        fprintf(1,'\n');

    end  
end

