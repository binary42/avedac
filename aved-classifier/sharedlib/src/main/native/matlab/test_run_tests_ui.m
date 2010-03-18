%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% Script to tests running the classifier against an entire directory
% for the user-interface code
%
% Author: Danelle Cline
% Date: Dec 2009
%

function test_run_tests_ui()

dbroot='/Users/dcline';
color_space=1;

testdir='/Users/dcline/aved/ui/aved-classifier/sharedlibjni/src/test/resources/2526_Test_Cases/2526_00_47_53_05-events';
testdir='/var/tmp/2344_00_32_40_25/testimages';
testname='2526_00_47_53_05-events';
testname='testimages'
collect_tests(testdir, dbroot, color_space);
  
trainingalias='BenthicTest';
 
threshold=0.8;
[eventids, majoritywinnerindex, probabilitywinnerindex, probability] = run_tests_ui(dbroot, testname, trainingalias,threshold)
end
