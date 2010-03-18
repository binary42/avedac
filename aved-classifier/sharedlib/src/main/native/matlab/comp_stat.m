function [mbgi, stdbgi] = comp_stat ( ima )


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% function In = comp_stat ( I )
% 
% It computes mean and variance of the bg looking at the border of the
% image.
%
% USE:  ima input image 
%       mbgi mean of bg
%       stdbgi standard deviation of bg
%
% EXAMPLE:  >> 
%           
%
% DATE: 16 Dec 2003
%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

    x=min(10,round(size(ima,1)/2));
    y=x-1;
    if(isempty(ima) == 0)
        B = [ima(1:x,:)'];
        B = [B; ima(end - y:end,:)'];
        B = [B; ima(x + 1:end - x,1:x)];
        B = [B; ima(x + 1:end - x,end - y:end)];
        mbgi = mean(B(:));
        stdbgi = std(B(:));
    else
        B = 0;
        mbgi = 0.;
        stdbgi = 0.;
    end    
    
end

