%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% Script to tests running the classifier against an entire directory
% for the user-interface code
%
% Author: Danelle Cline
% Date: Dec 2009
%

function test_run_tests_ui()

dbroot='./DB';
color_space=1;
threshold = 0.8;

handle='./kill_test';
create_kill_handle(handle); 

testdir='./testimages'; 
testname='testimages'
collect_tests(handle, testdir, dbroot, color_space);
 

trainingalias='benthic';
 
[eventids, majoritywinnerindex, probabilitywinnerindex, probability] = run_tests_ui(handle, dbroot, color_space, testname, trainingalias,threshold)
end
