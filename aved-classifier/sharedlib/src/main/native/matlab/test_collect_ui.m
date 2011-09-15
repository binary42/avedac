%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% Script to tests collection for the user-interface code
%
% Author: Danelle Cline
% Date: Dec 2009
% 

function test_collect_ui(a)

dbroot='./DB';   
color_space=1;
classname='Flatfish';
dirct_or_filelist='./trainingclasses/Flatfish';
handle='./kill_collect';
create_kill_handle(handle);
collect_ui(handle, dirct_or_filelist, dirct_or_filelist, classname, dbroot, color_space, classname, 'Test flatfish image class'); 
    
classname='Rockfish';
dirct_or_filelist='./trainingclasses/Rockfish';
collect_ui(handle, dirct_or_filelist, dirct_or_filelist, classname, dbroot, color_space, classname, 'Test rockfish image class');

classname='Leukethele';
dirct_or_filelist='./trainingclasses/Leukethele';
collect_ui(handle, dirct_or_filelist, dirct_or_filelist, classname, dbroot, color_space, classname, 'Test Leukethele');

classname='Rathbunaster';
dirct_or_filelist='./trainingclasses/Rathbunaster';
collect_ui(handle, dirct_or_filelist, dirct_or_filelist, classname, dbroot, color_space, classname, 'Test Rathbunaster');

classname='Other';
dirct_or_filelist='./trainingclasses/Other'; 
collect_ui(handle, dirct_or_filelist, dirct_or_filelist, classname, dbroot, color_space, classname, 'Test Other');

end
