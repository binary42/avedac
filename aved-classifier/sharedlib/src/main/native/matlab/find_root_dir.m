function rootdir = find_root_dir(dirstr)
%format class data file using root directory name
a=regexp(dirstr,'\.*?/\.*?','start');
if(length(a) > 1)
    rootdir = dirstr(a(end)+1:length(dirstr));
else
    rootdir = dirstr;
end
end