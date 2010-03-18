%  
% Collect feature vectors from images on directory 'image_dir'
% and save them on the output directory 'featurerootdir'. Features are
% extracted from images using the Averaged Local Jets with 3
% nonlinearities algorithm.
%
% @param image_dir where to look for images
% @param featurerootdir where to save feature vectors
% @param color_space GRAY = 1, RGB = 2, YCBCR = 3;
%
% @return filenames name of image files inside each of subdirectories of
% image_dir
% @return resolfiles resolution of images
% @return feature vectors
%
function [filenames, resolfiles, datafiles] = collect_features( image_dir, featurerootdir, color_space )

    % check number of channels
    if (n_channels ~= 1) && (n_channels ~= 3)
        fprintf(1,'Error - number of channels should be 1 or 3');
        return;
    end
    
    [subdir] = check_directory(image_dir);

    %check class directory
    if(isdir(image_dir) == 0)
       fprintf(1, 'Error - %s is not a valid directory\n', classdir);
       return
    end

    %check if output directories exists to save training data to, 
    %if not create it
    if(isdir(featurerootdir) == 0)
        mkdir(featurerootdir);   
    end    

    kill = create_kill_handle();
    
    %collect classes
    fprintf( 1, sprintf( 'Collecting features from %s\n', image_dir ) );
    [filenames, resolfiles, datafiles] = collect(kill, image_dir, subdir, featurerootdir, color_space);

end

