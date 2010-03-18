%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% Script to check directories for storing training and test data files
% Use this with build_classes and collect_classes
%
% Author: Danelle Cline
% Date: Apil 2005
%

function [subdir, subdirstr] = check_directories(directory)

%subdirectories within directory
%the code below will pull out all subdirectories
a = dir(directory);

subdirstr=[];
subdir={};

%first two listings of dir() are . and .., 
%if more than these listings - this directory might have
%subdirectories to read in so get list of directories and copy to pat
if(size(a,1) > 2)
    for j = 3 : size(a,1)
        if(a(j).isdir == 1)
            subdirstr = [subdirstr '-' a(j).name];
            subdir(1,end+1) = {a(j).name};
        end
    end    

end
