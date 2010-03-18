%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% Script to read class image file, calculate invariant and non-linearity
% and return calculated value and image size. 
%
% Author: dcline@mbari.org
% Date: July 2005
%

function [data, imsize] = compute_invariant(filename)

%initialize variables
val = 0;
scale = 3;
       
%read in the image
ima = imread(filename);
        
%read file info         
%iminfo = imfinfo(filename);                
% if image is too small - throw it out
%if ( iminfo.Height < 24 )
%   fprintf(1,'\n%s too small to classify, ignoring image', filename); 
%   ii = ii + 1;
%   continue;
%end               
        
% COMPUTE INVARIANTS AT DIFFERENT SCALES
i = calcola_invarianti(ima, scale);
        
% Apply non linearity                        
data = apply_non_lin3(i,scale);

% Return image size
imsize = size(ima,1);
        
end