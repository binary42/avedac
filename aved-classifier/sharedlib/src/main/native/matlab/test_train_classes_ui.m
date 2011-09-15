%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% Script to tests collection for the user-interface code
%
% Author: Danelle Cline
% Date: Dec 2009
% 

function test_train_classes_ui()

dbroot='./DB';   

class1='Flatfish';
class2='Rockfish';
class3='Leukethele'; 
class4='Rathbunaster';
class5='Other';
classnames=[{class1} {class2} {class3} {class4} {class5}];  
classalias='benthic';
color_space=1
handle='kill_train';
create_kill_handle(handle);  
train_classes_ui(handle, dbroot, color_space, classalias, classnames, 'Test benthic training classes'); 
end
