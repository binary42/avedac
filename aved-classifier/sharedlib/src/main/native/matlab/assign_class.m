function [classindex, storeprob] = assign_class(kill, file, thresh, trainclasses)

global RIS;

%extract the file data
[data, imsize] = compute_invariant(file);               
        
tcd = data;
tcf{1,1} = file;
tcf{1,2} = imsize;

%init testing parameters
scale = 3;

%test classes against training classes
[CM, recfiles, storeprob, classindex] = test_ljmNL3 (kill, trainclasses, tcd, tcf, RIS, scale, thresh);

end