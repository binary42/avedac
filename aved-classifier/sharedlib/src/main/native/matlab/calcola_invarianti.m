function data = calcola_invarianti(ima, scale)

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% It computes Schmid's invariants at different scales. 
% DATE 16 Dec 2003
% USE   ima input image
%       scale scale to be used, i.e. scale = 3
%       data structure that contains all the invariants at different scales
% Author: Marc'Aurelio Ranzato
% 
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

echo on
SS = 0:scale;

% kernel local jet

gaus = [1 2 1]/4;
dd = [-1 0 1];
dx = dd;
dy = dd';

ima_g = double(ima);
 
for s = SS,
    
    ima_x = conv2(ima_g,dx,'same');
    ima_y = conv2(ima_g,dy,'same');
    ima_xy = conv2(ima_x,dy,'same');
    ima_xx = conv2(ima_x,dx,'same');
    ima_yy = conv2(ima_y,dy,'same');
    ima_xyy = conv2(ima_yy,dx,'same');
    ima_xxy = conv2(ima_xx,dy,'same');
    ima_xxx = conv2(ima_xx,dx,'same');
    ima_yyy = conv2(ima_yy,dy,'same');
    
    L = ima_g; 
    data(s+1).inv{1} = L(8:end-7, 8:end-7);
    LiLi = ima_x.^2 + ima_y.^2;
    data(s+1).inv{2} = LiLi(8:end-7, 8:end-7);
    LiLijLj = ima_x .* ima_xx .* ima_x + 2 * ima_x .* ima_xy .* ima_y + ima_y.*ima_yy .* ima_y;
    data(s+1).inv{3} = LiLijLj(8:end-7, 8:end-7);
    Lii = ima_xx + ima_yy;
    data(s+1).inv{4} = Lii(8:end-7, 8:end-7);
    LijLji = ima_xx.^2 + 2*ima_xy.^2 + ima_yy.^2;
    data(s+1).inv{5} = LijLji(8:end-7, 8:end-7);
    v5 = ima_xxx .* ima_y.^ 3 - ima_yyy .* ima_x.^3 + 4 * ima_xyy .* ima_x.^2 .* ima_y - 4 * ima_xxy .* ima_x .* ima_y.^2;
    data(s+1).inv{6} = v5(8:end-7, 8:end-7);
    v6 = ima_xxy .* ima_y.^3 + ima_xxy .* ima_x.*2 .* ima_y - ima_xyy .* ima_x .* ima_y.^2 - ima_xyy .* ima_x.^3;
    data(s+1).inv{7} = v6(8:end-7, 8:end-7);
    v7 = - ima_xxy .* ima_x.^3 - 2*ima_xyy .* ima_x.^2 .* ima_y - ima_yyy .* ima_x .* ima_y.^2 + ima_xxx .* ima_y .* ima_x.^2 + 2*ima_xxy .* ima_y.^2 .* ima_x + ima_xyy .* ima_y.^3;
    data(s+1).inv{8} = v7(8:end-7, 8:end-7);
    v8 = ima_xxx .* ima_x.^3 + 3*ima_xxy .* ima_x.^2 .* ima_y + 3*ima_xyy .* ima_x .* ima_y.^2 + ima_yyy .* ima_y.^3;
    data(s+1).inv{9} = v8(8:end-7, 8:end-7); 
    
    if ( s < SS(end) )        
        ima_g = conv2(conv2(ima_g,gaus,'same'),gaus','same'); 
    end
    
    echo off
end
