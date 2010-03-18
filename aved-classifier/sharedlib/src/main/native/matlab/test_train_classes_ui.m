%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% Script to tests collection for the user-interface code
%
% Author: Danelle Cline
% Date: Dec 2009
% 

function test_train_classes_ui()

dbroot='/Users/dcline';   

class1='Flatfish';
class2='Rockfish';
class3='Leukethele'; 
class4='Rathbunaster californicus';
class5='junk';
%classnames=[{class1} {class2} {class3} {class4} {class5}]; 
classnames=[{class1} {class2}]; 
classalias='benthic';

train_classes_ui(dbroot, classalias, classnames, 'Test benthic training classes'); 
end
