%
% Reduces the dimensionality of feature vectors to n_dimensions using Fisher
% Linear Discriminant and then estimates a mixture of gaussians (MoG) model
% for each class.
%
% @param class_data each row of class_data{i} is a feature vector for one
% sample of class #i
% @param n_gauss number of gaussians to be used to model the discriminants
% distribution
% @param n_dimensions the original feature vector is reduced to
% n_dimensions dimensions
% @param covar_type type of covariance matrices ('spherical', 'diag',
% 'full' or 'ppca'). See gmm.
% @return ris classes model
%         ris.mog mixture of gaussians
%         ris.FLD matrix to reduce data dimensionality
%         ris.mediatot value to be subtracted to features before reducing
%         dimensionality
% @return all_features projection of data in class_data into the new
% n_dimensions-space
%
function [ris, all_features] = build_mog(class_data, n_gauss, n_dimensions, covar_type)

    fprintf(1,'TRAINING \n');    
    n_classes = length(class_data);   
    
    n_samples = zeros(n_classes,1);
    for cc = 1:n_classes
        n_samples(cc) = size(class_data{cc},1);
    end
    sample_indices = [0; cumsum(n_samples)];
    n_tot_samp = sum(n_samples);

    feature_size = size(class_data{1}, 2);
    
    % each row is a the feature vector for one sample image
    all_features = zeros(n_tot_samp, feature_size);

    % *modified - total iterations
    MAX_N_ITER = 1000;  % see gmmem    

    for cc = 1:n_classes   
        all_features(sample_indices(cc)+1:sample_indices(cc+1),:) = class_data{cc};
    end

    %%% F L D %%%
    fprintf(1,'Computing FLD \n')
    mediatot = mean( all_features ); % mean for each feature component
    all_features = all_features - repmat(mediatot,size(all_features,1),1); % subtract mean for each component
    FLD = fisher_l_d( all_features', n_dimensions, n_samples );
    all_features = (FLD'*all_features')';   % on the 1st row the projection along FLD of 1st image features

    fprintf(1,'Computing MoG \n');
    % estimate MoG
    index2 = 0;
    for jj = 1:n_classes

        st = all_features(index2 + 1: index2 + n_samples(jj),:);

        fprintf(1,'computing MoG of class %d \n', jj);
        randn('state', 0);

        mix = gmm( size(st,2), n_gauss, covar_type );
        option = zeros(1,14);
        option(1) = -1;
        option(3) = 1;
        option(5) = 1;
        option(14) = MAX_N_ITER;
        rand('state', 0);
        randn('state', 0);

        mix = gmminit(mix, st, option);
        mix = gmmem(mix, st, option);
        mog(jj).mix = mix;
        index2 = index2 + n_samples(jj);

    end

    ris.mog = mog;
    ris.FLD = FLD;
    ris.mediatot = mediatot;    
    
end % end of function
