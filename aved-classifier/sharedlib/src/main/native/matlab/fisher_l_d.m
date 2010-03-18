function [ FLD ] = fisher_l_d ( X, comp, n_samples)

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% function [ FLD ] = fisher_l_d ( X, comp, indx )
%
% Compute Fisher Linear Discriminant
% DATE: 29 Dec 2003
% USE:  X data set (each image data is in column vector)
%       comp nr. of discriminants to be used
%       n_samples vector storing the nr. of images in each class
%       FLD vectors where to project data for the best discrimination
% Author: Markus Weber
%         Marc'Aurelio Ranzato
%         Anelia Angelova  
%       
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

[D, N] = size( X );  %% Get n. of dimensions and n. of sample points from X
tot_n_samp = sum( n_samples );
lc = length( n_samples );   % nr. of (true) classes 

if comp < lc    % there is no need to split classes
    
	g = lc;
	Ng = ones(g,1);
	G = zeros(tot_n_samp ,g);
    index2 = 0;
	for jj=1:lc
        
        G(index2 + 1 : index2 + n_samples(jj),jj) = 1;
        index2 = index2 + n_samples(jj);
        Ng(jj) = n_samples(jj);
        
	end

else    % final dimension is greater than lc-1 => split classes in a sufficient nr. of clusters
    
    num_split = floor(comp/lc) + 1; % nr. of splittings per class
    g = lc*num_split;
    Ng = ones(g,1);
    G = zeros(tot_n_samp,g);
    index2 = 0;
    for jj=1:lc
        
        nr_s = round( n_samples(jj)/num_split );
        
        for kk = 1: num_split - 1
            
            G(index2 + 1 : index2 + nr_s, (jj-1)*num_split + kk) = 1;
            Ng( (jj-1)*num_split + kk ) = nr_s;
            index2 = index2 + nr_s;
            
        end
        
        % assigning the last data to the last cluster
        nr_s = n_samples(jj) - nr_s*(num_split - 1);
        kk = num_split;
        G(index2 + 1 : index2 + nr_s, (jj-1)*num_split + kk) = 1;
        Ng( (jj-1)*num_split + kk ) = nr_s;
        index2 = index2 + nr_s;
        
    end
    
end

T = diag(sqrt(N ./ Ng));

% Make X zeromean
mn = mean(X');
X = X - mn' * ones(1,N);

% Collect Group means
[U L V] = svd(X',0);

R = min(D,N);
U = U(:, 1:R);
L = L(1:R, 1:R);
V = V(:, 1:R);

l = diag(L);    % avoid warning while inverting
il = l.^-1;
iL = diag(il);
S = sqrt(N) * V * iL;
Xrs = (X' * S)';
M = inv(G' * G) * G' * Xrs';

[UU LL VV] = svd(inv(T) * M);
FLD = S * VV;

FLD = FLD(:,1:comp);

