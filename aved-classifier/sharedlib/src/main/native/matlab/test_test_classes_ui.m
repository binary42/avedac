
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% Script to tests collection for the user-interface code
%
% Author: Danelle Cline
% Date: Dec 2009
%

function test_test_classes_ui()

dbroot='/Users/dcline'; 
 
%class1='flat';

class1='Rathbunaster-californicus';
class2='Sebastes';
trainingclasses=[{class1} {class2}];

%class1='flat';
%class2='leuk';
%class3='rath';
%class4='junk';
%trainingclasses=[{class1} {class2} {class3} {class4}]; 

testclassname=class1;
threshold=0.8;

[testfiles, classindex, storeprob] = test_class(dbroot, testclassname, trainingclasses, threshold);

testclassname=class2;
threshold=0.8;

[testfiles, classindex, storeprob] = test_class(dbroot, testclassname, trainingclasses, threshold);
end
