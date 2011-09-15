function ris = training_ljmNL3 (kill, classfiles, resolfiles, datafiles, cl, indx, N_GAUSS, n_dimensions, covar_type)

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% ris = training_ljmNL3 (cl, indx, N_GAUSS, n_dimensions, scale, covar_type)
%
% Date: 29 Dec 2003
% Description: training using averaged local jet features with third non
%   linearity. For each pixel a set of invariants are extracted and
%   each of these invariants is splitted in positive negative and absolute
%   value (with respect to the background mean). The averaged values over
%   all image pixel are the feature vector. After dimension reduction
%   obtained thanks to FLD a MoG is estimated for all the data of each
%   class.
% USE:  cl classes to consider, ex. cl=1:12;
%       indx indexes of images to consider (the
%           same for all classes), i.e. it=1; indx{1}=0:499; etc.
%       N_GAUSS nr. of gaussians in MoG
%       n_dimensions nr. of dimensions in the final feature space
%       covar_type define covariance matrix structure in MoG, ex. covar_type = 'full'
%       ris variable storing the training models
% Author: Marc'Aurelio Ranzato
%
% modified doliver@mbari.org - Dec 05, 2004
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

%use this class to train a training set
fprintf(1,'TRAINING \n')

pat = classfiles(1,:);

n_tot_samp = 0;
for cc = 1:length(cl)
    n_samples(cc) = length(indx{cc});
    n_tot_samp = n_tot_samp + n_samples(cc);
end

% n_scales * n_invariants * n_nonlinearities * n_channels
storet = []; %zeros(n_tot_samp,(scale + 1)*9*3);

% *modified - total iterations
MAX_N_ITER = 1000;  % see gmmem
% Initial condition for randn and rand
%%%load('performance_SEPT/BestInitCondSEPTSim.mat')
%%%s = stateo;

lc = length(cl);

for cc = 1:lc   % Load data
    %keyboard
    
    % modified - load data saved from classes using different directory
    load([datafiles{cc} '.mat']);
    %storet(index2 + 1: index2 + n_samples(cc),:) = store(indx{cc}+1,:);
    storet = [storet; store(indx{cc}+1,:)];
    load([resolfiles{cc} '.mat']);
    resolution(cc,1) = mean(resol(indx{cc}+1));
    resolution(cc,2) = std(resol(indx{cc}+1));
    %index2 = index2 + n_samples( cc );
    
end


%%% F L D %%%
fprintf(1,'Computing FLD \n')
mediatot = mean(storet);
storet = storet - repmat(mediatot,size(storet,1),1);
FLD = fisher_l_d( storet', n_dimensions, n_samples );
storet = (FLD'*storet')';   % on the 1st row the projection along FLD of 1st image features

fprintf(1,'Computing MoG \n');
% estimate MoG
index2 = 0;
for jj = 1:lc
    % if kill signaled return 
    if (isKill(kill))
        error('Killing training_ljmNL3');
    end
    
    st = storet(index2 + 1: index2 + n_samples(jj),:);
    
    fprintf(1,'computing MoG-%u \n', cl(jj));
    randn('state',0);
    %%randn('state',sum(100*clock))
    %%s{1}{jj} = randn('state');
    %%%randn('state',s{1}{jj});
    mix = gmm(size(st,2), N_GAUSS{jj}, covar_type{jj});
    option = zeros(1,14);
    option(1) = -1;
    option(3) = 1;
    option(5) = 1;
    option(14) = MAX_N_ITER;
    %%rand('state',0);
    %%randn('state',0);
    %%rand('state',sum(100*clock))
    %%s{2}{jj} = rand('state');
    %%randn('state',sum(100*clock))
    %%s{3}{jj} = randn('state');
    %%%rand('state',s{2}{jj});
    %%%randn('state',s{3}{jj});
     
    mix = gmminit(mix, st, option);
    mix = gmmem(mix, st, option);
    mog(jj).mix = mix;
    index2 = index2 + n_samples(jj);
    
end

%%ris.state =  s;
ris.mog = mog;
ris.FLD = FLD;
ris.mediatot = mediatot;
ris.resolution = resolution;

