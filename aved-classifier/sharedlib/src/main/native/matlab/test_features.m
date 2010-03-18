%
% The function test_features returns the most probable class for each
% sample in features.
%
% @param features local jets with nonlinearities features. Each row is one
% different sample
% @param class_model mog, features mean and FLD matrix
% @param Thresh Threshold
%
% @return result each row is of the form <class number> <class1 prob>
% <class2 prob> ... <classn prob>. If the normalized probability (sum(prob)
% = 1) of the most probable class is not at least Thresh, then 
% class number is 0 (unknown).
%
function [result] = test_features(features, class_model, Thresh)

    mog = class_model.mog; % mixture of gaussians
    mediatot = class_model.mediatot; % mean of features
    FLD = class_model.FLD; % Fisher Linear Discriminant
    
    ncltr = length(mog);
    
    n_tests = size(features, 1);
    
    result = zeros(n_tests, ncltr+1);    
    
    for ii=1:n_tests
         
        val = features(ii,:) - mediatot;               
        val = (FLD' * val')';
        
        dprobab = zeros(ncltr, 1);
        for jjj= 1:ncltr            
            dprobab(jjj) = gmmprob( mog(jjj).mix, val );            
        end
       
        result(ii, 2:end) = dprobab;
        
        if sum(dprobab) == 0
            Mp = 0;
            estimate = 0;                       
        else
            prob = dprobab / sum(dprobab);
            Mp = max(prob);            
            
            estimate = find(prob == Mp);
            if(size(estimate,2) > 0)
                estimate = estimate(1);
            else
                estimate = 0;
            end
        end      
        
        %if less than the minimum probability threshold, then store this in
        %the unknown class
        if  ( estimate == 0 | Mp <= Thresh ) % maximum probability below treshold          
            result(ii, 1) = 0; 
        else
            result(ii, 1) = estimate;             
        end
	           
    end   
  
end % end of function