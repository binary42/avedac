function [recfiles, storeprob, classindex, probtable] = test_ljmNL3 (kill, classes, filenames, cltd, cltr, ris, threshold)

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% [CM,recfiles,storprob] = test_ljmNL3 (cl, indx, ris)
% Date 29 Dec. 2003
% Description: test using averaged local jet features with 3rd non
%   linearity. The classification is done taking the class with maximum
%   likelihood (hypothesis of equally probable classes)
% Use:  cl classes to consider, for ex. cl=1:12
%       indx data structure with images to be tested, for ex.
%           indx{1}=500:545; etc.
%       ris models of training
%       CM confusion matrix
%       recfiles junk files
%       storeprob maximum probablity
%       probtable each row is the probability for each class
% Author: Marc'Aurelio Ranzato
%
% modified doliver@mbari.org - Dec 10, 2004
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% 

% use this function if you have trained your data and now you want to test
% a class (ie a test class) against your training sample

% add classes from a saved variable removed pat line
% resolution statistics
resolution = ris.resolution;
NDS = 10;            % nr. of standard deviations 

%determine number of classes in the training set
ncltr = length(classes);

%determine the number of test classes
nclts = length(cltr);

CMttls = zeros(ncltr+1,1);
mog = ris.mog;
mediatot = ris.mediatot;
FLD = ris.FLD;
 

% name of the classes from training and test set and new matrix to store
% file names
recfiles(1,:) = [{'Unknown'} classes];

% MAIN LOOP: OUT -> class, IN -> index 
fprintf(1,'TEST \n')

probtable.data = zeros(nclts, ncltr);
storeprob = zeros(nclts,1); 
classindex = zeros(nclts,1);

% number of training classes
for jj=1:ncltr
    
    % number of test classes
    for ii=1:nclts
        
        % if kill signaled return 
        if (isKill(kill))
             error('Killing test_ljmNL3');
        end

        val = cltd(ii,:);
        risol = cltr(ii);

        % store the current file in variable
        currentfile = filenames(ii);
        probtable.files(ii,1) = currentfile;
 
        % update status in console 
        a = [classes{jj}];
        
        fprintf(1,'testing %d of %d for class: %s\r', ii, nclts, a ); 
             
        val = val - mediatot;
        val = (FLD' * val')'; 
         
        for jjj= 1:ncltr
            dprobab(jjj) = gmmprob( mog(jjj).mix, val );
        end
  
        if sum(dprobab) == 0
            Mp = 0;
            estimate = 0;
            storeprob(ii) = Mp;
        else
            sum(dprobab);
            prob = dprobab/sum(dprobab);
            Mp = max(prob);
            
            % store the maximum probability for a file
            storeprob(ii) = Mp;
            estimate = find(prob == Mp);
            if(size(estimate,2) > 0)
                estimate = estimate(1);
            else
                estimate = 0;
            end
        end

       fprintf(1,'max probability: %f probability estimate: %f for class: %s \r', threshold, Mp, a );
            
       probtable.data(ii,:) = dprobab;
        
        %if the risol is out of bounds with estimated class resolution and
        %is less than the minimum probability threshold, then store this in
        %the unknown class
        if  ( estimate == 0 | ... % prob = 0 for all classes
                ((risol >= resolution(estimate,1) + NDS*resolution(estimate,2) | ... % resolution too large
                risol <= resolution(estimate,1) - NDS*resolution(estimate,2)) |... % resolution too low
                Mp <= threshold )) % maximum prob below treshold
            % update the unknown class if the threshold not met
            % store the file name in unknown column 
            recfiles(ii+1,1) = currentfile;
            classindex(ii) = 1;
        else
            % update the sum of files found in class and save name of file
            recfiles(ii+1,estimate+1) = currentfile;
            classindex(ii) = estimate+1;
        end
    end
    
end
