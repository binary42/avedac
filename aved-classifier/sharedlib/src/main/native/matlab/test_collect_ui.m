%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% Script to tests collection for the user-interface code
%
% Author: Danelle Cline
% Date: Dec 2009
% 

function test_collect_ui(a)

dbroot='/Users/dcline';
color_space=2;
classname='Flatfish';
dirct_or_filelist='/Users/dcline/aved/ui/aved-classifier/sharedlibjni/src/test/resources/2526_Training_Classes/flat';
a='/Users/dcline/Desktop/test';
create_kill_handle(a);
collect_ui(a, dirct_or_filelist, dirct_or_filelist, classname, dbroot, color_space, 'Flatfish', 'Test flatfish image class');

%classname='Rockfish';
%dirct_or_filelist='/Users/dcline/aved/ui/aved-classifier/sharedlibjni/src/test/resources/2526_Training_Classes/rock';
%collect_ui(a, dirct_or_filelist, classname, dbroot, color_space, 'Rockfish', 'Test rockfish image class');

%classname='Leukethele';
%dirct_or_filelist='/Users/dcline/aved/ui/aved-classifier/sharedlibjni/src/test/resources/2526_Training_Classes/leuk';
%collect_ui(dirct_or_filelist, classname, dbroot, color_space);

%classname='Rathbunaster californicus';
%dirct_or_filelist='/Users/dcline/aved/ui/aved-classifier/sharedlibjni/src/test/resources/2526_Training_Classes/rath';
%collect_ui(dirct_or_filelist, classname, dbroot, color_space);

%classname='junk';
%dirct_or_filelist='/Users/dcline/aved/ui/aved-classifier/sharedlibjni/src/test/resources/2526_Training_Classes/junk';
%collect_ui(dirct_or_filelist, classname, dbroot, color_space);

end
