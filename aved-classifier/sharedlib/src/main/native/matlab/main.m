clc;
close all;
clear all;

matlabpath(pathdef());

COLOR_SPACE = 1; % Grayscale
 
tr_images_dir = 'U:/aved_marco/training_images/';
ts_images_dir = 'U:/aved_marco/test_images/V267smoothridge/';
dbtrain_dir = 'U:/aved_marco/DBtrain';
dbtest_dir = 'U:/aved_marco/DBtest';    
 
% build_classes(tr_images_dir,  dbtrain_dir, COLOR_SPACE);

% collect_tests(ts_images_dir, dbtest_dir, COLOR_SPACE);

run_tests( dbtrain_dir, dbtest_dir, 'V267results');

