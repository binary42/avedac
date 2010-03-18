function val = apply_non_lin3(data, scale)

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% function dataf = apply_non_lin3(data, scale)
%
% DATE  15 Dec 2003
% DESCRIPTION apply a non linearity to the inavariants. From each invariant, its mean is
%    subtracted and the positive, negative and the absolute value are
%    considered
% USE   data data-structure in wich are stored the invariants
%       scale max. scale used
%       dataf transformed data 
%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

nr_inv = length(data(1).inv);   % nr. of invariants per scale

for s = 0 : scale

    for iii = 1 : nr_inv
        
        I = data(s+1).inv{iii};
        [mbg, stdbg] = comp_stat ( I ); % compute stat. bg.
        I = I - mbg;
        if(isempty(I) == 0)
            val(s*nr_inv + iii) = mean( I( find( I <= 0 ) ) );  % 1..36 neg. part, 37..72 positive part, 73..108 absolute value
            val((scale+1)*nr_inv + s*nr_inv + iii) = mean( I( find( I >= 0 ) ) );
            val(2*(scale+1)*nr_inv + s*nr_inv + iii) = mean(mean( abs(I) )) + mbg;
        else
            val(s*nr_inv + iii) = 0;
            val((scale+1)*nr_inv + s*nr_inv + iii) = 0;
            val(2*(scale+1)*nr_inv + s*nr_inv + iii) = 0;
        end       

    end
        
end

