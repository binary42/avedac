
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% Script to tests collection for the user-interface code
%
% Author: Danelle Cline
% Date: Dec 2009
%

function test_test_classes_ui()

dbroot='./DB'; 
color_space=1;

handle='./kill_test';
create_kill_handle(handle); 

class1='Flatfish';
class2='Rockfish';
classalias='benthic';
testclassname=class1;
threshold=0.8; 

[testfiles, classindex, storeprob] = test_class(handle, dbroot, testclassname, classalias, threshold, color_space);

testclassname=class2;
threshold=0.8;

[testfiles, classindex, storeprob] = test_class(handle, dbroot, testclassname, classalias, threshold, color_space);
end
